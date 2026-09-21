package com.stackcraft.retroflow.exception;

public class InvalidActionPriorityException extends InvalidInputException {

    public InvalidActionPriorityException(String priority) {
        super("Invalid action priority '" + priority + "': expected LOW, MEDIUM or HIGH");
    }
}
