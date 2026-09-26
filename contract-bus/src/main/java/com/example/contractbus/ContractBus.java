package com.example.contractbus;

import com.example.basecontract.Contract;

/**
 * Callers depend only on this interface, so the transport (in-process today,
 * HTTP or a queue tomorrow) can change without touching them.
 */
public interface ContractBus {

    <R> R send(Contract<R> contract);
}
