package com.jbeacon.integrationTest.aeron.command;

import com.jbeacon.aeron.command.AeronOnPollResponseTryClaimCommand;
import com.jbeacon.core.command.OnPollResponseCommand;
import com.jbeacon.integrationTest.aeron.util.SubscriberAndPollingTestCoordinator;
import com.jbeacon.integrationTest.aeron.util.SubscriptionService;
import com.jbeacon.integrationTest.util.PollingTestService;
import com.jbeacon.integrationTest.util.UDPTestServer;
import io.aeron.Aeron;
import io.aeron.Publication;
import io.aeron.driver.MediaDriver;
import io.aeron.logbuffer.BufferClaim;
import io.aeron.logbuffer.FragmentHandler;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AeronOnPollResponseTryClaimCommandIT {
	private static final String DEFAULT_HOST = "localhost";
	private static final int DEFAULT_PORT = 20122;
	private static final String DEFAULT_CHANNEL = String.format("aeron:udp?endpoint=%s:%d", DEFAULT_HOST, DEFAULT_PORT);
	private static final int STREAM_ID = 1001;

	private static final String MDC_HOST = "localhost";
	private static final int MDC_CONTROL_PORT = 20123;
	private static final String MDC_CONTROL_CHANNEL = String.format("aeron:udp?control-mode=dynamic|control=%s:%d", MDC_HOST, MDC_CONTROL_PORT);
	private static final String MDC_SUBSCRIPTION_CHANNEL = String.format("aeron:udp?endpoint=localhost:0|control=%s:%d|control-mode=dynamic", MDC_HOST, MDC_CONTROL_PORT);

	@AutoClose
	private static UDPTestServer testServer;
	@AutoClose
	private static MediaDriver mediaDriver;
	@AutoClose
	private static Aeron aeron;
	private static SubscriberAndPollingTestCoordinator coordinator;

	private final BufferClaim bufferClaim = new BufferClaim();

	@BeforeAll
	static void setUpClass() throws SocketException {
		mediaDriver = MediaDriver.launchEmbedded();
		final Aeron.Context ctx = new Aeron.Context();
		ctx.aeronDirectoryName(mediaDriver.aeronDirectoryName());

		aeron = Aeron.connect(ctx);
		testServer = new UDPTestServer();
		InetSocketAddress localhostAddress = new InetSocketAddress(testServer.getSocket().getLocalAddress(), testServer.getSocket().getLocalPort());
		// This thread should terminate when testServer, which has @AutoClose, is closed
		Thread serverThread = new Thread(testServer::startServer);
		serverThread.setDaemon(true);
		serverThread.start();

		PollingTestService pollingTestService = new PollingTestService(testServer, localhostAddress);
		coordinator = new SubscriberAndPollingTestCoordinator(pollingTestService);
	}

	private static FragmentHandler createDefaultFragmentHandler() {
		return (buffer, offset, length, header) -> {
			String receivedMessage = buffer.getStringWithoutLengthUtf8(offset, length);
			assertEquals(new String(testServer.getData(), UDPTestServer.CHARSET), receivedMessage);
		};
	}

	@Test
	void testOffsetAndLengthValuesProvidedToOffer() throws ExecutionException, InterruptedException, IOException {
		FragmentHandler fragmentHandlerTest = (buffer, offset, length, header) -> {
			final String subscriberMsg = buffer.getStringWithoutLengthUtf8(offset, length);

			assertEquals(new String(testServer.getData(), UDPTestServer.CHARSET), subscriberMsg);
			assertEquals(0, buffer.byteBuffer().position());
			assertEquals(testServer.getData().length, length);
		};

		try (Publication publication = aeron.addPublication(DEFAULT_CHANNEL, STREAM_ID)) {
			try (SubscriptionService subscription = new SubscriptionService(1, DEFAULT_CHANNEL, fragmentHandlerTest)) {
				Supplier<OnPollResponseCommand> commandSupplier = () -> new AeronOnPollResponseTryClaimCommand(publication, bufferClaim);

				coordinator.executeTest(commandSupplier, List.of(subscription));
			}
		}
	}

	@Test
	void testPublicationOfferedAfterPoll() throws Exception {
		try (Publication publication = aeron.addPublication(DEFAULT_CHANNEL, STREAM_ID)) {
			try (SubscriptionService subscription = new SubscriptionService(1, DEFAULT_CHANNEL, createDefaultFragmentHandler())) {
				Supplier<OnPollResponseCommand> commandSupplier = () -> new AeronOnPollResponseTryClaimCommand(publication, bufferClaim);

				coordinator.executeTest(commandSupplier, List.of(subscription));
			}
		}
	}

	@Test
	void testPublishedMessagesForMultiDestinationCastSubscribers() throws Exception {
		try (Publication publication = aeron.addPublication(MDC_CONTROL_CHANNEL, STREAM_ID)) {
			try (SubscriptionService subscription1 = new SubscriptionService(1, MDC_SUBSCRIPTION_CHANNEL, createDefaultFragmentHandler());
				 SubscriptionService subscription2 = new SubscriptionService(2, MDC_SUBSCRIPTION_CHANNEL, createDefaultFragmentHandler())) {
				Supplier<OnPollResponseCommand> commandSupplier = () -> new AeronOnPollResponseTryClaimCommand(publication, bufferClaim);

				coordinator.executeTest(commandSupplier, List.of(subscription1, subscription2));
			}
		}
	}
}
