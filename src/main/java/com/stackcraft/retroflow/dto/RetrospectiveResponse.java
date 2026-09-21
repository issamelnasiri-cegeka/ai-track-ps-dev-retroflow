package com.stackcraft.retroflow.dto;

import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.entity.RetrospectiveStatus;

import java.time.LocalDate;

/**
 * Response payload representing a retrospective.
 */
public record RetrospectiveResponse(

        Long id,
        String title,
        LocalDate date,
        RetrospectiveStatus status,
        Long teamId

) {

    /**
     * Converts a retrospective entity to a response payload.
     *
     * @param retrospective the retrospective entity
     * @return the retrospective response
     */
    public static RetrospectiveResponse from(Retrospective retrospective) {
        return new RetrospectiveResponse(
                retrospective.getId(),
                retrospective.getTitle(),
                retrospective.getDate(),
                retrospective.getStatus(),
                retrospective.getTeam() != null ? retrospective.getTeam().getId() : null
        );
    }

}
