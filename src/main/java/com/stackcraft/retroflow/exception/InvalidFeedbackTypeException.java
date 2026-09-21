package com.stackcraft.retroflow.exception;

public class InvalidFeedbackTypeException extends InvalidInputException {

    public InvalidFeedbackTypeException(String type) {
        super("Invalid feedback type '" + type + "': expected WENT_WELL or NEEDS_IMPROVEMENT; "
                + "use addActionItem with a priority for ACTION_ITEM");
    }
}
