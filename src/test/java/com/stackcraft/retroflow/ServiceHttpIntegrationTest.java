package com.stackcraft.retroflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stackcraft.retroflow.controller.RetrospectiveController;
import com.stackcraft.retroflow.controller.TeamController;
import com.stackcraft.retroflow.dto.RetrospectiveResponse;
import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.service.RetrospectiveService;
import com.stackcraft.retroflow.service.TeamService;
import com.stackcraft.retroflow.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@SpringBootTest
class ServiceHttpIntegrationTest {

    private final MockMvc mvc;
    private final ObjectMapper objectMapper;
    private final TeamService teamService;
    private final RetrospectiveService retrospectiveService;

    @Autowired
    ServiceHttpIntegrationTest(TeamService teamService, RetrospectiveService retrospectiveService,
                               ObjectMapper objectMapper) {
        this.mvc = MockMvcBuilders.standaloneSetup(
                        new TeamController(teamService), new RetrospectiveController(retrospectiveService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        this.objectMapper = objectMapper;
        this.teamService = teamService;
        this.retrospectiveService = retrospectiveService;
    }

    @Test
    void creatingTeamReturnsCreatedTeamWithSortedMembers() throws Exception {
        // Arrange
        var request = post("/api/teams").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Platform","members":["Bob","Alice"]}
                        """);

        // Act
        MockHttpServletResponse response = mvc.perform(request).andReturn().getResponse();

        // Assert
        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED.value());
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("id").asLong()).isPositive();
        assertThat(body.get("name").asText()).isEqualTo("Platform");
        assertThat(body.get("members").size()).isEqualTo(2);
        assertThat(body.get("members").get(0).asText()).isEqualTo("Alice");
        assertThat(body.get("members").get(1).asText()).isEqualTo("Bob");
    }

    @Test
    void retrievingTeamReturnsItsMembers() throws Exception {
        // Arrange
        Team team = teamService.createTeam("Platform", List.of("Alice", "Bob"));
        var request = get("/api/teams/{id}", team.getId());

        // Act
        MockHttpServletResponse response = mvc.perform(request).andReturn().getResponse();

        // Assert
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("id").asLong()).isEqualTo(team.getId());
        assertThat(body.get("members").size()).isEqualTo(2);
    }

    @Test
    void creatingRetrospectiveReturnsOpenRetrospective() throws Exception {
        // Arrange
        Team team = teamService.createTeam("Platform", List.of("Alice"));
        var request = post("/api/teams/{id}/retrospectives", team.getId())
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Sprint\"}");

        // Act
        MockHttpServletResponse response = mvc.perform(request).andReturn().getResponse();

        // Assert
        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED.value());
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("id").asLong()).isPositive();
        assertThat(body.get("status").asText()).isEqualTo("OPEN");
        assertThat(body.get("teamId").asLong()).isEqualTo(team.getId());
        RetrospectiveResponse retrospectiveResponse = objectMapper.treeToValue(body, RetrospectiveResponse.class);
        assertThat(retrospectiveResponse.date()).isNotNull();
    }

    @Test
    void creatingSecondOpenRetrospectiveReturnsConflict() throws Exception {
        // Arrange
        Team team = teamService.createTeam("Platform", List.of("Alice"));
        retrospectiveService.createRetrospective(team.getId(), "Sprint");
        var request = post("/api/teams/{id}/retrospectives", team.getId())
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Duplicate\"}");

        // Act
        MockHttpServletResponse response = mvc.perform(request).andReturn().getResponse();

        // Assert
        assertThat(response.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("message").asText())
                .isEqualTo("Team " + team.getId() + " already has an OPEN retrospective");
    }

    @Test
    void listingRetrospectivesReturnsTeamHistory() throws Exception {
        // Arrange
        Team team = teamService.createTeam("Platform", List.of("Alice"));
        Retrospective retro = retrospectiveService.createRetrospective(team.getId(), "Sprint");
        var request = get("/api/teams/{id}/retrospectives", team.getId());

        // Act
        MockHttpServletResponse response = mvc.perform(request).andReturn().getResponse();

        // Assert
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.size()).isEqualTo(1);
        assertThat(body.get(0).get("id").asLong()).isEqualTo(retro.getId());
    }

    @Test
    void closingRetrospectiveReturnsClosedStatus() throws Exception {
        // Arrange
        Team team = teamService.createTeam("Platform", List.of("Alice"));
        Retrospective retro = retrospectiveService.createRetrospective(team.getId(), "Sprint");
        var request = put("/api/retrospectives/{id}/close", retro.getId());

        // Act
        MockHttpServletResponse response = mvc.perform(request).andReturn().getResponse();

        // Assert
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("status").asText()).isEqualTo("CLOSED");
    }

    @Test
    void closingAlreadyClosedRetrospectiveReturnsConflict() throws Exception {
        // Arrange
        Team team = teamService.createTeam("Platform", List.of("Alice"));
        Retrospective retro = retrospectiveService.createRetrospective(team.getId(), "Sprint");
        retrospectiveService.closeRetrospective(retro.getId());
        var request = put("/api/retrospectives/{id}/close", retro.getId());

        // Act
        MockHttpServletResponse response = mvc.perform(request).andReturn().getResponse();

        // Assert
        assertThat(response.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("message").asText())
                .isEqualTo("Retrospective " + retro.getId() + " is CLOSED and cannot be modified");
    }

    @Test
    void invalidTeamRequestReturnsBadRequest() throws Exception {
        // Arrange
        var request = post("/api/teams").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"members\":[]}");

        // Act
        MockHttpServletResponse response = mvc.perform(request).andReturn().getResponse();

        // Assert
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("message").asText()).isNotEmpty();
    }

    @Test
    void malformedTeamRequestReturnsBadRequest() throws Exception {
        // Arrange
        var request = post("/api/teams").contentType(MediaType.APPLICATION_JSON).content("{");

        // Act
        MockHttpServletResponse response = mvc.perform(request).andReturn().getResponse();

        // Assert
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("message").asText()).isEqualTo("Malformed or missing request body");
    }

    @Test
    void nonnumericTeamIdReturnsBadRequest() throws Exception {
        // Arrange
        var request = get("/api/teams/not-a-number");

        // Act
        MockHttpServletResponse response = mvc.perform(request).andReturn().getResponse();

        // Assert
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void retrievingMissingTeamReturnsNotFound() throws Exception {
        // Arrange
        long missingTeamId = Long.MAX_VALUE;
        var request = get("/api/teams/{id}", missingTeamId);

        // Act
        MockHttpServletResponse response = mvc.perform(request).andReturn().getResponse();

        // Assert
        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("message").asText()).isEqualTo("Team " + missingTeamId + " not found");
    }

    @Test
    void creatingRetrospectiveForMissingTeamReturnsNotFound() throws Exception {
        // Arrange
        var request = post("/api/teams/{id}/retrospectives", Long.MAX_VALUE)
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Sprint\"}");

        // Act
        MockHttpServletResponse response = mvc.perform(request).andReturn().getResponse();

        // Assert
        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void serviceInputExceptionsAreTranslatedWithoutControllerValidation() throws Exception {
        // Arrange: standalone controllers have no method-validation proxy, so the service validates the ID.
        var request = get("/api/teams/0");

        // Act
        MockHttpServletResponse response = mvc.perform(request).andReturn().getResponse();

        // Assert
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("message").asText()).isEqualTo("teamId must be positive");
    }
}
