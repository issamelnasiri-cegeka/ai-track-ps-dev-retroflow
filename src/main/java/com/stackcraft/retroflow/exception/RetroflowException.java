package com.stackcraft.retroflow.exception;

/**
 * Thrown when a request violates one of RetroFlow's business rules — e.g.
 * attempting to modify a closed retrospective, creating a second OPEN
 * retrospective for a team, or uncompleting an already-completed action
 * item.
 *
 * Specific business-rule exceptions extend this class and are mapped to
 * HTTP 409 Conflict by the global exception handler.
 */
public class RetroflowException extends RuntimeException {

    public RetroflowException(String message) {
        super(message);
    }

}
