package org.maranji.actionrouter;

import org.maranji.actioncore.Action;
import org.maranji.actioncore.Info;

/**
 * Callers depend only on this interface, so the transport (in-process,
 * HTTP or a queue or ...) can change without touching them.
 */
public interface ActionRouter {

    /** Exactly one handler must be registered for the action type. */
    <R> R route(Action<R> action);

    /**
     * Zero or more handlers may be registered for the info type; a handler
     * failure never reaches the caller.
     */
    void broadcast(Info info);
}
