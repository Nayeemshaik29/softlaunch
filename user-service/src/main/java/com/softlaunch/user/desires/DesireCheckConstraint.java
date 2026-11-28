package com.softlaunch.user.desires;

import com.softlaunch.user.model.Desire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Hibernate wrote a CHECK constraint listing the allowed desires when the table was first created, and
 * "ddl-auto: update" never changes it again. So new Desire values would be refused by the database.
 * On startup we rebuild that one constraint from the enum. Idempotent; values come from our own enum, not input.
 */
@Component
public class DesireCheckConstraint {

    private static final Logger log = LoggerFactory.getLogger(DesireCheckConstraint.class);
    private final JdbcTemplate jdbc;

    public DesireCheckConstraint(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void sync() {
        String allowed = Arrays.stream(Desire.values()).map(d -> "'" + d.name() + "'").collect(Collectors.joining(","));
        try {
            jdbc.execute("ALTER TABLE profile_desires DROP CONSTRAINT IF EXISTS profile_desires_desire_check");
            jdbc.execute("ALTER TABLE profile_desires ADD CONSTRAINT profile_desires_desire_check CHECK (desire IN (" + allowed + "))");
        } catch (RuntimeException e) {
            log.warn("Couldn't refresh the desire check constraint: {}", e.getClass().getSimpleName());
        }
    }
}
