package com.jbeacon.aeron.command;

import com.jbeacon.aeron.util.AsyncAssertionExecutor;
import com.jbeacon.aeron.util.SubscriptionService;
import com.jbeacon.poll.UdpPollingService;
import com.jbeacon.util.UdpTestServer;
import io.aeron.Aeron;
import io.aeron.Publication;
import io.aeron.driver.MediaDriver;
import io.aeron.logbuffer.FragmentHandler;
import org.agrona.BufferUtil;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class AeronOnPollResponseOfferCommandIT {
	private static final String DEFAULT_HOST = "localhost";
	private static final int DEFAULT_PORT = 20122;
	private static final String DEFAULT_CHANNEL = String.format("aeron:udp?endpoint=%s:%d", DEFAULT_HOST, DEFAULT_PORT);
	private static final int STREAM_ID = 1001;

	private static final String MDC_HOST = "localhost";
	private static final int MDC_CONTROL_PORT = 20123;
	private static final String MDC_CONTROL_CHANNEL = String.format("aeron:udp?control-mode=dynamic|control=%s:%d", MDC_HOST, MDC_CONTROL_PORT);
	private static final String MDC_SUBSCRIPTION_CHANNEL = String.format("aeron:udp?endpoint=localhost:0|control=%s:%d|control-mode=dynamic", MDC_HOST, MDC_CONTROL_PORT);

	private static final int OUT_BUFFER_SIZE = 1;
	private static final int IN_BUFFER_SIZE = 100;
	private static final int TEST_ITERATIONS = 10;


	private static final UnsafeBuffer BUFFER = new UnsafeBuffer(BufferUtil.allocateDirectAligned(256, 64));
	@AutoClose
	private static UdpTestServer testServer;
	private static InetSocketAddress localhostAddress;
	@AutoClose
	private static MediaDriver mediaDriver;
	@AutoClose
	private static Aeron aeron;

	private static AsyncAssertionExecutor initSubscriptionExecutor(SubscriptionService subscriptionService, FragmentHandler fragmentHandlerTest) {
		return new AsyncAssertionExecutor(() -> subscriptionService.startSubscribing(fragmentHandlerTest));
	}

	private static FragmentHandler createFragmentHandler(UdpTestServer server) {
		return (buf, offset, length, header) -> {
			final String message = buf.getStringWithoutLengthUtf8(offset, length);
			assertEquals(new String(server.getData(), UdpTestServer.CHARSET), message);
		};
	}

	@BeforeAll
	static void setUpClass() throws SocketException {
		mediaDriver = MediaDriver.launchEmbedded();
		final Aeron.Context ctx = new Aeron.Context();
		ctx.aeronDirectoryName(mediaDriver.aeronDirectoryName());

		aeron = Aeron.connect(ctx);
		testServer = new UdpTestServer();
		localhostAddress = new InetSocketAddress(testServer.getSocket().getLocalAddress(), testServer.getSocket().getLocalPort());
		// This thread should terminate when testServer, which has @AutoClose, is closed
		Thread serverThread = new Thread(testServer::startServer);
		serverThread.setDaemon(true);
		serverThread.start();
	}

	@Test
	void testOffsetAndLengthValuesProvidedToOffer() throws ExecutionException, InterruptedException, IOException {
		try (Publication publication = aeron.addPublication(DEFAULT_CHANNEL, STREAM_ID)) {
			try (SubscriptionService subscription = new SubscriptionService(1, DEFAULT_CHANNEL)) {

				FragmentHandler fragmentHandlerTest = (buffer, offset, length, header) -> {
					final String subscriberMsg = buffer.getStringWithoutLengthUtf8(offset, length);

					assertEquals(new String(testServer.getData(), UdpTestServer.CHARSET), subscriberMsg);
					assertEquals(0, buffer.byteBuffer().position());
					assertEquals(testServer.getData().length, length);
				};

				createServicesAndRunTest(publication, fragmentHandlerTest, subscription);
			}
		}
	}

	@Test
	void testPublicationOfferedAfterPoll() throws Exception {
		try (Publication publication = aeron.addPublication(DEFAULT_CHANNEL, STREAM_ID)) {
			try (SubscriptionService subscription = new SubscriptionService(1, DEFAULT_CHANNEL)) {
				createServicesAndRunTest(publication, subscription);
			}
		}
	}

	@Test
	void testPublishedMessagesForMultiDestinationCastSubscribers() throws Exception {
		try (Publication publication = aeron.addPublication(MDC_CONTROL_CHANNEL, STREAM_ID)) {
			try (SubscriptionService subscription1 = new SubscriptionService(1, MDC_SUBSCRIPTION_CHANNEL);
				 SubscriptionService subscription2 = new SubscriptionService(2, MDC_SUBSCRIPTION_CHANNEL)) {
				createServicesAndRunTest(publication, subscription1, subscription2);
			}
		}
	}

	private void createServicesAndRunTest(Publication publication, FragmentHandler fragmentHandler, SubscriptionService... subscribers) throws ExecutionException, InterruptedException, IOException {
		List<AsyncAssertionExecutor> testExecutors = new ArrayList<>();
		for (SubscriptionService subscriber : subscribers) {
			testExecutors.add(initSubscriptionExecutor(subscriber, fragmentHandler));
		}

		runTest(publication, testExecutors.toArray(AsyncAssertionExecutor[]::new));

		for (SubscriptionService subscriber : subscribers) {
			subscriber.close();
		}

		for (AsyncAssertionExecutor subscriberThread : testExecutors) {
			subscriberThread.waitForCompletionAndAssert();
		}
	}

	private void createServicesAndRunTest(Publication publication, SubscriptionService... subscribers) throws ExecutionException, InterruptedException, IOException {
		createServicesAndRunTest(publication, createFragmentHandler(testServer), subscribers);
	}

	private void runTest(Publication publication, AsyncAssertionExecutor... subscriberThreads) throws ExecutionException, InterruptedException, IOException {
		for (AsyncAssertionExecutor subscriberThread : subscriberThreads) {
			subscriberThread.start();
		}

		waitForPublicationConnection(publication);

		var pollingService = UdpPollingService.builder()
				.serverSocketAddress(localhostAddress)
				.outBuffer(ByteBuffer.allocate(OUT_BUFFER_SIZE))
				.inBuffer(ByteBuffer.allocate(IN_BUFFER_SIZE))
				.onPollResponseCommand(new AeronOnPollResponseOfferCommand(publication, BUFFER))
				.blocks(true)
				.build();

		// test
		for (int i = 0; i < TEST_ITERATIONS; i++) {
			testServer.updateTestServerResponse();
			pollingService.poll();
		}
	}

	/**
	 * Utility method to wait until the Aeron publication is connected.
	 * Throws an exception if the connection isn't established within the timeout.
	 */
	private void waitForPublicationConnection(Publication publication) throws ExecutionException, InterruptedException {
		CompletableFuture<Boolean> isSubscribed = CompletableFuture.supplyAsync(() -> {
			while (true) {
				if (!publication.isConnected()) {
					try {
						TimeUnit.MILLISECONDS.sleep(100);
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				} else {
					return true;
				}
			}
		});
		isSubscribed.completeOnTimeout(false, 10, TimeUnit.SECONDS);
		if (!isSubscribed.get()) {
			fail("Failed to connect to Aeron publication");
		}
	}
}
