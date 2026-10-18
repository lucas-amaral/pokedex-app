package com.example.tasks.controller;

import com.example.tasks.service.LogInUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth")
class AuthController {

    private final LogInUseCase logIn;

    AuthController(LogInUseCase logIn) {
        this.logIn = logIn;
    }

    @PostMapping("/login")
    @Operation(summary = "Log in", description = "Returns a Bearer token for the task endpoints.")
    @ApiResponse(responseCode = "200", description = "Logged in")
    @ApiResponse(responseCode = "400", description = "Missing or malformed username or password")
    @ApiResponse(responseCode = "401", description = "Wrong username or password")
    LoginResponse logIn(@Valid @RequestBody LoginRequest request) {
        return LoginResponse.from(logIn.logIn(request.username(), request.password()));
    }
}
