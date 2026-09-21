package com.stackcraft.retroflow.exception;

public class SubmitterNotTeamMemberException extends RetroflowException {

    public SubmitterNotTeamMemberException(String submittedBy, Long teamId) {
        super("Submitter '" + submittedBy + "' is not a member of team " + teamId);
    }
}
