package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.exception.ResourceNotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import com.stackcraft.retroflow.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

/**
 * Service layer for team management.
 *
 * Enforces team business rules and coordinates team persistence.
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
    @Transactional
    public Team createTeam(String name, List<String> members) {
        boolean nameExists = teamRepository.findAll().stream()
                .anyMatch(team -> team.getName().equalsIgnoreCase(name));
        if (nameExists) {
            throw new RetroflowException("[Team] [" + name + "]: already exists");
        }

        Team team = new Team();
        team.setName(name);
        team.setMembers(new HashSet<>(members));
        return teamRepository.save(team);
    }

    /**
     * Retrieves a team by its id.
     *
     * @param id the team id
     * @return the matching team
     * @throws ResourceNotFoundException if no team exists with the given id
     */
    public Team getTeamById(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("[Team] [" + id + "]: not found"));
    }

}
