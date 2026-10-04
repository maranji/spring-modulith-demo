package org.maranji.contractbus;

import org.maranji.basecontract.Contract;
import org.maranji.basecontract.Info;

/**
 * Callers depend only on this interface, so the transport (in-process,
 * HTTP or a queue or ...) can change without touching them.
 */
public interface ContractBus {

    /** Exactly one handler must be registered for the contract type. */
    <R> R send(Contract<R> contract);

    /**
     * Zero or more handlers may be registered for the info type; a handler
     * failure never reaches the caller.
     */
    void broadcast(Info info);
}
