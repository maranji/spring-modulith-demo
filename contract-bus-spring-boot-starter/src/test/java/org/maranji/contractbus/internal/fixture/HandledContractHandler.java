package org.maranji.contractbus.internal.fixture;

import org.maranji.contractbus.ContractHandler;

public class HandledContractHandler implements ContractHandler<HandledContract, String> {

    @Override
    public String handle(HandledContract contract) {
        return "handled";
    }
}
