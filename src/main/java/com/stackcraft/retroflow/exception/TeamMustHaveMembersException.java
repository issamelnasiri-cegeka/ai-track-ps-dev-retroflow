package com.stackcraft.retroflow.exception;

public class TeamMustHaveMembersException extends RetroflowException {

    public TeamMustHaveMembersException() {
        super("A team must have at least one member");
    }
}
