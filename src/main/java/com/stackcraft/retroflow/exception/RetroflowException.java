package com.stackcraft.retroflow.exception;

/**
 * Thrown when a request violates one of RetroFlow's business rules — e.g.
 * attempting to modify a closed retrospective, creating a second OPEN
 * retrospective for a team, or uncompleting an already-completed action
 * item.
 *
 * Left as bare infrastructure by the previous vendor: the class exists so
 * the inherited test file compiles against it, but nothing yet throws it
 * with a real, specific message. Building the service layer that actually
 * enforces the business rules and throws this appropriately — plus a
 * global exception handler that maps it to a proper HTTP error response —
 * is part of Block 1 and Block 2.
 */
public class RetroflowException extends RuntimeException {

    public RetroflowException(String message) {
        super(message);
    }

}
