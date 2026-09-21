package com.stackcraft.retroflow.exception;

public class OpenRetrospectiveExistsException extends RetroflowException {

    public OpenRetrospectiveExistsException(Long teamId) {
        super("Team " + teamId + " already has an OPEN retrospective");
    }
}
