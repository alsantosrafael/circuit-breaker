package org.com.circuitbreaker.exception;

public class InvalidCircuitBreakerValueException extends RuntimeException {
	public InvalidCircuitBreakerValueException(String message) {
		super(message);
	}
}
