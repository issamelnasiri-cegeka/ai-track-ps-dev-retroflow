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

    /**
     * Creates an API error with the current timestamp.
     *
     * @param status  the HTTP status code
     * @param error   the HTTP status reason
     * @param message the error detail
     * @return the API error payload
     */
    public static ApiError of(int status, String error, String message) {
        return new ApiError(Instant.now(), status, error, message);
    }

}
