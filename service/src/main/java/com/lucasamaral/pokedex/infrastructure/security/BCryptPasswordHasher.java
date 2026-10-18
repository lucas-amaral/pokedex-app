package com.lucasamaral.pokedex.infrastructure.security;

import com.lucasamaral.pokedex.application.user.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Output adapter: BCrypt with a random salt per password and Spring Security's default cost.
 */
@Component
class BCryptPasswordHasher implements PasswordHasher {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String hash) {
        return encoder.matches(rawPassword, hash);
    }
}
