package com.jbeacon.aeron.command;

import io.aeron.Publication;
import org.agrona.BufferUtil;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class AeronOnPollResponseOfferCommandTest {
	private static final UnsafeBuffer BUFFER = new UnsafeBuffer(BufferUtil.allocateDirectAligned(256, 64));

	@Test
	void testOfferResponseCallbackOnBackPressureFailure() {
		try (Publication publicationMock = Mockito.mock(Publication.class)) {
			var onPollResponseTest = new AeronOnPollResponseOfferCommand(publicationMock, BUFFER,
					(resultingPosition) -> assertEquals(Publication.BACK_PRESSURED, resultingPosition));
			byte[] bytes = "Test message".getBytes();
			ByteBuffer testBuffer = ByteBuffer.wrap(bytes);
			BUFFER.wrap(ByteBuffer.wrap(bytes));

			long expectedPosition = Publication.BACK_PRESSURED;
			when(publicationMock.offer(BUFFER, 0, bytes.length)).thenReturn(expectedPosition);

			onPollResponseTest.execute(testBuffer);
		}
	}
}
