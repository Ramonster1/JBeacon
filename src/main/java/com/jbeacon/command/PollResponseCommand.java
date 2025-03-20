package com.jbeacon.command;

import java.nio.ByteBuffer;

/**
 * Represents an operation encapsulated as a command to be executed, following the command behavioral design pattern.
 * The command is applied to a flipped {@link ByteBuffer}.
 */
@FunctionalInterface
public interface PollResponseCommand {

	void execute(ByteBuffer buffer);
}
