package com.stackcraft.retroflow.dto;

import com.stackcraft.retroflow.entity.Team;

import java.util.Set;
import java.util.TreeSet;

/**
 * Response payload representing a team.
 */
public record TeamResponse(

        Long id,
        String name,
        Set<String> members

) {

    public static TeamResponse from(Team team) {
        return new TeamResponse(
                team.getId(),
                team.getName(),
                new TreeSet<>(team.getMembers())
        );
    }

}
