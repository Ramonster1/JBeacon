package com.jbeacon.poll;

import com.jbeacon.command.OnPollResponseCommand;
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

/**
 * UDPPollingService is an implementation of the PollingService interface, designed for
 * performing polling operations over UDP protocols. It enables both blocking and non-blocking
 * modes of operation, facilitating interactions with remote services by sending requests
 * and processing responses.
 * <p>
 * This class utilizes a DatagramChannel for UDP communication, and data transfer is handled
 * through ByteBuffers. The response handling is provided by an injected `OnPollResponseCommand`
 * implementation, which is executed after a response is received. For non-blocking operations,
 * it integrates with a PollSelector, which monitors readiness states of the channels.
 * <p>
 * Key Features:
 * - Configurable blocking or non-blocking operation modes.
 * - Ability to bind and connect to UDP sockets for sending and receiving data.
 * - Provides a mechanism to process incoming responses via callback commands.
 * - Supports flexible configuration through the use of a builder pattern.
 * <p>
 * Usage Notes:
 * - The serverSocketAddress must be provided at initialization to define the remote endpoint.
 * - Buffers inBuffer and outBuffer must be properly configured for respective read and write operations.
 * - For non-blocking mode, a valid PollSelector instance must be provided. If the selector is
 * closed or null, a SelectorClosedException will be thrown.
 * - Proper synchronization should be considered when sharing a DatagramChannel across threads.
 * <p>
 * Exception Handling:
 * - Throws IOException for various I/O-related errors during channel operations or selector usage.
 * - Throws SelectorClosedException if the selector is closed or invalid in non-blocking mode.
 * <p>
 * Thread Safety:
 * - The class is not inherently thread-safe. If used in a multi-threaded environment, external
 * synchronization is required for thread safety.
 */
@Builder
public class UDPPollingService implements PollingService {
	private static final Logger logger = LogManager.getLogger();

	private final InetSocketAddress serverSocketAddress;
	private final ByteBuffer inBuffer;
	private final ByteBuffer outBuffer;
	@Builder.Default
	private final boolean blocks = true;
	private final OnPollResponseCommand onPollResponseCommand;
	private PollSelector pollSelector;
	private ScheduledExecutorService scheduledExecutor;


	/**
	 * Constructs a UDPPollingService instance for UDP-based communication and polling.
	 * This service can be used to send and receive data over UDP while implementing
	 * custom behavior for handling responses and controlling polling mechanisms.
	 *
	 * @param serverSocketAddress the server's address and port used for communication
	 * @param inBuffer the {@link ByteBuffer} used to store incoming data
	 * @param outBuffer the {@link ByteBuffer} used to store outgoing data
	 * @param blocks a boolean flag indicating whether the polling should be blocking or non-blocking
	 * @param onPollResponseCommand the {@link OnPollResponseCommand} implementation to process received data
	 * @param pollSelector the {@link PollSelector} instance responsible for channel readiness monitoring
	 * @param scheduledExecutor the {@link ScheduledExecutorService} used to manage periodic tasks
	 */
	UDPPollingService(InetSocketAddress serverSocketAddress, ByteBuffer inBuffer, ByteBuffer outBuffer, boolean blocks, OnPollResponseCommand onPollResponseCommand, PollSelector pollSelector, ScheduledExecutorService scheduledExecutor) {
		this.serverSocketAddress = serverSocketAddress;
		this.inBuffer = inBuffer;
		this.outBuffer = outBuffer;
		this.blocks = blocks;
		this.onPollResponseCommand = onPollResponseCommand;
		this.pollSelector = pollSelector;
		this.scheduledExecutor = scheduledExecutor;
	}

	public void poll() throws IOException {
		logger.info("Polling in {} mode", blocks ? "blocking" : "non-blocking");

		try (DatagramChannel datagramChannel = DatagramChannel.open()) {
			if (!blocks && (pollSelector == null || pollSelector.selector() == null || !pollSelector.selector().isOpen())) {
				logger.warn("Selector is null or not open");
				throw new SelectorClosedException("Selector is " + (pollSelector == null ? "null" : "closed"));
			}

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

				onPollResponseCommand.execute(inBuffer);
			} else {
				Selector selector = pollSelector.selector();
				datagramChannel.configureBlocking(false);
				ProcessPollAttachment attachment = new ProcessPollAttachment(onPollResponseCommand, inBuffer);
				datagramChannel.register(selector, SelectionKey.OP_READ, attachment);

				logger.info("Registered channel, waiting for response");

				pollSelector.process();
			}

			inBuffer.clear();
		}
	}
}
