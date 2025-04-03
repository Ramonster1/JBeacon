package com.jbeacon.aeron.command;

import com.jbeacon.aeron.util.SubscriberAndPollingTestCoordinator;
import com.jbeacon.aeron.util.SubscriptionService;
import com.jbeacon.command.OnPollResponseCommand;
import com.jbeacon.util.PollingTestService;
import com.jbeacon.util.UDPTestServer;
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
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AeronOnPollResponseOfferCommandIT {
	private static final String DEFAULT_HOST = "localhost";
	private static final int DEFAULT_PORT = 20122;
	private static final String DEFAULT_CHANNEL = String.format("aeron:udp?endpoint=%s:%d", DEFAULT_HOST, DEFAULT_PORT);
	private static final int STREAM_ID = 1001;

	private static final String MDC_HOST = "localhost";
	private static final int MDC_CONTROL_PORT = 20123;
	private static final String MDC_CONTROL_CHANNEL = String.format("aeron:udp?control-mode=dynamic|control=%s:%d", MDC_HOST, MDC_CONTROL_PORT);
	private static final String MDC_SUBSCRIPTION_CHANNEL = String.format("aeron:udp?endpoint=localhost:0|control=%s:%d|control-mode=dynamic", MDC_HOST, MDC_CONTROL_PORT);

	private static final UnsafeBuffer BUFFER = new UnsafeBuffer(BufferUtil.allocateDirectAligned(256, 64));
	@AutoClose
	private static UDPTestServer testServer;
	@AutoClose
	private static MediaDriver mediaDriver;
	@AutoClose
	private static Aeron aeron;
	private static SubscriberAndPollingTestCoordinator coordinator;

	private static FragmentHandler createDefaultFragmentHandlerTest(UDPTestServer server) {
		return (buf, offset, length, header) -> {
			final String message = buf.getStringWithoutLengthUtf8(offset, length);
			assertEquals(new String(server.getData(), UDPTestServer.CHARSET), message);
		};
	}

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
				Supplier<OnPollResponseCommand> commandSupplier = () -> new AeronOnPollResponseOfferCommand(publication, BUFFER);
				coordinator.executeTest(commandSupplier, List.of(subscription));
			}
		}
	}

	@Test
	void testPublicationOfferedAfterPoll() throws Exception {
		try (Publication publication = aeron.addPublication(DEFAULT_CHANNEL, STREAM_ID)) {
			try (SubscriptionService subscription = new SubscriptionService(1, DEFAULT_CHANNEL, createDefaultFragmentHandlerTest(testServer))) {
				Supplier<OnPollResponseCommand> commandSupplier = () -> new AeronOnPollResponseOfferCommand(publication, BUFFER);
				coordinator.executeTest(commandSupplier, List.of(subscription));
			}
		}
	}

	@Test
	void testPublishedMessagesForMultiDestinationCastSubscribers() throws Exception {
		try (Publication publication = aeron.addPublication(MDC_CONTROL_CHANNEL, STREAM_ID)) {
			try (SubscriptionService subscription1 = new SubscriptionService(1, MDC_SUBSCRIPTION_CHANNEL, createDefaultFragmentHandlerTest(testServer));
				 SubscriptionService subscription2 = new SubscriptionService(2, MDC_SUBSCRIPTION_CHANNEL, createDefaultFragmentHandlerTest(testServer))) {

				Supplier<OnPollResponseCommand> commandSupplier = () -> new AeronOnPollResponseOfferCommand(publication, BUFFER);
				coordinator.executeTest(commandSupplier, List.of(subscription1, subscription2));
			}
		}
	}
}
