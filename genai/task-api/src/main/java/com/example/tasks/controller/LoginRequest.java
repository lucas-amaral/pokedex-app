package com.example.tasks.controller;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @Schema(example = "alice") @NotBlank @Size(max = 100) String username,
        @Schema(example = "wonderland123") @NotBlank @Size(max = 200) String password) {

    /** Keeps the password out of logs. */
    @Override
    public String toString() {
        return "LoginRequest[username=" + username + "]";
    }
}
