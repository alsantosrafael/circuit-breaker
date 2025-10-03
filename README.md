# Circuit Breaker

This project implements a simple, thread-safe Circuit Breaker pattern in Java. The circuit breaker helps protect your application from cascading failures and allows for graceful recovery from temporary issues when interacting with unreliable external services.

## Features

- **HALF_OPEN State:** After a timeout, the circuit transitions to HALF_OPEN, allowing a single trial request to test if the external service has recovered.
- **Exponential Backoff:** Retry timeouts increase exponentially after each failure in HALF_OPEN state, up to a configurable maximum.
- **Thread Safety:** Uses lock-free atomic operations via Compare-And-Swap (CAS) for high-performance thread safety without synchronization overhead.
- **Configurable Thresholds:** You can set the failure threshold, initial retry timeout, and maximum backoff factor.
- **Event Listeners:** Register listeners to receive notifications about state changes for monitoring and logging.
- **Builder Pattern:** Flexible configuration using a fluent builder API with validation.
- **Execute Methods:** Convenient methods that automatically handle circuit breaker logic for your operations.
- **Flexible Error Handling:** Choose between automatic fallback or exception-based error handling.

## Usage

### 1. Initialization

**Using Constructor:**
```java
CircuitBreaker cb = new CircuitBreaker(failureThreshold, retryTimeoutMillis, maxRetryFactor);
```

**Using Builder Pattern (Recommended):**
```java
CircuitBreaker cb = CircuitBreaker.newBuilder()
    .withFailureThreshold(5)
    .withRetryTimeoutMillis(1000)
    .withMaxRetryFactor(8)
    .build();
```

**Parameters:**
- `failureThreshold`: Number of consecutive failures before opening the circuit.
- `retryTimeoutMillis`: Initial timeout before allowing a retry.
- `maxRetryFactor`: Maximum multiplier for exponential backoff.

### 2. Adding Event Listeners

```java
cb.addListener(new CircuitBreakerListener() {
    @Override
    public void onOpen() {
        System.out.println("Circuit opened - requests will be blocked");
        // Log metrics, trigger alerts, etc.
    }

    @Override
    public void onHalfOpen() {
        System.out.println("Circuit half-open - testing recovery");
    }

    @Override
    public void onClose() {
        System.out.println("Circuit closed - normal operation resumed");
    }
});
```

### 3. Request Handling

**Option A: Using Execute Methods (Recommended)**

**With Automatic Fallback:**
```java
// Circuit breaker handles all logic automatically
String result = cb.execute(
    () -> externalService.call(),           // Main operation
    () -> "Service temporarily unavailable" // Fallback
);
```

**With Exception Handling:**
```java
try {
    String result = cb.execute(() -> externalService.call());
    // Use result
} catch (CircuitBreakerOpenException e) {
    // Circuit is open - implement fallback logic
    return fallbackResponse();
} catch (CircuitBreakerExecutionException e) {
    // Execution failed - handle the underlying exception
    Throwable cause = e.getCause();
    // Handle specific exception types
}
```

**Option B: Manual Circuit Breaker Control**

If you need fine-grained control, you can use the manual approach:
```java
if (cb.allowRequest()) {
    try {
        // Attempt the request to external service
        String result = externalService.call();
        cb.recordSuccess();
        return result;
    } catch (Exception e) {
        cb.recordFailure();
        throw e;
    }
} else {
    // Circuit is OPEN, use fallback or throw exception
    return fallbackResponse();
}
```

### 4. Status Monitoring

Get the current circuit state:
```java
CircuitState state = cb.getState();
// Can be: CLOSED, OPEN, or HALF_OPEN
```

## Example

See `Main.java` for a multi-threaded usage example.

## Thread Safety

This implementation uses lock-free atomic operations for all state management:

- **AtomicReference**: Circuit status transitions use `compareAndSet()` for lock-free state changes
- **AtomicInteger**: Failure counter uses atomic increment/reset operations
- **Volatile fields**: Timeout values use volatile for visibility guarantees
- **No synchronization blocks**: Eliminates thread contention and blocking for high-performance concurrent access

The Compare-And-Swap (CAS) approach provides better performance than traditional synchronized methods, especially under high concurrency.

## Circuit States

- **CLOSED:** Requests are allowed. Failure counter resets on success.
- **OPEN:** Requests are blocked. After the retry timeout, transitions to HALF_OPEN.
- **HALF_OPEN:** Allows a single trial request. On success, closes the circuit. On failure, increases the retry timeout and reopens the circuit.

## Advanced Features

### Event Listeners

The circuit breaker supports event listeners that are notified when state transitions occur. This is useful for:

- **Monitoring and Metrics:** Track circuit breaker behavior and performance
- **Logging:** Record state changes for debugging and analysis
- **Alerting:** Trigger notifications when circuits open or close
- **Fallback Logic:** Implement custom behaviors based on state changes

Listeners are stored in a thread-safe `CopyOnWriteArrayList` and exceptions in listener code are caught and logged to prevent disrupting circuit breaker operation.

### Builder Pattern

The builder pattern provides several advantages over constructor-based configuration:

- **Validation:** Input parameters are validated with clear error messages
- **Defaults:** Sensible default values (threshold: 5, timeout: 1000ms, factor: 8)
- **Flexibility:** Easy to add new configuration options without breaking existing code
- **Readability:** Method names clearly indicate what each parameter controls

### Execute Methods

The circuit breaker provides two convenient execute methods that handle all circuit breaker logic automatically:

1. **`execute(supplier, fallback)`**: Never throws exceptions, always returns a result
   - Use when you want guaranteed response with automatic fallback
   - Perfect for scenarios where you always need a valid result
   - Fallback is called when circuit is open OR when execution fails

2. **`execute(supplier)`**: Throws specific exceptions for different failure modes
   - Use when you need explicit error handling
   - Throws `CircuitBreakerOpenException` when circuit is open
   - Throws `CircuitBreakerExecutionException` when execution fails
   - Allows fine-grained exception handling

Both methods automatically handle:
- Circuit state checking
- Success/failure recording
- State transitions and listener notifications

### Performance Characteristics

The lock-free implementation using atomic operations provides:

- **High Throughput:** No blocking or contention under concurrent access
- **Low Latency:** CAS operations are faster than synchronized blocks
- **Scalability:** Performance doesn't degrade with increased thread count
- **Memory Efficiency:** Minimal memory overhead from atomic fields
