package com.example.contractbus;

import com.example.basecontract.Info;

/**
 * Implement this interface directly: the bus resolves {@code I} from the
 * generic type, and a proxy that hides it would break dispatch.
 */
public interface InfoHandler<I extends Info> {

    void handle(I info);
}
