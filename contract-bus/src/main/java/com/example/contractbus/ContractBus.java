package com.example.contractbus;

import com.example.basecontract.Contract;
import com.example.basecontract.Info;

/**
 * Callers depend only on this interface, so the transport (in-process today,
 * HTTP or a queue tomorrow) can change without touching them.
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
