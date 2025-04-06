package com.jbeacon.poll;

import com.jbeacon.command.OnPollResponseCommand;

import java.nio.ByteBuffer;

public record ProcessPollAttachment(OnPollResponseCommand onPollResponseCommand, ByteBuffer buffer) {
}
