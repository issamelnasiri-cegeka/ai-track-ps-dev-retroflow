package com.stackcraft.retroflow.controller;

import com.stackcraft.retroflow.dto.RetrospectiveCreateRequest;
import com.stackcraft.retroflow.dto.RetrospectiveResponse;
import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.service.RetrospectiveService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints for managing retrospectives.
 * All business logic lives in {@link RetrospectiveService}.
 */
@RestController
@Validated
public class RetrospectiveController {

    private final RetrospectiveService retrospectiveService;

    public RetrospectiveController(RetrospectiveService retrospectiveService) {
        this.retrospectiveService = retrospectiveService;
    }

    @PostMapping("/api/teams/{teamId}/retrospectives")
    public ResponseEntity<RetrospectiveResponse> createRetrospective(
            @PathVariable @Positive(message = "teamId must be positive") Long teamId,
            @Valid @RequestBody RetrospectiveCreateRequest request) {
        Retrospective retrospective = retrospectiveService.createRetrospective(teamId, request.title());
        return ResponseEntity.status(HttpStatus.CREATED).body(RetrospectiveResponse.from(retrospective));
    }

    @GetMapping("/api/teams/{teamId}/retrospectives")
    public ResponseEntity<List<RetrospectiveResponse>> listRetrospectives(
            @PathVariable @Positive(message = "teamId must be positive") Long teamId) {
        List<RetrospectiveResponse> retrospectives = retrospectiveService.getRetrospectivesForTeam(teamId).stream()
                .map(RetrospectiveResponse::from)
                .toList();
        return ResponseEntity.ok(retrospectives);
    }

    @PutMapping("/api/retrospectives/{id}/close")
    public ResponseEntity<RetrospectiveResponse> closeRetrospective(
            @PathVariable @Positive(message = "id must be positive") Long id) {
        Retrospective retrospective = retrospectiveService.closeRetrospective(id);
        return ResponseEntity.ok(RetrospectiveResponse.from(retrospective));
    }

}
