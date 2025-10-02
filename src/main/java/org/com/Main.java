package org.com;

import org.com.circuitbreaker.CircuitBreaker;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
	public static void main(String[] args) throws InterruptedException {
		CircuitBreaker cb = new CircuitBreaker(3, 1000);
		ExecutorService executor = Executors.newFixedThreadPool(10);

		for (int i = 0; i < 20; i++) {
			final int requestId = i;
			System.out.println("Submitting Request " + requestId);

			executor.submit(() -> {
				System.out.println("Current thread working: " + Thread.currentThread().getName());
				try {
					if (cb.allowRequest()) {
						Thread.sleep(100);

						boolean success = Math.random() > 0.8; // 20% fail, 80% success
						if (success) {
							cb.recordSuccess();
							System.out.println("Request " + requestId + " SUCCESS");
						} else {
							cb.recordFailure();
							System.out.println("Request " + requestId + " FAILURE");
						}
					} else {
						System.out.println("Request " + requestId + " BLOCKED (Circuit is OPEN).");
					}

					System.out.println("Request " + requestId + " - Status: " + cb.getStatus());

				} catch (Exception e) {
					Thread.currentThread().interrupt();
				}
			});

			Thread.sleep(50);
		}

		executor.shutdown();
		executor.awaitTermination(1, TimeUnit.MINUTES);

		System.out.println("\n=== Test Complete ===");
		System.out.println("Final Circuit status: " + cb.getStatus());
	}
}