package com.stackcraft.retroflow;

import com.stackcraft.retroflow.entity.ActionItem;
import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.exception.RetroflowException;
import com.stackcraft.retroflow.service.RetrospectiveService;
import com.stackcraft.retroflow.service.TeamService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * These tests were written by the previous vendor.
 * They describe the core business rules RetroFlow must enforce.
 *
 * Keep these regression cases alongside the more detailed service tests.
 *
 * Do not modify the test assertions. Do not skip or disable tests.
 * If a test feels wrong, raise it — but don't quietly remove it.
 */
@SpringBootTest
class RetroflowBusinessRulesTest {

    private final TeamService teamService;
    private final RetrospectiveService retrospectiveService;

    @Autowired
    RetroflowBusinessRulesTest(TeamService teamService, RetrospectiveService retrospectiveService) {
        this.teamService = teamService;
        this.retrospectiveService = retrospectiveService;
    }

    @Test
    void closingARetrospective_preventsAddingFeedback() {
        // Arrange
        Team team = teamService.createTeam("Team Alpha", List.of("Alice", "Bob"));
        Retrospective retro = retrospectiveService.createRetrospective(team.getId(), "Sprint 1 Retro");
        retrospectiveService.closeRetrospective(retro.getId());

        // Act
        Throwable exception = catchThrowable(() ->
                retrospectiveService.addFeedbackItem(retro.getId(), "It went well", "WENT_WELL", "Alice"));

        // Assert
        assertThat(exception).isInstanceOf(RetroflowException.class);
    }

    @Test
    void aTeam_cannotHaveTwoOpenRetrospectives() {
        // Arrange
        Team team = teamService.createTeam("Team Beta", List.of("Carol", "Dave"));
        retrospectiveService.createRetrospective(team.getId(), "Sprint 1 Retro");

        // Act
        Throwable exception = catchThrowable(() ->
                retrospectiveService.createRetrospective(team.getId(), "Sprint 2 Retro"));

        // Assert
        assertThat(exception).isInstanceOf(RetroflowException.class);
    }

    @Test
    void completedActionItem_cannotBeUncompleted() {
        // Arrange
        Team team = teamService.createTeam("Team Gamma", List.of("Eve", "Frank"));
        Retrospective retro = retrospectiveService.createRetrospective(team.getId(), "Sprint 1 Retro");
        ActionItem actionItem = retrospectiveService.addActionItem(retro.getId(), "Fix the build", "HIGH", "Eve");
        retrospectiveService.completeActionItem(actionItem.getId());

        // Act
        Throwable exception = catchThrowable(() -> retrospectiveService.uncompleteActionItem(actionItem.getId()));

        // Assert
        assertThat(exception).isInstanceOf(RetroflowException.class);
    }

}
