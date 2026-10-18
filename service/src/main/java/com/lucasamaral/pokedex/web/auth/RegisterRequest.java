package com.lucasamaral.pokedex.web.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Detailed rules (username format, email format, password length) are checked by the use case. */
public record RegisterRequest(
        @Schema(example = "ash") @NotBlank @Size(max = 100) String username,
        @Schema(example = "ash@pallet.town") @NotBlank @Size(max = 254) String email,
        @Schema(example = "pikachu123") @NotBlank @Size(max = 200) String password) {

    @Override
    public String toString() {
        return "RegisterRequest[username=" + username + ", email=" + email + "]";
    }
}
