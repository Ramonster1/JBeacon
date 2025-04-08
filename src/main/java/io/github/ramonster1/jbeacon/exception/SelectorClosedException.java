package io.github.ramonster1.jbeacon.exception;

import java.io.IOException;

/**
 * Thrown to indicate that an operation was attempted on a {@code Selector} that has
 * already been closed. This exception is typically used to indicate that a non-blocking
 * operation cannot proceed because the associated {@code Selector} is no longer available.
 * <p>
 * Instances of this exception are generally used in the context of non-blocking I/O
 * operations where a {@code Selector} is employed to monitor channels for readiness
 * states, such as read or write operations. If the {@code Selector} has been closed
 * prior to or during the operation, this exception will signal the failure.
 */
public class SelectorClosedException extends IOException {


	/**
	 * Constructs a new {@code SelectorClosedException} with the specified detail message.
	 * This exception is typically thrown when an operation is attempted on a {@code Selector}
	 * that has already been closed, indicating that the operation cannot proceed.
	 *
	 * @param msg the detail message, providing additional context about the exception
	 */
	public SelectorClosedException(String msg) {
		super(msg);
	}
}
