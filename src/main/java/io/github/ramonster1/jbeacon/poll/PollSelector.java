package io.github.ramonster1.jbeacon.poll;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

/**
 * PollSelector facilitates the monitoring and processing of readiness states for
 * channels registered with a Selector, using a specified timeout. It is primarily
 * designed for non-blocking polling operations over network channels, particularly
 * those utilizing UDP protocols.
 * <p>
 * This class allows channels to be registered with a Selector instance, and provides
 * a mechanism to process received data or events when a channel becomes ready for
 * reading. The timeout can be configured to define how long the selector waits before
 * returning.
 * <p>
 * PollSelector implements the AutoCloseable interface, ensuring that resources
 * associated with the Selector are released properly when the selector is no longer
 * in use.
 * <p>
 * Key Features:
 * - Monitors a Selector for channels ready for operations, specifically read events.
 * - Processes selected keys with associated attachments, like buffers and callbacks.
 * - Logs received data at both info and debug levels for better observability.
 * - Automatically closes the Selector when the PollSelector is closed.
 * <p>
 * Usage Notes:
 * - The Selector instance provided during construction must be initialized appropriately
 * with channels and their attachments, as PollSelector does not handle this initialization.
 * - The timeout parameter controls how long the select operation blocks. Set this value
 * carefully based on application latency requirements.
 * - Ensure that resources like buffers and attachments are managed correctly outside the
 * PollSelector to avoid memory leaks or unintended behavior.
 *
 * @param selector the selector instance to monitor for channel readiness states
 * @param timeout  the maximum time (in milliseconds) the selector will block while waiting
 *                 for channels to become ready before returning
 */
public record PollSelector(Selector selector, Long timeout) implements AutoCloseable {
	private static final Logger logger = LogManager.getLogger();

	/**
	 * Processes channels registered with the Selector. This method checks for channels that
	 * are ready for read operations, reads data from those channels, and executes the associated
	 * process logic defined in the channel's attachment.
	 *
	 * @throws IOException if an I/O error occurs while selecting channels or reading data
	 */

	public void process() throws IOException {
		if (selector.select(timeout) >= 0) {
			for (SelectionKey selectedKey : selector.selectedKeys()) {
				if (selectedKey.isReadable()) {
					ProcessPollAttachment attachment;
					ByteBuffer buffer;
					try (DatagramChannel channel = (DatagramChannel) selectedKey.channel()) {
						attachment = (ProcessPollAttachment) selectedKey.attachment();
						buffer = attachment.buffer();
						channel.receive(buffer);
					}
					buffer.flip();
					logger.info("Received response buffer: {}", buffer);
					logger.debug("Response buffer: {}", buffer.array());

					attachment.onPollResponseCommand().execute(buffer);
				}
			}
		}
	}

	/**
	 * Closes the underlying Selector instance, releasing any associated resources.
	 *
	 * @throws IOException if an error occurs while closing the Selector
	 */
	@Override
	public void close() throws IOException {
		selector.close();
	}
}
