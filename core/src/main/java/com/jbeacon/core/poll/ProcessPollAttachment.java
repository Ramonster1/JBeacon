package com.jbeacon.core.poll;

import com.jbeacon.core.command.OnPollResponseCommand;

import java.nio.ByteBuffer;

public record ProcessPollAttachment(OnPollResponseCommand onPollResponseCommand, ByteBuffer buffer) {
}
