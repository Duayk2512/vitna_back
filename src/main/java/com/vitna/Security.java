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
 @Bean @org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication(type=org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET) SecurityFilterChain chain(HttpSecurity http) throws Exception {
  return http.authorizeHttpRequests(a->a.requestMatchers("/api/public/**","/api/csrf").permitAll().anyRequest().authenticated())
   .formLogin(f->f.loginProcessingUrl("/api/login").usernameParameter("email")
    .successHandler((q,s,a)->{s.setContentType("application/json");s.getWriter().write("{\"ok\":true}");})
    .failureHandler((q,s,e)->s.sendError(401,"Credenciales incorrectas")))
   .logout(l->l.logoutUrl("/api/logout").logoutSuccessHandler((q,s,a)->s.setStatus(204)))
   .exceptionHandling(e->e.authenticationEntryPoint((q,s,x)->s.sendError(401))).build();
 }
}

