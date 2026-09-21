package com.stackcraft.retroflow.controller;

import com.stackcraft.retroflow.dto.TeamCreateRequest;
import com.stackcraft.retroflow.dto.TeamResponse;
import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.service.TeamService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for managing teams.
 * All business logic lives in {@link TeamService}.
 */
@RestController
@RequestMapping("/api/teams")
@Validated
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @PostMapping
    public ResponseEntity<TeamResponse> createTeam(@Valid @RequestBody TeamCreateRequest request) {
        Team team = teamService.createTeam(request.name(), request.members());
        return ResponseEntity.status(HttpStatus.CREATED).body(TeamResponse.from(team));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TeamResponse> getTeam(@PathVariable @Positive(message = "id must be positive") Long id) {
        Team team = teamService.getTeamById(id);
        return ResponseEntity.ok(TeamResponse.from(team));
    }

}
