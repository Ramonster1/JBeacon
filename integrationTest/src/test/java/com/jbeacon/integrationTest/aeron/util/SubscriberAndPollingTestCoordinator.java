package com.jbeacon.integrationTest.aeron.util;

import com.jbeacon.core.command.OnPollResponseCommand;
import com.jbeacon.integrationTest.util.PollingTestService;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.fail;

public class SubscriberAndPollingTestCoordinator {
	private final PollingTestService pollingTestService;
	private AsyncSubscriptionManagementService subscriptionManagementService;

	public SubscriberAndPollingTestCoordinator(PollingTestService pollingTestService) {
		this.pollingTestService = pollingTestService;
	}

	public void executeTest(Supplier<OnPollResponseCommand> commandSupplier, List<SubscriptionService> subscribers) throws ExecutionException, InterruptedException, IOException {
		try {
			subscriptionManagementService = new AsyncSubscriptionManagementService(subscribers);
			subscriptionManagementService.startSubscribers();
			waitForPublicationConnection();

			pollingTestService.createBlockingPollingService(commandSupplier.get());
			pollingTestService.pollRepeatedly();
		} finally {
			subscriptionManagementService.close();
			subscriptionManagementService.throwAssertFailures();
		}
	}

	private void waitForPublicationConnection() throws ExecutionException, InterruptedException {
		CompletableFuture<Boolean> isConnected = CompletableFuture.supplyAsync(() -> {
			while (!subscriptionManagementService.isConnected()) {
				try {
					TimeUnit.MILLISECONDS.sleep(100);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
			return true;
		});

		isConnected.completeOnTimeout(false, 10, TimeUnit.SECONDS);

		if (!isConnected.get()) {
			fail("Failed to connect to Aeron publication");
		}
	}

}
