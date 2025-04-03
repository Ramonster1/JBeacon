package com.jbeacon.aeron.command;

import com.jbeacon.command.OnPollResponseCommand;
import io.aeron.Publication;
import io.aeron.exceptions.AeronException;
import io.aeron.logbuffer.BufferClaim;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;


/**
 * Represents an implementation of the {@link OnPollResponseCommand} interface that encapsulates a command
 * using the Aeron {@link Publication#tryClaim} mechanism for efficient message delivery with a claimed buffer section.
 * <p>
 * This record facilitates an interaction between a flipped {@link ByteBuffer} and an Aeron {@link Publication}
 * by claiming a section of the Aeron publication buffer and writing the provided ByteBuffer content directly into it.
 * After writing, the buffer claim is committed to finalize the message delivery.
 * <p>
 * The command processes the result of the {@link Publication#tryClaim} operation and handles various scenarios such as
 * publication backpressure, connection issues, or reaching position limits. In cases of transient states like
 * administrative actions, the command retries the operation. Critical states like a closed publication or position overflow result
 * in an exception being thrown.
 * <p>
 * This record is particularly useful in Aeron-based systems to efficiently send incoming data by directly utilizing
 * a claimed segment of the Aeron publication buffer.
 */
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
