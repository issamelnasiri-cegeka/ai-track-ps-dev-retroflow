package com.stackcraft.retroflow.exception;

public class ActionItemUncompletionNotAllowedException extends RetroflowException {

    public ActionItemUncompletionNotAllowedException(Long actionItemId) {
        super("Action item " + actionItemId + " is completed and cannot be uncompleted");
    }
}
