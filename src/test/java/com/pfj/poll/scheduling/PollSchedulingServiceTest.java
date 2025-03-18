package com.pfj.poll.scheduling;

import com.pfj.poll.PollingService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;

class PollSchedulingServiceTest {

	@Test
	void testExecutePeriodically() throws InterruptedException, IOException {
		// Arrange
		PollingService pollingServiceMock = mock(PollingService.class);
		ScheduledExecutorService mockExecutor = Executors.newScheduledThreadPool(1);

		try (var pollSchedulingService = PollSchedulingService.builder()
				.executor(mockExecutor)
				.pollingService(pollingServiceMock)
				.initialDelay(0L)
				.period(100L)
				.timeUnit(TimeUnit.MILLISECONDS)
				.build()) {

			// Act
			pollSchedulingService.executePeriodically();

			// Wait for a short while to allow periodic execution
			Thread.sleep(300);

			// Assert
			verify(pollingServiceMock, atLeast(2)).poll();
		}
	}

	@Test
	void testExceptionHandling() throws InterruptedException, IOException {
		// Arrange
		PollingService pollingServiceMock = mock(PollingService.class);
		ScheduledExecutorService mockExecutor = Executors.newScheduledThreadPool(1);

		// Make poll throw an exception
		doThrow(new IOException("Test IOException")).when(pollingServiceMock).poll();

		try (var pollSchedulingService = PollSchedulingService.builder()
				.executor(mockExecutor)
				.pollingService(pollingServiceMock)
				.initialDelay(0L)
				.period(100L)
				.timeUnit(TimeUnit.MILLISECONDS)
				.build()) {

			// Act
			pollSchedulingService.executePeriodically();

			// Wait for a short while to allow error logging
			Thread.sleep(200);

			// Assert
			verify(pollingServiceMock, atLeast(1)).poll();
		}
	}

	@Test
	void testShutdownBehavior() {
		// Arrange
		PollingService pollingServiceMock = mock(PollingService.class);
		ScheduledExecutorService mockExecutor = mock(ScheduledExecutorService.class);

		try (var ignored = PollSchedulingService.builder()
				.executor(mockExecutor)
				.pollingService(pollingServiceMock)
				.initialDelay(0L)
				.period(100L)
				.timeUnit(TimeUnit.MILLISECONDS)
				.build()) {
		}

		// Assert
		verify(mockExecutor, times(1)).shutdown();
	}
}