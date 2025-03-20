package com.jbeacon.poll;

import com.jbeacon.command.PollResponseCommand;
import com.jbeacon.exception.SelectorClosedException;
import lombok.Builder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.concurrent.ScheduledExecutorService;

@Builder
public class UdpPollingService implements PollingService {
	private static final Logger logger = LogManager.getLogger();

	private final InetSocketAddress serverSocketAddress;
	private final ByteBuffer inBuffer;
	private final ByteBuffer outBuffer;
	@Builder.Default
	private final boolean blocks = true;
	private final PollResponseCommand pollResponseCommand;
	private PollSelector pollSelector;
	private ScheduledExecutorService scheduledExecutor;

	public void poll() throws IOException {
		logger.info("Polling in {} mode", blocks ? "blocking" : "non-blocking");

		try (DatagramChannel datagramChannel = DatagramChannel.open()) {
			InetSocketAddress inetSocketAddress = new InetSocketAddress(0);

			logger.debug("Binding to {}", inetSocketAddress);
			datagramChannel.bind(inetSocketAddress);
			datagramChannel.connect(serverSocketAddress);

			logger.info("Sending request to {} with buffer {}", serverSocketAddress, outBuffer);

			datagramChannel.send(outBuffer, serverSocketAddress);
			outBuffer.flip();

			if (blocks) {
				datagramChannel.configureBlocking(true);
				datagramChannel.receive(inBuffer);
				inBuffer.flip();

				logger.info("Received response with buffer {}", outBuffer);

				pollResponseCommand.execute(inBuffer);
			} else {
				if (pollSelector == null || pollSelector.selector() == null || !pollSelector.selector().isOpen()) {
					logger.warn("Selector is null or not open");
					throw new SelectorClosedException("Selector is null or closed");
				}

				Selector selector = pollSelector.selector();
				datagramChannel.configureBlocking(false);
				ProcessPollAttachment attachment = new ProcessPollAttachment(pollResponseCommand, inBuffer);
				datagramChannel.register(selector, SelectionKey.OP_READ, attachment);

				logger.info("Registered channel, waiting for response");

				pollSelector.process();
			}

			inBuffer.clear();
		}
	}
}
