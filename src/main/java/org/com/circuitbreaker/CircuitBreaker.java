	package org.com.circuitbreaker;

	import org.com.circuitbreaker.exception.CircuitBreakerExecutionException;
	import org.com.circuitbreaker.exception.CircuitBreakerOpenException;

	import java.util.List;
	import java.util.concurrent.CopyOnWriteArrayList;
	import java.util.concurrent.atomic.AtomicInteger;
	import java.util.concurrent.atomic.AtomicReference;
	import java.util.function.Supplier;

	/**
	 * Implements a simple Circuit Breaker pattern with exponential backoff for retry timeouts.
	 * <p>
	 * Thread Safety: This implementation uses lock-free atomic operations via Compare-And-Swap (CAS)
	 * for state transitions and counters, ensuring high-performance thread safety without synchronization overhead.
	 * State changes use {@link AtomicReference#compareAndSet(Object, Object)} and counter operations use
	 * {@link AtomicInteger} methods to avoid blocking and contention.
	 * <p>
	 * Features:
	 * <ul>
	 *   <li>HALF_OPEN state for trial requests after timeout</li>
	 *   <li>Exponential backoff for retry timeouts up to a maximum</li>
	 *   <li>Lock-free thread-safe state transitions using atomic CAS operations</li>
	 *   <li>High-performance concurrent access without synchronization blocks</li>
	 *   <li>Event listener support for state change notifications</li>
	 *   <li>Builder pattern for flexible configuration</li>
	 *   <li>Convenient execute methods with automatic circuit breaker handling</li>
	 *   <li>Support for both fallback and exception-based error handling</li>
	 * </ul>
	 * <p>
	 * Example usage:
	 * <pre>{@code
	 * // Using constructor
	 * CircuitBreaker cb = new CircuitBreaker(5, 1000, 8);
	 *
	 * // Using builder pattern
	 * CircuitBreaker cb = CircuitBreaker.newBuilder()
	 *     .withFailureThreshold(5)
	 *     .withRetryTimeoutMillis(1000)
	 *     .withMaxRetryFactor(8)
	 *     .build();
	 *
	 * // Execute with automatic fallback
	 * String result = cb.execute(
	 *     () -> externalService.call(),
	 *     () -> "fallback result"
	 * );
	 *
	 * // Execute with exception handling
	 * try {
	 *     String result = cb.execute(() -> externalService.call());
	 * } catch (CircuitBreakerOpenException e) {
	 *     // Handle circuit open
	 * } catch (CircuitBreakerExecutionException e) {
	 *     // Handle execution failure
	 * }
	 *
	 * // Adding listeners
	 * cb.addListener(new CircuitBreakerListener() {
	 *     public void onOpen() { System.out.println("Circuit opened"); }
	 *     public void onHalfOpen() { System.out.println("Circuit half-open"); }
	 *     public void onClose() { System.out.println("Circuit closed"); }
	 * });
	 * }</pre>
	 */
	public class CircuitBreaker {
		private AtomicReference<CircuitState> state = new AtomicReference<>(CircuitState.CLOSED);
		private AtomicInteger failureCounter = new AtomicInteger(0);
		private final int failureThreshold;
		private final long retryTimeOutMillis;
		private volatile long lastFailureTime = 0;
		private volatile long currentRetryTimeoutMillis;
		private final long maxRetryTimeoutMillis;

		/**
		 * Constructs a CircuitBreaker with the specified failure threshold and retry timeout.
		 * Supports exponential backoff for retry timeouts up to a maximum factor.
		 *
		 * @param failureThreshold the number of failures before opening the circuit
		 * @param retryTimeOutMillis the initial timeout in milliseconds before allowing a retry
		 * @param maxRetryFactor the maximum multiplier for exponential backoff of retry timeout
		 */
		public CircuitBreaker(int failureThreshold, long retryTimeOutMillis, int maxRetryFactor) {
			this.failureThreshold = failureThreshold;
			this.retryTimeOutMillis = retryTimeOutMillis;
			this.currentRetryTimeoutMillis = retryTimeOutMillis;
			this.maxRetryTimeoutMillis = retryTimeOutMillis * maxRetryFactor;
		}

		/**
		 * Executes the given supplier with circuit breaker protection and automatic fallback.
		 * <p>
		 * This method handles all circuit breaker logic automatically:
		 * <ul>
		 *   <li>If the circuit is OPEN, the fallback supplier is executed immediately</li>
		 *   <li>If the circuit allows the request, the main supplier is executed</li>
		 *   <li>On success, the circuit is notified and the result is returned</li>
		 *   <li>On failure, the circuit is notified and the fallback supplier is executed</li>
		 * </ul>
		 * <p>
		 * This method never throws exceptions - it always returns a result from either
		 * the main supplier or the fallback supplier.
		 *
		 * @param <T> the type of result returned by both suppliers
		 * @param supplier the main operation to execute
		 * @param fallBack the fallback operation to execute when the circuit is open or main operation fails
		 * @return the result from either the main supplier or fallback supplier
		 */
		public <T> T execute(Supplier<T> supplier, Supplier<T> fallBack) {
			if(!this.allowRequest()) {
				return fallBack.get();
			}
			try {
				T result = supplier.get();
				this.recordSuccess();
				return result;
			} catch (Exception e) {
				this.recordFailure();
				return fallBack.get();
			}
		}

		/**
		 * Executes the given supplier with circuit breaker protection, throwing exceptions on failure.
		 * <p>
		 * This method provides fail-fast behavior:
		 * <ul>
		 *   <li>If the circuit is OPEN, throws {@link CircuitBreakerOpenException} immediately</li>
		 *   <li>If the circuit allows the request, the supplier is executed</li>
		 *   <li>On success, the circuit is notified and the result is returned</li>
		 *   <li>On failure, the circuit is notified and {@link CircuitBreakerExecutionException} is thrown</li>
		 * </ul>
		 * <p>
		 * Use this method when you want to handle circuit breaker exceptions explicitly
		 * rather than using automatic fallback behavior.
		 *
		 * @param <T> the type of result returned by the supplier
		 * @param supplier the operation to execute
		 * @return the result from the supplier if successful
		 * @throws CircuitBreakerOpenException if the circuit is OPEN and blocking requests
		 * @throws CircuitBreakerExecutionException if the supplier execution fails
		 */
		public <T> T execute(Supplier<T> supplier) {
			if (!allowRequest()) {
				throw new CircuitBreakerOpenException(
					"Circuit is OPEN. Blocked request after failure threshold reached ("
						+ failureThreshold + "). Retry after "
						+ currentRetryTimeoutMillis + " ms.");
			}
			try {
				T result = supplier.get();
				recordSuccess();
				return result;
			} catch (Exception e) {
				recordFailure();
				throw new CircuitBreakerExecutionException(
					"Execution failed while circuit was " + state.get(), e);
			}
		}

		/**
		 * Determines if a request is allowed based on the current circuit state.
		 * <p>
		 * If the circuit is OPEN and the retry timeout has passed, the state transitions to HALF_OPEN
		 * using atomic CAS operation, allowing a single trial request. Further requests are blocked
		 * until the trial succeeds or fails.
		 * <p>
		 * Thread Safety: Uses lock-free atomic operations for thread-safe state checking and transitions.
		 *
		 * @return true if the request is allowed, false otherwise
		 */
		private boolean allowRequest() {
			if (this.state.get() == CircuitState.OPEN) {
				if (System.currentTimeMillis() - lastFailureTime > this.currentRetryTimeoutMillis) {
					if(this.state.compareAndSet(CircuitState.OPEN, CircuitState.HALF_OPEN)) {
						this.fireOnHalfOpen();
						return true;
					} else {
						return false;
					}
				}
				return false;
			} else if(this.state.get() == CircuitState.HALF_OPEN) {
				return false;
			}
			return true;
		}

		/**
		 * Records a successful request, closing the circuit and resetting counters and retry timeout.
		 * <p>
		 * Thread Safety: Uses atomic operations to safely reset state without blocking.
		 */
		private void recordSuccess() {
			CircuitState old = this.state.getAndSet(CircuitState.CLOSED);
			this.failureCounter.set(0);
			this.lastFailureTime = 0;
			this.currentRetryTimeoutMillis = this.retryTimeOutMillis;

			if (old != CircuitState.CLOSED) {
				this.fireOnClose();
			}
		}

		/**
		 * Records a failed request, incrementing the failure counter and updating the last failure time.
		 * <p>
		 * If the circuit is HALF_OPEN and the trial fails, the retry timeout is exponentially increased (up to a max),
		 * and the circuit returns to OPEN. If the failure threshold is reached, the circuit opens.
		 * <p>
		 * Thread Safety: Uses atomic counter increments and state updates for lock-free operation.
		 */
		private void recordFailure() {
			this.failureCounter.getAndIncrement();
			this.lastFailureTime = System.currentTimeMillis();
			if (this.state.get() == CircuitState.HALF_OPEN) {
				this.currentRetryTimeoutMillis = Math.min(2 * this.currentRetryTimeoutMillis, this.maxRetryTimeoutMillis);
				CircuitState old = this.state.getAndSet(CircuitState.OPEN);

				if(old == CircuitState.OPEN){
					this.fireOnOpen();
				}
			}
			if (this.failureCounter.get() >= this.failureThreshold) {
				CircuitState old = this.state.getAndSet(CircuitState.OPEN);
				if(old == CircuitState.OPEN) {
					this.fireOnOpen();

				}
			}
		}

		/**
		 * Returns the current state of the circuit.
		 *
		 * @return the current CircuitState
		 */
		public CircuitState getState() {
			return this.state.get();
		}

		private final List<CircuitBreakerListener> listeners = new CopyOnWriteArrayList<>();

		/**
		 * Adds a listener to receive notifications about circuit state changes.
		 * <p>
		 * Listeners are called when the circuit transitions between OPEN, HALF_OPEN, and CLOSED states.
		 * Listener exceptions are caught and logged to prevent disrupting circuit breaker operation.
		 *
		 * @param listener the listener to add, must not be null
		 */
		public void addListener(CircuitBreakerListener listener) {
			this.listeners.add(listener);
		}

		/**
		 * Creates a new CircuitBreakerBuilder for flexible configuration.
		 * <p>
		 * The builder provides a fluent API for setting failure threshold, retry timeout,
		 * and maximum retry factor with validation.
		 *
		 * @return a new CircuitBreakerBuilder instance
		 */
		public static CircuitBreakerBuilder newBuilder() {
			return new CircuitBreakerBuilder();
		}


		private void fireOnOpen() {
			for (CircuitBreakerListener listener: listeners) {
				try {
					listener.onOpen();
				} catch (Exception e) {
					System.err.println("Listener error in onOpen: " + e.getMessage());
				}
			}
		}

		private void fireOnHalfOpen() {
			for (CircuitBreakerListener listener: listeners) {
				try {
					listener.onHalfOpen();
				} catch (Exception e) {
					System.err.println("Listener error in onHalfOpen: " + e.getMessage());
				}
			}
		}

		private void fireOnClose() {
			for (CircuitBreakerListener listener: listeners) {
				try {
					listener.onClose();
				} catch (Exception e) {
					System.err.println("Listener error in onClose: " + e.getMessage());
				}
			}
		}
	}
