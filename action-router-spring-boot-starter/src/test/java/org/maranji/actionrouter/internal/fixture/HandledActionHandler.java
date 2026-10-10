package org.maranji.actionrouter.internal.fixture;

import org.maranji.actionrouter.ActionHandler;

public class HandledActionHandler implements ActionHandler<HandledAction, String> {

    @Override
    public String handle(HandledAction action) {
        return "handled";
    }
}
