package com.jbeacon.poll.scheduling;

import com.jbeacon.poll.PollingService;
import lombok.Builder;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.*;

@Builder
public class PollSchedulingService implements AutoCloseable {
	private static final Logger logger = LogManager.getLogger();

	@Getter
	@Builder.Default private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	@Builder.Default private Long initialDelay = 0L;
	private Long period;
	private TimeUnit timeUnit;
	private PollingService pollingService;

	/**
	 * Schedules the execution of a polling task at a fixed rate and blocks the current thread
	 * until the execution of the scheduled task finishes or an exception occurs.
	 * <p>
	 * The method uses a {@link ScheduledExecutorService} to schedule the execution of the
	 * polling task provided via a {@link PollingService} implementation. The task execution begins
	 * after an initial delay and recurs at a fixed rate defined by the period and time unit.
	 * </p><p>
	 * If the execution of the scheduled task is interrupted or encounters an exception,
	 * it throws a {@link RuntimeException} wrapping the underlying cause.
	 * </p><p>
	 * Preconditions:
	 * - The {@code period} and {@code timeUnit} must be configured correctly prior to executing this method.
	 * - A valid {@link PollingService} must be provided.
	 * </p><p>
	 * Postconditions:
	 * - The polling task will execute periodically on the configured schedule until the
	 *   {@link PollSchedulingService} instance is closed.
	 * </p><p>
	 * Thread Blocking:
	 * - The method blocks indefinitely due to the {@code scheduledFuture.get()} call, which waits
	 *   for the scheduled task to complete execution or for an exception.
	 * </p><p>
	 * Exceptions:
	 * - {@link RuntimeException} wraps an {@link InterruptedException} if the thread waiting on
	 *   the scheduled task's completion is interrupted.
	 * - {@link RuntimeException} wraps an {@link ExecutionException} if the scheduled task execution fails.
	 * </p>
	 */
	public void executePeriodically() {
		executor.scheduleAtFixedRate(() -> {
			try {
				pollingService.poll();
			} catch (IOException e) {
				logger.error("Exception from polling service", e);
				throw new RuntimeException(e);
			}
		}, initialDelay, period, timeUnit);
	}

	@Override
	public void close() {
		executor.shutdown();
	}
}