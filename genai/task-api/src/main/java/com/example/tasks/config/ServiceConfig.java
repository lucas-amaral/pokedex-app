package com.example.tasks.config;

import com.example.tasks.repository.TaskRepository;
import com.example.tasks.repository.UserRepository;
import com.example.tasks.service.LogInService;
import com.example.tasks.service.LogInUseCase;
import com.example.tasks.service.PasswordHasher;
import com.example.tasks.service.TaskService;
import com.example.tasks.service.TokenIssuer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Wires the framework-free services into Spring; the service package itself has no Spring annotations. */
@Configuration(proxyBeanMethods = false)
public class ServiceConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    TaskService taskService(TaskRepository tasks, Clock clock) {
        return new TaskService(tasks, clock);
    }

    @Bean
    LogInUseCase logInUseCase(UserRepository users, PasswordHasher passwordHasher, TokenIssuer tokenIssuer) {
        return new LogInService(users, passwordHasher, tokenIssuer);
    }
}
