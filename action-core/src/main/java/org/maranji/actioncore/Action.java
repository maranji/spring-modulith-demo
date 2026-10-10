package org.maranji.actioncore;

/**
 * Lives in its own module so domain actions don't depend on the router.
 *
 * @param <R> the response type; use {@code Void} for commands
 */
public interface Action<R> {
}
