package com.stackcraft.retroflow.dto;

import java.time.Instant;

/**
 * Uniform error body returned by the API for 4xx/5xx responses.
 */
public record ApiError(

        Instant timestamp,
        int status,
        String error,
        String message

) {

    public static ApiError of(int status, String error, String message) {
        return new ApiError(Instant.now(), status, error, message);
    }

}
