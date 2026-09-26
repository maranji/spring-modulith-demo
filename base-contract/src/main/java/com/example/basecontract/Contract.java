package com.example.basecontract;

/**
 * Lives in its own module so domain contracts don't depend on the bus.
 *
 * @param <R> the response type; use {@code Void} for commands
 */
public interface Contract<R> {
}
