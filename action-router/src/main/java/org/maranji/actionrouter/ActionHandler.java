package org.maranji.actionrouter;

import org.maranji.actioncore.Action;

/**
 * Implement this interface directly: the router resolves {@code C} from the
 * generic type, and a proxy that hides it would break dispatch.
 */
public interface ActionHandler<C extends Action<R>, R> {

    R handle(C action);
}
