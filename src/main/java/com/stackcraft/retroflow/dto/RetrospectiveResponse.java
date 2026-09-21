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
