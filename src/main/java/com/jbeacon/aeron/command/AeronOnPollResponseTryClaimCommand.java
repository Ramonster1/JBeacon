package com.jbeacon.aeron.command;

import com.jbeacon.command.OnPollResponseCommand;
import io.aeron.Publication;
import io.aeron.exceptions.AeronException;
import io.aeron.logbuffer.BufferClaim;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;


/**
 * Represents an implementation of the {@link OnPollResponseCommand} interface utilizing the Aeron
 * {@link Publication#tryClaim} mechanism for message delivery.
 * <p>
 * This record facilitates interaction between a flipped {@link ByteBuffer} and an Aeron {@link Publication}
 * by attempting to claim a buffer of a specified size within the publication. If the claim is successful,
 * the buffer content is copied, and the claim is committed. If not, appropriate actions are taken based on
 * the response from the tryClaim operation.
 * <p>
 * The command processes various outcomes of the {@link Publication#tryClaim} operation, including:
 * - Successful claim: Copies data to the claimed buffer and commits it.
 * - NOT_CONNECTED: Logs an informational message indicating the publication is not connected.
 * - BACK_PRESSURED: Logs an informational message when the publication is experiencing backpressure.
 * - ADMIN_ACTION: Retries the operation due to an administrative action (e.g., log rotation).
 * - CLOSED: Throws a fatal {@link AeronException} indicating the publication is closed.
 * - MAX_POSITION_EXCEEDED: Throws a fatal {@link AeronException} indicating the publication has reached the maximum allowed position.
 * - Unrecognized response: Logs a warning with the unknown response value.
 * <p>
 * This command will retry the tryClaim operation in the case of a ADMIN_ACTION until a terminal state is reached or the operation succeeds.
 *
 * @param publication Defines the Aeron publication to operate on.
 * @param bufferClaim The buffer claim instance used for data transfer within the Aeron publication.
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
