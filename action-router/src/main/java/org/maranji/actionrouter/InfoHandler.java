package org.maranji.actionrouter;

import org.maranji.actioncore.Info;

/**
 * Implement this interface directly: the router resolves {@code I} from the
 * generic type, and a proxy that hides it would break dispatch.
 */
public interface InfoHandler<I extends Info> {

    void handle(I info);
}
