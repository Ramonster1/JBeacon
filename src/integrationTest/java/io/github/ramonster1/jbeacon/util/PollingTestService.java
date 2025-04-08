package io.github.ramonster1.jbeacon.util;

import io.github.ramonster1.jbeacon.command.OnPollResponseCommand;
import io.github.ramonster1.jbeacon.poll.PollSelector;
import io.github.ramonster1.jbeacon.poll.PollingService;
import io.github.ramonster1.jbeacon.poll.UDPPollingService;
import lombok.AllArgsConstructor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

@AllArgsConstructor
public class PollingTestService {
	private static final int DEFAULT_TEST_ITERATIONS = 10;
	private static final int DEFAULT_OUT_BUFFER_SIZE = 1;
	private static final int DEFAULT_IN_BUFFER_SIZE = 100;

	private final UDPTestServer testServer;
	private final InetSocketAddress serverSocketAddress;
	private PollingService pollingService;

	public PollingTestService(UDPTestServer testServer, InetSocketAddress serverSocketAddress) {
		this.testServer = testServer;
		this.serverSocketAddress = serverSocketAddress;
	}

	public void createNonBlockingPollingService(OnPollResponseCommand onPollResponseCommand, PollSelector pollSelector) {
		pollingService = UDPPollingService.builder()
				.serverSocketAddress(serverSocketAddress)
				.outBuffer(ByteBuffer.allocate(DEFAULT_OUT_BUFFER_SIZE))
				.inBuffer(ByteBuffer.allocate(DEFAULT_IN_BUFFER_SIZE))
				.onPollResponseCommand(onPollResponseCommand)
				.blocks(false)
				.pollSelector(pollSelector)
				.build();
	}

	public void createBlockingPollingService(OnPollResponseCommand onPollResponseCommand) {
		pollingService = UDPPollingService.builder()
				.serverSocketAddress(serverSocketAddress)
				.outBuffer(ByteBuffer.allocate(DEFAULT_OUT_BUFFER_SIZE))
				.inBuffer(ByteBuffer.allocate(DEFAULT_IN_BUFFER_SIZE))
				.onPollResponseCommand(onPollResponseCommand)
				.build();
	}

	public void pollRepeatedly() throws IOException {
		if (pollingService == null) {
			throw new IllegalStateException("Polling service not initialized");
		}

		for (int i = 0; i < DEFAULT_TEST_ITERATIONS; i++) {
			testServer.updateTestServerResponse();
			pollingService.poll();
		}
	}

}
