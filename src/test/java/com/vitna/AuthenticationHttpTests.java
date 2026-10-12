package com.vitna;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class AuthenticationHttpTests {
 @Autowired MockMvc mvc;
 @Test void invalidCsrfIsForbiddenJson() throws Exception {
  mvc.perform(post("/api/login").with(csrf().useInvalidToken()).param("email","probe@example.invalid").param("password","synthetic-test-only"))
   .andExpect(status().isForbidden()).andExpect(content().contentTypeCompatibleWith("application/json"));
 }
 @Test void unknownCredentialsWithValidCsrfAreUnauthorizedJson() throws Exception {
  mvc.perform(post("/api/login").with(csrf()).param("email","probe@example.invalid").param("password","synthetic-test-only"))
   .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("Credenciales incorrectas o cuenta inactiva"));
 }
 @Test void anonymousProfileIsUnauthorizedJson() throws Exception {
  mvc.perform(get("/api/me")).andExpect(status().isUnauthorized()).andExpect(content().contentTypeCompatibleWith("application/json"));
 }
}
