package com.stackcraft.retroflow.exception;

public class RetrospectiveReopeningNotAllowedException extends RetroflowException {

    public RetrospectiveReopeningNotAllowedException(Long retrospectiveId) {
        super("Retrospective " + retrospectiveId + " is CLOSED and cannot be reopened");
    }
}
