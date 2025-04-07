package com.jbeacon.poll.scheduling;

import com.jbeacon.poll.PollingService;
import lombok.Builder;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A service for scheduling periodic polling tasks using a {@link ScheduledExecutorService}.
 * <p>
 * The {@code PollSchedulingService} allows configurable periodic execution of a polling task.
 * It uses a single-threaded {@link ScheduledExecutorService} to schedule tasks at a fixed rate
 * defined by the user. This class should be constructed using its {@code @Builder} annotation and
 * provides a closeable lifecycle for proper resource management.
 * <p>
 * Key Features:
 * - Configurable polling schedule with an initial delay, period, and time unit.
 * - Dependency on a {@link PollingService} implementation for the actual polling logic.
 * - Graceful shutdown of the executor service upon closing the instance.
 * - Thread-blocking behavior in the {@code executePeriodically} method for ensuring periodic execution.
 * <p>
 * Usage Notes:
 * - Ensure all required fields such as {@code period}, {@code timeUnit}, and {@code pollingService}
 * are initialized before use.
 * - Properly handle exceptions during polling as they are logged and wrapped into runtime exceptions.
 * - Always close the instance after use to release resources and stop the executor service.
 * <p>
 * Exception Handling:
 * - Any {@link IOException} thrown by the polling service during execution is logged and rethrown
 * as a {@link RuntimeException}.
 * - After the service is closed, the executor will no longer accept new tasks.
 * <p>
 * Implements:
 * - {@link AutoCloseable} to facilitate correct cleanup of resources when used in a try-with-resources block.
 */
@Builder
public class PollSchedulingService implements AutoCloseable {
	private static final Logger logger = LogManager.getLogger();

	@Getter
	@Builder.Default
	private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	@Builder.Default
	private Long initialDelay = 0L;
	private Long period;
	private TimeUnit timeUnit;
	private PollingService pollingService;


	/**
	 * Constructs a new instance of PollSchedulingService with specified configurations
	 * for scheduling and executing periodic polling tasks.
	 *
	 * @param executor       the ScheduledExecutorService instance used to schedule and execute tasks
	 * @param initialDelay   the delay, in the specified time unit, before the first execution of the polling task
	 * @param period         the period, in the specified time unit, between successive executions of the polling task
	 * @param timeUnit       the time unit for the initialDelay and period
	 * @param pollingService the PollingService implementation used to perform polling operations
	 */
	public PollSchedulingService(ScheduledExecutorService executor, Long initialDelay, Long period, TimeUnit timeUnit, PollingService pollingService) {
		this.executor = executor;
		this.initialDelay = initialDelay;
		this.period = period;
		this.timeUnit = timeUnit;
		this.pollingService = pollingService;
	}

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
	 * {@link PollSchedulingService} instance is closed.
	 * </p><p>
	 * Thread Blocking:
	 * - The method blocks indefinitely due to the {@code scheduledFuture.get()} call, which waits
	 * for the scheduled task to complete execution or for an exception.
	 * </p><p>
	 * Exceptions:
	 * - {@link RuntimeException} wraps an {@link InterruptedException} if the thread waiting on
	 * the scheduled task's completion is interrupted.
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