package com.jbeacon.poll;

import com.jbeacon.command.PollResponseCommand;
import com.jbeacon.exception.SelectorClosedException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.UnresolvedAddressException;
import java.nio.charset.Charset;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UdpPollingServiceTest {
	private final static Charset CHARSET = Charset.defaultCharset();
	private static Thread serverThread;
	@AutoClose
	private static UdpTestServer testServer;
	private static InetSocketAddress localhostAddress;
	private static PollResponseCommand testBufferReadyForDrainingCommand;

	@BeforeAll
	static void setUp() throws SocketException {
		testServer = new UdpTestServer();
		localhostAddress = new InetSocketAddress(testServer.getSocket().getLocalAddress(), testServer.getSocket().getLocalPort());
		serverThread = new Thread(testServer::startServer);
		serverThread.start();

		testBufferReadyForDrainingCommand = buffer -> {
			assertEquals(0, buffer.position());
			assertEquals(testServer.getData().length, buffer.limit());
			assertEquals(100, buffer.capacity());

			String bufferData = new String(buffer.array(), buffer.position(), buffer.limit(), CHARSET);
			assertEquals(testServer.getDate(), bufferData);
		};
	}

	private void updateTestServerResponse() {
		var now = new Date();
		testServer.setDate(now.toString());
		testServer.setData(now.toString().getBytes(CHARSET));
	}

	@AfterAll
	static void tearDown() {
		serverThread.interrupt();
	}

	@Test
	void testNonblockingPollWithNoSelectorFails() {
		var nonBlockingPoller = UdpPollingService.builder()
				.serverSocketAddress(localhostAddress)
				.outBuffer(ByteBuffer.allocate(1))
				.inBuffer(ByteBuffer.allocate(4))
				.blocks(false)
				.build();

		assertThrows(SelectorClosedException.class, nonBlockingPoller::poll);
	}

	@Test
	void testBlockingPollBufferReadyForDraining() throws Exception {
		updateTestServerResponse();

		var blockingPoller = UdpPollingService.builder()
				.serverSocketAddress(localhostAddress)
				.outBuffer(ByteBuffer.allocate(1))
				.inBuffer(ByteBuffer.allocate(100))
				.pollResponseCommand(testBufferReadyForDrainingCommand)
				.blocks(true)
				.build();

		// test
		blockingPoller.poll();
	}

	@Test
	void testNonBlockingPollBufferReadyForDraining() throws Exception {
		try (Selector selector = Selector.open()) {
			var pollSelector = new PollSelector(selector, 1000L);

			var nonblockingPoller = UdpPollingService.builder()
					.serverSocketAddress(localhostAddress)
					.outBuffer(ByteBuffer.allocate(1))
					.inBuffer(ByteBuffer.allocate(100))
					.pollResponseCommand(testBufferReadyForDrainingCommand)
					.blocks(false)
					.pollSelector(pollSelector)
					.build();

			// test
			nonblockingPoller.poll();
		}
	}

	@Test
	void testBlockingPollBuffersAreResetForMultipleRequests() throws Exception {
		var blockingPoller = UdpPollingService.builder()
				.serverSocketAddress(localhostAddress)
				.outBuffer(ByteBuffer.allocate(1))
				.inBuffer(ByteBuffer.allocate(100))
				.pollResponseCommand(testBufferReadyForDrainingCommand)
				.blocks(true)
				.build();

		// test
		for (int i = 0; i < 10; i++) {
			updateTestServerResponse();
			blockingPoller.poll();
		}
	}

	@Test
	void testNonBlockingPollBuffersCanHandleMultipleRequests() throws Exception {
		try (Selector selector = Selector.open()) {
			var pollSelector = new PollSelector(selector, 1000L);

			var nonblockingPoller = UdpPollingService.builder()
					.serverSocketAddress(localhostAddress)
					.outBuffer(ByteBuffer.allocate(1))
					.inBuffer(ByteBuffer.allocate(100))
					.pollResponseCommand(testBufferReadyForDrainingCommand)
					.blocks(false)
					.pollSelector(pollSelector)
					.build();

			// test
			for (int i = 0; i < 10; i++) {
				updateTestServerResponse();
				nonblockingPoller.poll();
			}
		}
	}

	@Test
	void testPollWithInvalidAddress() {
		InetSocketAddress invalidAddress = new InetSocketAddress("invalid.host", 12345);

		var poller = UdpPollingService.builder()
				.serverSocketAddress(invalidAddress)
				.outBuffer(ByteBuffer.allocate(1))
				.inBuffer(ByteBuffer.allocate(100))
				.blocks(true)
				.build();

		assertThrows(UnresolvedAddressException.class, poller::poll);
	}

}