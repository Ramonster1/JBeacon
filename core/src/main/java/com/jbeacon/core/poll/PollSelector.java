package com.jbeacon.core.poll;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

public record PollSelector(Selector selector, Long timeout) implements AutoCloseable {
	private static final Logger logger = LogManager.getLogger();

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

	@Override
	public void close() throws Exception {
		selector.close();
	}
}
