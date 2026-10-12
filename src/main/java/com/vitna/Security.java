package com.vitna;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;
@Configuration public class Security {
 @Bean PasswordEncoder encoder(){return new BCryptPasswordEncoder();}
 @Bean UserDetailsService users(Accounts accounts){return email -> {
  var a=accounts.findByEmailIgnoreCase(email.trim()).orElseThrow(()->new UsernameNotFoundException("Cuenta no encontrada"));
  return new User(a.email,a.passwordHash,a.active,true,true,true,List.of(new SimpleGrantedAuthority("ROLE_"+a.role)));
 };}
 @Bean @org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication(type=org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET) SecurityFilterChain chain(HttpSecurity http,Accounts accounts,PasswordEncoder encoder,org.springframework.core.env.Environment environment) throws Exception {
  return http.authorizeHttpRequests(a->a.requestMatchers("/api/public/**","/api/csrf").permitAll().anyRequest().authenticated())
   .formLogin(f->f.loginProcessingUrl("/api/login").usernameParameter("email")
    .successHandler((q,s,a)->{s.setContentType("application/json");s.getWriter().write("{\"ok\":true}");})
    .failureHandler((q,s,e)->{
     if(environment.getProperty("VITNA_AUTH_DIAGNOSTICS",Boolean.class,false))
      org.slf4j.LoggerFactory.getLogger(Security.class).info("VITNA_LOGIN_DIAGNOSTIC: {}; provider={}",loginDiagnostic(accounts,encoder,q.getParameter("email"),q.getParameter("password")),e.getClass().getSimpleName());
     s.setStatus(401);s.setContentType("application/json");s.getWriter().write("{\"message\":\"Credenciales incorrectas o cuenta inactiva\"}");}))
   .logout(l->l.logoutUrl("/api/logout").logoutSuccessHandler((q,s,a)->s.setStatus(204)))
   .exceptionHandling(e->e.authenticationEntryPoint((q,s,x)->{s.setStatus(401);s.setContentType("application/json");s.getWriter().write("{\"message\":\"Debes iniciar sesion\"}");})
    .accessDeniedHandler((q,s,x)->{s.setStatus(403);s.setContentType("application/json");s.getWriter().write("{\"message\":\"Solicitud rechazada. Recarga la pagina para renovar la sesion y el token CSRF; verifica tus permisos\"}");})).build();
 }
 static String loginDiagnostic(Accounts accounts,PasswordEncoder encoder,String email,String password) {
  if(email==null || email.isBlank()) return "EMAIL_FIELD_MISSING";
  if(password==null || password.isEmpty()) return "PASSWORD_FIELD_MISSING";
  if(!"admin@vitna.pe".equalsIgnoreCase(email.trim())) return "EMAIL_DIFFERS_FROM_BOOTSTRAP_ADMIN";
  try {
   var found=accounts.findByEmailIgnoreCase(email.trim());
   if(found.isEmpty()) return "ADMIN_NOT_FOUND";
   var account=found.get();
   if(!account.active) return "ACCOUNT_INACTIVE";
   if(account.passwordHash==null) return "HASH_MISSING";
   return encoder.matches(password,account.passwordHash) ? "PASSWORD_MATCH_PROVIDER_REJECTED" : "SUBMITTED_PASSWORD_MISMATCH";
  } catch(Exception exception) { return "ACCOUNT_CHECK_FAILED"; }
 }
}

