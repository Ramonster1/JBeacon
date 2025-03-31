package com.jbeacon.poll;

import com.jbeacon.exception.SelectorClosedException;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.UnresolvedAddressException;

import static org.junit.jupiter.api.Assertions.assertThrows;

class UdpPollingServiceTest {

	@Test
	void testNonblockingPollWithNoSelectorFails() {
		var nonBlockingPoller = UdpPollingService.builder()
				.serverSocketAddress(null)
				.outBuffer(ByteBuffer.allocate(1))
				.inBuffer(ByteBuffer.allocate(4))
				.blocks(false)
				.build();

		assertThrows(SelectorClosedException.class, nonBlockingPoller::poll);
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