package com.jbeacon.aeron.command;

import com.jbeacon.command.OnPollResponseCommand;
import io.aeron.Publication;
import org.agrona.DirectBuffer;

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
											  DirectBuffer directBuffer,
											  Consumer<Long> offerResponseCallback) implements OnPollResponseCommand {

	public AeronOnPollResponseOfferCommand(Publication publication, DirectBuffer directBuffer) {
		this(publication, directBuffer, null);
	}

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

		final long resultingPosition = publication.offer(directBuffer, 0, byteBuffer.limit());
		if (offerResponseCallback != null) {
			offerResponseCallback.accept(resultingPosition);
		}
	}
}
