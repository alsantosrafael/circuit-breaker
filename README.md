# Circuit Breaker

This project implements a simple, thread-safe Circuit Breaker pattern in Java. The circuit breaker helps protect your application from cascading failures and allows for graceful recovery from temporary issues when interacting with unreliable external services.

## Features

- **HALF_OPEN State:** After a timeout, the circuit transitions to HALF_OPEN, allowing a single trial request to test if the external service has recovered.
- **Exponential Backoff:** Retry timeouts increase exponentially after each failure in HALF_OPEN state, up to a configurable maximum.
- **Thread Safety:** All state-changing and query methods are synchronized, making the circuit breaker safe for use in multi-threaded environments.
- **Configurable Thresholds:** You can set the failure threshold, initial retry timeout, and maximum backoff factor.

## Usage

1. **Initialization:**
   ```java
   CircuitBreaker cb = new CircuitBreaker(failureThreshold, retryTimeoutMillis, maxRetryFactor);
   ```
   - `failureThreshold`: Number of consecutive failures before opening the circuit.
   - `retryTimeoutMillis`: Initial timeout before allowing a retry.
   - `maxRetryFactor`: Maximum multiplier for exponential backoff.

2. **Request Handling:**
   - Before making a request, check if it's allowed:
     ```java
     if (cb.allowRequest()) {
         // Attempt the request
         // On success:
         cb.recordSuccess();
         // On failure:
         cb.recordFailure();
     } else {
         // Circuit is OPEN, block or fallback
     }
     ```

3. **Status Monitoring:**
   - Get the current status:
     ```java
     CircuitStatus status = cb.getStatus();
     ```

## Example

See `Main.java` for a multi-threaded usage example.

## Thread Safety

All public methods that modify or query the circuit state are synchronized, ensuring safe concurrent access.

## Circuit States

- **CLOSED:** Requests are allowed. Failure counter resets on success.
- **OPEN:** Requests are blocked. After the retry timeout, transitions to HALF_OPEN.
- **HALF_OPEN:** Allows a single trial request. On success, closes the circuit. On failure, increases the retry timeout and reopens the circuit.
