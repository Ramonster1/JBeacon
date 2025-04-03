package com.jbeacon.aeron.command;

import com.jbeacon.command.OnPollResponseCommand;
import io.aeron.Publication;
import io.aeron.exceptions.AeronException;
import org.agrona.DirectBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.util.function.Consumer;


/**
 * Implements the {@link OnPollResponseCommand} interface to encapsulate a command
 * that interacts with Aeron publications by offering data contained in a {@link ByteBuffer}.
 * <p>
 * The command uses an Aeron {@link Publication} to send data provided through a {@link ByteBuffer}
 * wrapped into a {@link DirectBuffer}. The result of the {@link Publication#offer} operation
 * can be captured using an optional callback {@link Consumer} that processes the resulting position.
 * <p>
 * This record is particularly useful for processing and publishing incoming data in Aeron-based
 * UDP polling mechanisms or systems where responses need to be offered directly to an Aeron publication.
 */
public record AeronOnPollResponseOfferCommand(Publication publication,
											  DirectBuffer directBuffer) implements OnPollResponseCommand {
	private static final Logger logger = LogManager.getLogger();

	/**
	 * Executes the command by wrapping the provided {@link ByteBuffer} into a {@link DirectBuffer}
	 * and offering its data via the Aeron {@link Publication}.
	 * <p>
	 * When calling Publication.offer() a return value greater than 0 indicates the message was sent.
	 * Negative values indicate that the message has not been enqueued for sending.The result of the offer operation
	 * can be optionally processed using the provided callback.
	 *
	 * @param byteBuffer the {@link ByteBuffer} containing the data to be offered via the {@link Publication}.
	 *                   The buffer's current position and limit are used for the offer operation.
	 */
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
