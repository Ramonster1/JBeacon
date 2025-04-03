package com.jbeacon.aeron.command;

import io.aeron.Publication;
import io.aeron.exceptions.AeronException;
import io.aeron.logbuffer.BufferClaim;
import org.agrona.MutableDirectBuffer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.ByteBuffer;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AeronOnPollResponseTryClaimCommandTest {

	@Mock
	private Publication mockPublication;
	@Mock
	private BufferClaim mockBufferClaim;
	@Mock
	private MutableDirectBuffer mockDirectBuffer;
	private final ByteBuffer testBuffer = ByteBuffer.wrap("testData".getBytes());

	@Test
	void testExecute_SuccessfulClaim() {
		when(mockPublication.tryClaim(testBuffer.limit(), mockBufferClaim)).thenReturn(1L);
		when(mockBufferClaim.buffer()).thenReturn(mockDirectBuffer);

		AeronOnPollResponseTryClaimCommand command = new AeronOnPollResponseTryClaimCommand(mockPublication, mockBufferClaim);

		command.execute(testBuffer);

		verify(mockPublication).tryClaim(testBuffer.limit(), mockBufferClaim);
		verify(mockBufferClaim).commit();
	}

	@Test
	void testExecute_AdminAction() {
		when(mockPublication.tryClaim(testBuffer.limit(), mockBufferClaim))
				.thenReturn(Publication.ADMIN_ACTION)
				.thenReturn(1L);
		when(mockBufferClaim.buffer()).thenReturn(mockDirectBuffer);

		AeronOnPollResponseTryClaimCommand command = new AeronOnPollResponseTryClaimCommand(mockPublication, mockBufferClaim);

		command.execute(testBuffer);

		verify(mockPublication, times(2)).tryClaim(testBuffer.limit(), mockBufferClaim);
		verify(mockBufferClaim).commit();
	}

	@Test
	void testExecute_PublicationClosed() {
		when(mockPublication.tryClaim(testBuffer.limit(), mockBufferClaim)).thenReturn(Publication.CLOSED);

		AeronOnPollResponseTryClaimCommand command = new AeronOnPollResponseTryClaimCommand(mockPublication, mockBufferClaim);

		org.junit.jupiter.api.Assertions.assertThrows(
				AeronException.class,
				() -> command.execute(testBuffer),
				"Publication is closed"
		);
	}

	@Test
	void testExecute_MaxPositionExceeded() {
		when(mockPublication.tryClaim(testBuffer.limit(), mockBufferClaim)).thenReturn(Publication.MAX_POSITION_EXCEEDED);

		AeronOnPollResponseTryClaimCommand command = new AeronOnPollResponseTryClaimCommand(mockPublication, mockBufferClaim);

		org.junit.jupiter.api.Assertions.assertThrows(
				AeronException.class,
				() -> command.execute(testBuffer),
				"Publication reached max position"
		);
	}
}