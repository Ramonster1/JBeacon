package io.github.ramonster1.jbeacon.aeron.util;

import java.util.ArrayList;
import java.util.List;


/**
 * A service class for managing subscription operations asynchronously.
 * This class allows initializing, handling, and closing resources related to multiple subscriptions
 * and ensures proper completion of asynchronous assertion execution.
 * <p>
 * Responsibilities:
 * - Initializes and manages a list of asynchronous subscription executions.
 * - Coordinates resource allocation and cleanup.
 * - Ensures proper assertion handling for asynchronous operations.
 */
public class AsyncSubscriptionManagementService implements AutoCloseable {
	private final List<SubscriptionService> subscribers;
	private final List<AsyncAssertionExecutor> asyncExecutors = new ArrayList<>();

	public AsyncSubscriptionManagementService(List<SubscriptionService> subscribers) {
		this.subscribers = subscribers;

		for (SubscriptionService subscriber : subscribers) {
			asyncExecutors.add(new AsyncAssertionExecutor(subscriber::startSubscribing));
		}
	}

	public void startSubscribers() {
		for (AsyncAssertionExecutor executor : asyncExecutors) {
			executor.start();
		}
	}

	public boolean isConnected() {
		for (SubscriptionService subscriber : subscribers) {
			if (!subscriber.isConnected()) {
				return false;
			}
		}

		return true;
	}


	@Override
	public void close() throws InterruptedException {
		for (SubscriptionService subscriber : subscribers) {
			subscriber.close();
		}
	}

	// Should be run after close
	public void throwAssertFailures() throws InterruptedException {
		for (AsyncAssertionExecutor executor : asyncExecutors) {
			executor.waitForCompletionAndAssert();
		}
	}
}
