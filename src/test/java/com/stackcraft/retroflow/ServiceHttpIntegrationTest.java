package com.stackcraft.retroflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stackcraft.retroflow.controller.RetrospectiveController;
import com.stackcraft.retroflow.controller.TeamController;
import com.stackcraft.retroflow.service.RetrospectiveService;
import com.stackcraft.retroflow.service.TeamService;
import com.stackcraft.retroflow.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ServiceHttpIntegrationTest {

    private final MockMvc mvc;
    private final ObjectMapper objectMapper;

    @Autowired
    ServiceHttpIntegrationTest(TeamService teamService, RetrospectiveService retrospectiveService,
                               ObjectMapper objectMapper) {
        this.mvc = MockMvcBuilders.standaloneSetup(
                        new TeamController(teamService), new RetrospectiveController(retrospectiveService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        this.objectMapper = objectMapper;
    }

    @Test
    void existingRoutesUseServicesAndTranslateBusinessConflicts() throws Exception {
        String teamJson = mvc.perform(post("/api/teams").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Platform","members":["Bob","Alice"]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Platform"))
                .andExpect(jsonPath("$.members[0]").value("Alice"))
                .andReturn().getResponse().getContentAsString();
        long teamId = objectMapper.readTree(teamJson).get("id").asLong();
        mvc.perform(get("/api/teams/{id}", teamId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members.length()").value(2));

        String retroJson = mvc.perform(post("/api/teams/{id}/retrospectives", teamId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Sprint\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.teamId").value(teamId))
                .andExpect(jsonPath("$.date").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        long retroId = objectMapper.readTree(retroJson).get("id").asLong();
        mvc.perform(post("/api/teams/{id}/retrospectives", teamId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Duplicate\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Team " + teamId + " already has an OPEN retrospective"));
        mvc.perform(get("/api/teams/{id}/retrospectives", teamId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(retroId));
        mvc.perform(put("/api/retrospectives/{id}/close", retroId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
        mvc.perform(put("/api/retrospectives/{id}/close", retroId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "Retrospective " + retroId + " is CLOSED and cannot be modified"));
    }

    @Test
    void existingInputValidationAndMissingResourceResponsesArePreserved() throws Exception {
        mvc.perform(post("/api/teams").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"members\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
        mvc.perform(post("/api/teams").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed or missing request body"));
        mvc.perform(get("/api/teams/not-a-number"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/teams/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Team " + Long.MAX_VALUE + " not found"));
        mvc.perform(post("/api/teams/{id}/retrospectives", Long.MAX_VALUE)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Sprint\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void serviceInputExceptionsAreTranslatedWithoutControllerValidation() throws Exception {
        // Standalone controllers have no method-validation proxy, exercising service validation.
        mvc.perform(get("/api/teams/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("teamId must be positive"));
    }
}
