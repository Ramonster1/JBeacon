package io.github.ramonster1.jbeacon.aeron.util;


import java.util.concurrent.TimeUnit;

/**
 * A helper class to run JUnit assertions asynchronously within a separate thread.
 * <p>
 * If running assertions in a separate thread spawned from the testing thread, then if an AssertionError will be
 * thrown, it will not fail the unit test. This helper class will catch and rethrow the AssertionError in the main test
 * thread to ensure a test is failed by an AssertionError.
 */
public class AsyncAssertionExecutor {

	public static final long WAIT_TIME_MILLIS = TimeUnit.SECONDS.toMillis(15);
	private final Thread thread;
	private AssertionError error;

	public AsyncAssertionExecutor(final Runnable runnable) {
		thread = new Thread(() -> {
			try {
				runnable.run();
			} catch (AssertionError e) {
				error = e;
			}
		});
	}

	public void start() {
		thread.setDaemon(true);
		thread.start();
	}

	public void waitForCompletionAndAssert() throws InterruptedException {
		thread.join(WAIT_TIME_MILLIS);

		if (error != null)
			throw error;
	}
}
