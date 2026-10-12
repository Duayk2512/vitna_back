package com.vitna;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest @ActiveProfiles("demo")
@EnabledIfEnvironmentVariable(named="VITNA_VERIFY_DEMO",matches="true")
class DemoAccountsVerificationTest {
 @Autowired Accounts accounts; @Autowired PasswordEncoder encoder;
 @Value("${VITNA_DEMO_PASSWORD}") String password;
 @Test void allSixRolesHaveWorkingCredentials(){
  String[] names={"admin","cliente","almacen","produccion","trabajador","calidad"};
  for(String name:names){var a=accounts.findByEmail(name+".demo@vitna.pe").orElseThrow();assertTrue(a.active);assertTrue(encoder.matches(password,a.passwordHash));}
 }
}
