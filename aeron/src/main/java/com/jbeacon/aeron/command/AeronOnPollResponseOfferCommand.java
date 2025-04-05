package com.jbeacon.aeron.command;

import com.jbeacon.core.command.OnPollResponseCommand;
import io.aeron.Publication;
import io.aeron.exceptions.AeronException;
import org.agrona.DirectBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;


/**
 * Represents an implementation of the {@link OnPollResponseCommand} interface that encapsulates a command
 * using the Aeron {@link Publication#offer} mechanism for message delivery.
 * <p>
 * This record facilitates interaction between a flipped {@link ByteBuffer} and an Aeron {@link Publication}
 * by wrapping the buffer content into a {@link DirectBuffer} and making an offer call to the publication.
 * The command processes the result of the {@link Publication#offer} operation and handles various outcomes,
 * including scenarios such as backpressure, connection issues, administrative actions, or reaching position limits.
 * <p>
 * In the event of a transient state like an administrative action, the command retries the operation until
 * a terminal state is reached or the operation succeeds. Terminal states such as a closed publication or
 * exceeding the maximum allowed position result in exceptions being thrown.
 * <p>
 * If Publication.BACK_PRESSURED is returned from offer(), then the response from the poll will be ignored.
 */
public record AeronOnPollResponseOfferCommand(Publication publication,
											  DirectBuffer directBuffer) implements OnPollResponseCommand {
	private static final Logger logger = LogManager.getLogger();

	@Override
	public void execute(ByteBuffer byteBuffer) {
		directBuffer.wrap(byteBuffer, byteBuffer.position(), byteBuffer.limit());

		final long response = publication.offer(directBuffer, 0, byteBuffer.limit());

		if (response == Publication.NOT_CONNECTED) {
			logger.info("Publication is not connected");
		} else if (response == Publication.BACK_PRESSURED) {
			logger.info("Publication is back pressured");
		} else if (response == Publication.ADMIN_ACTION) {
			logger.info("Publication is in admin action. Attempting a retry");

			// Retry offer(). The action is an operation such as log rotation which is likely to have succeeded by the next retry attempt.
			execute(byteBuffer);
		} else if (response == Publication.CLOSED) {
			logger.warn("Publication is closed");

			throw new AeronException("Publication is closed", AeronException.Category.FATAL);
		} else if (response == Publication.MAX_POSITION_EXCEEDED) {
			logger.info("Publication reached max position");

			throw new AeronException("Publication reached max position", AeronException.Category.FATAL);
		} else {
			logger.warn("Unknown try claim response: {}", response);
		}
	}
}
