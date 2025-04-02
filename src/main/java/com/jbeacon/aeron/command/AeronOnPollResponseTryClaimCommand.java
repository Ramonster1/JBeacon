package com.jbeacon.aeron.command;

import com.jbeacon.command.OnPollResponseCommand;
import io.aeron.Publication;
import io.aeron.exceptions.AeronException;
import io.aeron.logbuffer.BufferClaim;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;


public record AeronOnPollResponseTryClaimCommand(Publication publication,
												 BufferClaim bufferClaim) implements OnPollResponseCommand {
	private static final Logger logger = LogManager.getLogger();

	@Override
	public void execute(ByteBuffer byteBuffer) {
		long tryClaimResponse = publication.tryClaim(byteBuffer.limit(), bufferClaim);
		if (tryClaimResponse > 0L) {
			try {
				bufferClaim.buffer().putBytes(bufferClaim.offset(), byteBuffer.array(), byteBuffer.position(), byteBuffer.limit());
			} finally {
				bufferClaim.commit();
			}
		} else if (tryClaimResponse == Publication.NOT_CONNECTED) {
			logger.info("Publication is not connected");
		} else if (tryClaimResponse == Publication.BACK_PRESSURED) {
			logger.info("Publication is back pressured");
		} else if (tryClaimResponse == Publication.ADMIN_ACTION) {
			logger.info("Publication is in admin action. Attempting a retry");

			// Retry tryClaim(). The action is an operation such as log rotation which is likely to have succeeded by the next retry attempt.
			execute(byteBuffer);
		} else if (tryClaimResponse == Publication.CLOSED) {
			logger.warn("Publication is closed");

			throw new AeronException("Publication is closed", AeronException.Category.FATAL);
		} else if (tryClaimResponse == Publication.MAX_POSITION_EXCEEDED) {
			logger.info("Publication reached max position");

			throw new AeronException("Publication reached max position", AeronException.Category.FATAL);
		} else {
			logger.warn("Unknown try claim response: {}", tryClaimResponse);
		}
	}
}
