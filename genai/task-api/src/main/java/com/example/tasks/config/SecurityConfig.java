package com.example.tasks.config;

import com.example.tasks.entity.UserId;
import com.example.tasks.repository.UserRepository;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Only logging in, the API docs and the health check are public. Everything else, including any endpoint added
 * later, needs a Bearer token. Stateless: no session and no CSRF token.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JsonMapper json, UserRepository users) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(routes -> routes
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/health/**", "/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(token -> requireExistingAccount(token, users)))
                        .authenticationEntryPoint((request, response, failure) -> {
                            response.setHeader("WWW-Authenticate", "Bearer");
                            ProblemResponses.write(request, response, HttpStatus.UNAUTHORIZED, "Authentication required",
                                    "Log in and send the token as 'Authorization: Bearer <token>'.", json);
                        })
                        .accessDeniedHandler((request, response, denied) ->
                                ProblemResponses.write(request, response, HttpStatus.FORBIDDEN, "Not allowed",
                                        "Your account is not allowed to do this.", json)))
                .build();
    }

    @Bean
    JwtEncoder jwtEncoder(JwtProperties properties) {
        return createJwtEncoder(properties);
    }

    @Bean
    JwtDecoder jwtDecoder(JwtProperties properties) {
        return createJwtDecoder(properties);
    }

    public static JwtEncoder createJwtEncoder(JwtProperties properties) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key(properties)));
    }

    /** Checks the signature, the expiry, the issuer, and that the subject is a user id. */
    public static JwtDecoder createJwtDecoder(JwtProperties properties) {
        var decoder = NimbusJwtDecoder.withSecretKey(key(properties)).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(properties.issuer()), SecurityConfig::subjectIsAUserId));
        return decoder;
    }

    private static OAuth2TokenValidatorResult subjectIsAUserId(Jwt token) {
        try {
            UUID.fromString(token.getSubject());
            return OAuth2TokenValidatorResult.success();
        } catch (IllegalArgumentException | NullPointerException notAUserId) {
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "The token subject is not a user id", null));
        }
    }

    /**
     * A token stays valid until it expires, but its account may be deleted before that. Checking costs one
     * primary-key lookup per request and turns deleting a user into immediate revocation of their tokens.
     */
    private static JwtAuthenticationToken requireExistingAccount(Jwt token, UserRepository users) {
        if (!users.exists(new UserId(UUID.fromString(token.getSubject())))) {
            throw new InvalidBearerTokenException("The account this token belongs to no longer exists");
        }
        // The one-argument constructor leaves the token unauthenticated (every request would get 403); this one marks it
        // authenticated. Roles are not used, so the authority list is empty.
        return new JwtAuthenticationToken(token, List.of());
    }

    private static SecretKey key(JwtProperties properties) {
        return new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
