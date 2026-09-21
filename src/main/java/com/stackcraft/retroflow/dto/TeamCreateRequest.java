package com.stackcraft.retroflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request payload for creating a team.
 */
public record TeamCreateRequest(

        @NotBlank(message = "name must not be blank")
        @Size(max = 100, message = "name must be at most 100 characters")
        String name,

        @NotEmpty(message = "members must contain at least one name")
        List<@NotBlank(message = "member names must not be blank")
             @Size(max = 100, message = "member names must be at most 100 characters")
             String> members

) {
}
