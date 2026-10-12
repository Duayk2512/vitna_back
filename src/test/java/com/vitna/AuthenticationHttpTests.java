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
 @Autowired Accounts accounts;
 @Autowired org.springframework.security.crypto.password.PasswordEncoder encoder;
 @Test void realProviderAuthenticatesSpecialCharactersAndPersistsSession() throws Exception {
  var account=new Domain.Account(); account.email="auth-"+java.util.UUID.randomUUID()+"@example.invalid";
  account.name="Synthetic login test"; account.role=Domain.Role.ADMIN;
  String password="synthetic+&%= ñ test";
  account.passwordHash=encoder.encode(password); accounts.save(account);
  try {
   var result=mvc.perform(post("/api/login").with(csrf()).param("email",account.email.toUpperCase(java.util.Locale.ROOT)).param("password",password))
    .andExpect(status().isOk()).andReturn();
   var session=(org.springframework.mock.web.MockHttpSession)result.getRequest().getSession(false);
   mvc.perform(get("/api/me").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.email").value(account.email));
  } finally { accounts.deleteById(account.id); }
 }
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
