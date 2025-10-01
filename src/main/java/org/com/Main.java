package org.com;

import org.com.circuitbreaker.CircuitBreaker;

public class Main {
	public static void main(String[] args) throws InterruptedException {
		CircuitBreaker cb = new CircuitBreaker(3, 5000);

		for(int i=0; i < 10; i++) {
			System.out.println("---- Request " + i + " ----");
			if(cb.allowRequest()) {
				boolean success = Math.random() > 0.9;

				if(success) {
					cb.recordSuccess();
					System.out.println("Service call SUCCESS");
				} else {
					cb.recordFailure();
					System.out.println("Service call FAILURE");
				}
			} else {
				System.out.println("Request blocked (Circuit is OPEN).");
			}
			Thread.sleep(500);
		}
	}
}
