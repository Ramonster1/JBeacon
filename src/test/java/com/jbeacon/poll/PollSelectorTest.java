package com.jbeacon.poll;

import com.jbeacon.command.OnPollResponseCommand;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Collections;

import static org.mockito.Mockito.*;

class PollSelectorTest {

	@Test
	void testProcessWhenChannelReadyForProcessing() throws IOException {
		Selector selectorMock = Mockito.mock(Selector.class);
		SelectionKey selectionKeyMock = Mockito.mock(SelectionKey.class);
		DatagramChannel channelMock = Mockito.mock(DatagramChannel.class);
		ProcessPollAttachment attachmentMock = Mockito.mock(ProcessPollAttachment.class);
		OnPollResponseCommand commandMock = Mockito.mock(OnPollResponseCommand.class);
		ByteBuffer inBuffer = ByteBuffer.allocate(1024);

		when(selectorMock.select(anyLong())).thenReturn(1);
		when(selectorMock.selectedKeys()).thenReturn(Collections.singleton(selectionKeyMock));
		when(selectionKeyMock.isReadable()).thenReturn(true);
		when(selectionKeyMock.channel()).thenReturn(channelMock);
		when(selectionKeyMock.attachment()).thenReturn(attachmentMock);
		when(attachmentMock.buffer()).thenReturn(inBuffer);
		when(attachmentMock.onPollResponseCommand()).thenReturn(commandMock);

		PollSelector pollSelector = new PollSelector(selectorMock, 5000L);

		pollSelector.process();

		verify(channelMock).receive(inBuffer);
		verify(commandMock).execute(inBuffer);
	}

	@Test
	void testNotProcessingChannelWhenKeyIsNotReadable() throws IOException {
		Selector selectorMock = Mockito.mock(Selector.class);
		SelectionKey selectionKeyMock = Mockito.mock(SelectionKey.class);

		when(selectorMock.select(anyLong())).thenReturn(1);
		when(selectorMock.selectedKeys()).thenReturn(Collections.singleton(selectionKeyMock));
		when(selectionKeyMock.isReadable()).thenReturn(false);

		PollSelector pollSelector = new PollSelector(selectorMock, 5000L);

		pollSelector.process();

		verify(selectionKeyMock, never()).channel();
	}
}