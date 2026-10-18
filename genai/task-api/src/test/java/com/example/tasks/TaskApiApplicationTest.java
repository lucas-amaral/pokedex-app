package com.example.tasks;

import com.example.tasks.support.PostgresTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(PostgresTestConfiguration.class)
class TaskApiApplicationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void startsWithTheSchemaAndTheDemoUsers() {
        var users = jdbc.queryForList("SELECT username, password_hash FROM users ORDER BY username");

        assertThat(users).extracting(row -> row.get("username")).containsExactly("alice", "bob");
        assertThat(users).allSatisfy(row -> assertThat((String) row.get("password_hash")).startsWith("$2"));
    }
}
