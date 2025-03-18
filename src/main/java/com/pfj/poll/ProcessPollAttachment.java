package com.pfj.poll;

import com.pfj.command.PollResponseCommand;

import java.nio.ByteBuffer;

public record ProcessPollAttachment(PollResponseCommand pollResponseCommand, ByteBuffer buffer) {
}
