package com.jbeacon.poll;

import com.jbeacon.command.OnPollResponseCommand;

import java.nio.ByteBuffer;

/**
 * Represents the attachment used during the processing of selected keys in a {@link PollSelector}.
 * This record holds the data buffer and the command to be executed when a poll response is received.
 * It is designed to facilitate the handling of polling events and the subsequent processing of data.
 * <p>
 * The {@link OnPollResponseCommand} is responsible for defining the behavior upon receiving a
 * response, enabling custom processing of the flipped {@link ByteBuffer}.
 * <p>
 * This class is typically used as an attachment object when registering a {@link java.nio.channels.DatagramChannel}
 * with a {@link java.nio.channels.Selector} in non-blocking polling scenarios. The {@link ByteBuffer} is flipped for reading
 * before being provided to the associated command's execution method.
 * <p>
 * Key responsibilities:
 * - Provide storage for a {@link ByteBuffer} to hold received data.
 * - Define the command to execute upon processing the response.
 * <p>
 * Thread Safety:
 * - Instances of this record are immutable if the associated {@link ByteBuffer} is not modified externally.
 * <p>
 * Usage Context:
 * - Associated with {@link java.nio.channels.SelectionKey#attachment()} in non-blocking UDP polling mechanisms.
 * - Enables decoupled processing logic via the {@link OnPollResponseCommand} interface.
 *
 * @param onPollResponseCommand The command that defines the behavior to execute when a poll response is received.
 * @param buffer A {@link ByteBuffer} used to store data received during the polling process.
 */
public record ProcessPollAttachment(OnPollResponseCommand onPollResponseCommand, ByteBuffer buffer) {
}
