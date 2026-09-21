package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.exception.ResourceNotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import com.stackcraft.retroflow.repository.TeamRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service layer for team management.
 *
 * STUB — method signatures only. Business logic (persistence, invariant
 * enforcement, etc.) will be implemented separately.
 */
@Service
public class TeamService {

    private final TeamRepository teamRepository;

    public TeamService(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    /**
     * Creates a new team with the given name and members.
     *
     * @param name    the team's name
     * @param members the names of the team's members
     * @return the created team
     * @throws RetroflowException if a business rule is violated (e.g. a team
     *                            with the same name already exists)
     */
    public Team createTeam(String name, List<String> members) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Retrieves a team by its id.
     *
     * @param id the team id
     * @return the matching team
     * @throws ResourceNotFoundException if no team exists with the given id
     */
    public Team getTeamById(Long id) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

}
