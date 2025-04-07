package com.jbeacon.command;

import java.nio.ByteBuffer;


/**
 * Represents a command that processes the response received during polling.
 * This functional interface defines a method for handling a {@link ByteBuffer} that contains
 * the data received from a UDP poll response. When used with a PollingService, the buffer is
 * already flipped (limit and position fields are set for reading/processing).
 * <p>
 * Example usage:
 * <pre>
 * OnPollResponseCommand onPollResponseCommand = responseBuffer -> {
 *     while (responseBuffer.hasRemaining()) {
 *         // Process each byte from the buffer
 *         byte data = buffer.get();
 *         // Handle the data (e.g., log, parse, or store it)
 *     }
 * };
 * </pre>
 */

@FunctionalInterface
public interface OnPollResponseCommand {

	/**
	 * Processes the given flipped {@link ByteBuffer} containing data from a UDP poll response.
	 * Implementations define the specific behavior for handling the buffer, such as parsing,
	 * storing, or forwarding the information. The buffer's position and limit are set for reading.
	 *
	 * @param buffer the flipped {@link ByteBuffer} containing the data to process.
	 */
	void execute(ByteBuffer buffer);
}
