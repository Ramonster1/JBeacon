package com.jbeacon.poll;

import java.io.IOException;

/**
 * Provides a mechanism for polling a resource or service.
 * <p>
 * The `PollingService` interface defines a contract for implementing services
 * capable of performing polling operations, potentially to communicate with
 * external systems or perform periodic checks.
 */
public interface PollingService {

	/**
	 * Performs a polling operation, potentially communicating with an external service
	 * or system. This method is part of the contract defined by the `PollingService`
	 * interface.
	 * <p>
	 * Implementations of this method may utilize different modes of operation, such as
	 * blocking or non-blocking, depending on the underlying configuration or
	 * implementation. The polling behavior may vary by implementation but generally
	 * involves sending a request and processing a response.
	 *
	 * @throws IOException if an I/O error occurs during the polling operation
	 */
	void poll() throws IOException;
}
