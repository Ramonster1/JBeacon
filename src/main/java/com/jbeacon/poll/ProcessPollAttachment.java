package com.jbeacon.poll;

import com.jbeacon.command.PollResponseCommand;

import java.nio.ByteBuffer;

public record ProcessPollAttachment(PollResponseCommand pollResponseCommand, ByteBuffer buffer) {
}
