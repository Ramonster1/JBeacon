package io.github.ramonster1.jbeacon.aeron.command;

import io.github.ramonster1.jbeacon.command.OnPollResponseCommand;
import io.aeron.Publication;
import io.aeron.exceptions.AeronException;
import org.agrona.DirectBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;


/**
 * Represents an implementation of the {@link OnPollResponseCommand} interface utilizing the Aeron
 * {@link Publication#offer} mechanism for message delivery.
 * <p>
 * This record facilitates interaction between a flipped {@link ByteBuffer} and an Aeron {@link Publication}
 * by offering the data contained in the byte buffer to the publication. It handles various states of the
 * Aeron publication and acts accordingly based on the response.
 * <p>
 * The command processes various outcomes of the {@link Publication#offer} operation, including:
 * - Successful offer: The data is delivered to the publication.
 * - NOT_CONNECTED: Logs an informational message indicating the publication is not connected.
 * - BACK_PRESSURED: Logs an informational message when the publication is experiencing backpressure.
 * - ADMIN_ACTION: Retries the operation due to an administrative action (e.g., log rotation).
 * - CLOSED: Throws a fatal {@link AeronException} indicating the publication is closed.
 * - MAX_POSITION_EXCEEDED: Throws a fatal {@link AeronException} indicating the publication has reached the maximum allowed position.
 * - Unrecognized response: Logs a warning with the unknown response value.
 * <p>
 * This command will retry the offer operation in the case of a ADMIN_ACTION until a terminal state is reached or the operation succeeds.
 *
 * @param publication  Defines the Aeron publication to operate on.
 * @param directBuffer The Aeron {@link DirectBuffer} that wraps the {@link ByteBuffer} for data transfer within the publication.
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
