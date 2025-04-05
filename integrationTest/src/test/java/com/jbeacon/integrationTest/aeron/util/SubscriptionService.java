package com.jbeacon.integrationTest.aeron.util;

import io.aeron.Aeron;
import io.aeron.FragmentAssembler;
import io.aeron.Image;
import io.aeron.Subscription;
import io.aeron.driver.Configuration;
import io.aeron.driver.MediaDriver;
import io.aeron.logbuffer.FragmentHandler;
import org.agrona.CloseHelper;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.SigInt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class SubscriptionService implements AutoCloseable {
	private static final Logger logger = LogManager.getLogger();

	private static final int STREAM_ID = 1001;
	private static final int FRAGMENT_COUNT_LIMIT = 10;
	private final int id;
	private final String channel;
	private final FragmentHandler fragmentHandler;
	private final AtomicBoolean running = new AtomicBoolean(false);
	private MediaDriver driver;
	private Aeron aeron;
	private Subscription subscription;

	public SubscriptionService(int id, String channel, FragmentHandler fragmentHandler) {
		this.id = id;
		this.channel = channel;
		this.fragmentHandler = fragmentHandler;
	}

	/**
	 * Create a new {@link IdleStrategy}.
	 * BusySpinIdleStrategy is more responsive than the default BackOffIdleStrategy and should therefore be better for unit tests.
	 *
	 * @return a new {@link IdleStrategy}.
	 */
	private static IdleStrategy busySpinIdleStrategy() {
		return Configuration.agentIdleStrategy("org.agrona.concurrent.BusySpinIdleStrategy", null);
	}

	public void startSubscribing() {
		logger.info("Starting subscriber {}", id);

		driver = MediaDriver.launchEmbedded();
		final Aeron.Context ctx = new Aeron.Context().availableImageHandler(this::printAvailableImage).unavailableImageHandler(this::printUnavailableImage);

		ctx.aeronDirectoryName(driver.aeronDirectoryName());
		logger.info("Using embedded media driver: {}", driver.aeronDirectoryName());

		running.set(true);

		// Register a SIGINT handler for graceful shutdown (when exiting via CTRL-C).
		SigInt.register(() -> running.set(false));

		aeron = Aeron.connect(ctx);
		subscription = aeron.addSubscription(channel, STREAM_ID);
		logger.info("Subscribed to {} on stream id {}", channel, STREAM_ID);

		subscriberLoop().accept(subscription);
	}

	public boolean isConnected() {
		return subscription != null && subscription.isConnected();
	}

	public void close() throws InterruptedException {
		running.set(false);

		// Allow the subscription to read the new publication before closing the subscription which would delete the log
		// buffer file and fail any tests.
		TimeUnit.SECONDS.sleep(1);
		logger.info("Stopping subscriber {}", id);

		CloseHelper.quietCloseAll(driver, aeron, subscription);
	}

	/**
	 * Print the information for an available image to stdout.
	 *
	 * @param image that has been created.
	 */
	private void printAvailableImage(final Image image) {
		final Subscription subscription = image.subscription();
		logger.debug("Available image on {} streamId={} sessionId={} mtu={} term-length={} from {}", subscription.channel(), subscription.streamId(), image.sessionId(), image.mtuLength(), image.termBufferLength(), image.sourceIdentity());
	}

	/**
	 * Print the information for an unavailable image to stdout.
	 *
	 * @param image that has gone inactive.
	 */
	private void printUnavailableImage(final Image image) {
		final Subscription subscription = image.subscription();
		logger.debug("Unavailable image on {} streamId={} sessionId={}", subscription.channel(), subscription.streamId(), image.sessionId());
	}

	/**
	 * Return a reusable, parametrised event loop that calls and idler when no messages are received.
	 *
	 * @return loop function.
	 */
	private Consumer<Subscription> subscriberLoop() {
		return (subscription) -> {
			final FragmentAssembler assembler = new FragmentAssembler(fragmentHandler);
			while (running.get()) {
				final int fragmentsRead = subscription.poll(assembler, FRAGMENT_COUNT_LIMIT);
				busySpinIdleStrategy().idle(fragmentsRead);
			}
		};
	}

}
