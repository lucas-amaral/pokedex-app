package com.lucasamaral.pokedex.web.auth;

import com.lucasamaral.pokedex.application.user.AuthenticateUserUseCase;
import com.lucasamaral.pokedex.application.user.GetCurrentUserUseCase;
import com.lucasamaral.pokedex.application.user.RegisterUserUseCase;
import com.lucasamaral.pokedex.application.user.Registration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Accounts and access tokens")
class AuthController {

    private final RegisterUserUseCase register;
    private final AuthenticateUserUseCase authenticate;
    private final GetCurrentUserUseCase currentUser;

    AuthController(RegisterUserUseCase register, AuthenticateUserUseCase authenticate, GetCurrentUserUseCase currentUser) {
        this.register = register;
        this.authenticate = authenticate;
        this.currentUser = currentUser;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an account", description = "New accounts are regular users.")
    @ApiResponse(responseCode = "201", description = "Account created")
    @ApiResponse(responseCode = "400", description = "Malformed username, email or password", content = @Content(
            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Username or email already in use", content = @Content(
            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return UserResponse.from(register.register(new Registration(request.username(), request.email(), request.password())));
    }

    @PostMapping("/login")
    @Operation(summary = "Log in", description = "Returns a Bearer token for the protected endpoints.")
    @ApiResponse(responseCode = "200", description = "Logged in")
    @ApiResponse(responseCode = "401", description = "Wrong username or password", content = @Content(
            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return LoginResponse.from(authenticate.authenticate(request.username(), request.password()));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "The logged-in user")
    @ApiResponse(responseCode = "200", description = "The user behind the token")
    @ApiResponse(responseCode = "401", description = "No valid token", content = @Content(
            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    UserResponse me(@AuthenticationPrincipal Jwt token) {
        return UserResponse.from(currentUser.currentUser(UUID.fromString(token.getSubject())));
    }
}
