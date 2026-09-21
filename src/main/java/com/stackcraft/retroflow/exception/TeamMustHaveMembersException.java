package com.stackcraft.retroflow.exception;

public class TeamMustHaveMembersException extends InvalidInputException {

    public TeamMustHaveMembersException() {
        super("A team must have at least one member");
    }
}
