package com.jbeacon.integrationTest.core.poll;

import com.jbeacon.core.command.OnPollResponseCommand;
import com.jbeacon.integrationTest.util.PollingTestService;
import com.jbeacon.integrationTest.util.UDPTestServer;
import com.jbeacon.core.poll.PollSelector;
import com.jbeacon.core.poll.UDPPollingService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UDPPollingServiceIT {

	private static Thread serverThread;
	@AutoClose
	private static UDPTestServer testServer;
	private static InetSocketAddress localhostAddress;
	private static OnPollResponseCommand testBufferReadyForDrainingCommand;

	@BeforeAll
	static void setUp() throws SocketException {
		testServer = new UDPTestServer();
		localhostAddress = new InetSocketAddress(testServer.getSocket().getLocalAddress(), testServer.getSocket().getLocalPort());
		serverThread = new Thread(testServer::startServer);
		serverThread.start();

		testBufferReadyForDrainingCommand = buffer -> {
			assertEquals(0, buffer.position());
			assertEquals(testServer.getData().length, buffer.limit());
			assertEquals(100, buffer.capacity());

			String bufferData = new String(buffer.array(), buffer.position(), buffer.limit(), UDPTestServer.CHARSET);
			assertEquals(testServer.getDate(), bufferData);
		};
	}

	@AfterAll
	static void tearDown() {
		serverThread.interrupt();
	}

	@Test
	void testBlockingPollBufferReadyForDraining() throws Exception {
		testServer.updateTestServerResponse();

		var blockingPoller = UDPPollingService.builder()
				.serverSocketAddress(localhostAddress)
				.outBuffer(ByteBuffer.allocate(1))
				.inBuffer(ByteBuffer.allocate(100))
				.onPollResponseCommand(testBufferReadyForDrainingCommand)
				.blocks(true)
				.build();

		// test
		blockingPoller.poll();
	}

	@Test
	void testNonBlockingPollBufferReadyForDraining() throws Exception {
		try (Selector selector = Selector.open()) {
			var pollSelector = new PollSelector(selector, 1000L);

			var nonblockingPoller = UDPPollingService.builder()
					.serverSocketAddress(localhostAddress)
					.outBuffer(ByteBuffer.allocate(1))
					.inBuffer(ByteBuffer.allocate(100))
					.onPollResponseCommand(testBufferReadyForDrainingCommand)
					.blocks(false)
					.pollSelector(pollSelector)
					.build();

			// test
			nonblockingPoller.poll();
		}
	}

	@Test
	void testBlockingPollBuffersAreResetForMultipleRequests() throws Exception {
		PollingTestService blockingPollTestService = new PollingTestService(testServer, localhostAddress);
		blockingPollTestService.createBlockingPollingService(testBufferReadyForDrainingCommand);
		blockingPollTestService.pollRepeatedly();
	}

	@Test
	void testNonBlockingPollBuffersCanHandleMultipleRequests() throws Exception {
		try (Selector selector = Selector.open()) {
			var pollSelector = new PollSelector(selector, 1000L);

			PollingTestService nonBlockingPollTestService = new PollingTestService(testServer, localhostAddress);
			nonBlockingPollTestService.createNonBlockingPollingService(testBufferReadyForDrainingCommand, pollSelector);
			nonBlockingPollTestService.pollRepeatedly();
		}
	}
}
