package com.jbeacon.aeron.command;

import io.aeron.Publication;
import io.aeron.exceptions.AeronException;
import org.agrona.MutableDirectBuffer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.ByteBuffer;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AeronOnPollResponseOfferCommandTest {
	@Mock
	private Publication mockPublication;
	@Mock
	private MutableDirectBuffer mockDirectBuffer;
	private final ByteBuffer testBuffer = ByteBuffer.wrap("testData".getBytes());

	@Test
	void testExecute_SuccessfulClaim() {
		when(mockPublication.offer(mockDirectBuffer, 0, testBuffer.limit())).thenReturn(1L);

		AeronOnPollResponseOfferCommand command = new AeronOnPollResponseOfferCommand(mockPublication, mockDirectBuffer);
		command.execute(testBuffer);

		verify(mockPublication).offer(mockDirectBuffer,  0, testBuffer.limit());
	}

	@Test
	void testExecute_AdminAction() {
		when(mockPublication.offer(mockDirectBuffer, 0, testBuffer.limit()))
				.thenReturn(Publication.ADMIN_ACTION)
				.thenReturn(1L);

		AeronOnPollResponseOfferCommand command = new AeronOnPollResponseOfferCommand(mockPublication, mockDirectBuffer);
		command.execute(testBuffer);

		verify(mockPublication, times(2)).offer(mockDirectBuffer, 0, testBuffer.limit());
	}

	@Test
	void testExecute_PublicationClosed() {
		when(mockPublication.offer(mockDirectBuffer, 0, testBuffer.limit())).thenReturn(Publication.CLOSED);

		AeronOnPollResponseOfferCommand command = new AeronOnPollResponseOfferCommand(mockPublication, mockDirectBuffer);

		org.junit.jupiter.api.Assertions.assertThrows(
				AeronException.class,
				() -> command.execute(testBuffer),
				"Publication is closed"
		);
	}

	@Test
	void testExecute_MaxPositionExceeded() {
		when(mockPublication.offer(mockDirectBuffer, 0, testBuffer.limit())).thenReturn(Publication.MAX_POSITION_EXCEEDED);

		AeronOnPollResponseOfferCommand command = new AeronOnPollResponseOfferCommand(mockPublication, mockDirectBuffer);

		org.junit.jupiter.api.Assertions.assertThrows(
				AeronException.class,
				() -> command.execute(testBuffer),
				"Publication reached max position"
		);
	}
}
