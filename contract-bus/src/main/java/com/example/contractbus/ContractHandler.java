package com.example.contractbus;

import com.example.basecontract.Contract;

/**
 * Implement this interface directly: the bus resolves {@code C} from the
 * generic type, and a proxy that hides it would break dispatch.
 */
public interface ContractHandler<C extends Contract<R>, R> {

    R handle(C contract);
}
