package com.example.tasks;

import com.example.tasks.support.PostgresTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/** Outside a demo, Flyway reads only the schema migrations, so no account with a published password exists. */
@SpringBootTest(properties = "spring.flyway.locations=classpath:db/migration")
@Import(PostgresTestConfiguration.class)
class DemoUsersSwitchedOffTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void createsNoUsers() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users", Long.class)).isZero();
    }
}
