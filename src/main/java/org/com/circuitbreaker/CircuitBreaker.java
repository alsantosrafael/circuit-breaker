package org.com.circuitbreaker;

public class CircuitBreaker {
	private CircuitStatus status = CircuitStatus.CLOSED;

	private int failureCounter = 0;
	private final int failureThreshold;
	private final long retryTimeoutMillis;
	private long lastFailureTime = 0;
	public CircuitBreaker(int failureThreshold, long retryTimeoutMillis) {
		this.failureThreshold = failureThreshold;
		this.retryTimeoutMillis = retryTimeoutMillis;
	}

	public boolean allowRequest() {
		if(this.status == CircuitStatus.OPEN) {
			if(System.currentTimeMillis() - lastFailureTime > retryTimeoutMillis) {
				status = CircuitStatus.HALF_OPEN;
				return true;
			}

			return false;
		}
		return true;
	}

	public void recordSuccess() {
		this.lastFailureTime = 0;
		this.failureCounter = 0;
		status = CircuitStatus.CLOSED;
	}

	public void recordFailure() {
		this.lastFailureTime = System.currentTimeMillis();
		this.failureCounter++;

		if(this.failureCounter == this.failureThreshold) {
			this.status = CircuitStatus.OPEN;
		}
	}
}
