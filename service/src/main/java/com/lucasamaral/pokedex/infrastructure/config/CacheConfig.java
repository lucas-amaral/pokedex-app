package com.lucasamaral.pokedex.infrastructure.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * In-process Caffeine cache. Cache names and limits live in application.yml (spring.cache.*).
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
