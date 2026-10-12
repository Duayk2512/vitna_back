package com.vitna;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.flywaydb.core.Flyway;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest @Transactional
class OneTimeAccessMigrationTests {
 @Autowired JdbcTemplate jdbc;
 @Autowired Flyway flyway;
 @Autowired PasswordEncoder encoder;
 @Test void appliedMigrationDoesNotResetAChangedPasswordOnAnotherStartup() {
  Long id=jdbc.queryForObject("SELECT id FROM accounts WHERE email='admin@vitna.pe'",Long.class);
  String name=jdbc.queryForObject("SELECT name FROM accounts WHERE id=?",String.class,id);
  int count=jdbc.queryForObject("SELECT count(*) FROM accounts",Integer.class);
  String changed=encoder.encode("synthetic-rotation-after-migration");
  jdbc.update("UPDATE accounts SET password_hash=? WHERE id=?",changed,id);
  var result=flyway.migrate();
  assertEquals(0,result.migrationsExecuted);
  assertEquals(changed,jdbc.queryForObject("SELECT password_hash FROM accounts WHERE id=?",String.class,id));
  assertEquals(name,jdbc.queryForObject("SELECT name FROM accounts WHERE id=?",String.class,id));
  assertEquals(count,jdbc.queryForObject("SELECT count(*) FROM accounts",Integer.class));
  assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE version='5' AND success=true",Integer.class));
 }
}
