package com.stackcraft.retroflow.exception;

public class RetrospectiveClosedException extends RetroflowException {

    public RetrospectiveClosedException(Long retrospectiveId) {
        super("Retrospective " + retrospectiveId + " is CLOSED and cannot be modified");
    }
}
