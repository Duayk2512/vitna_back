package com.vitna;
import com.vitna.Domain.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import java.security.*;
import java.nio.charset.StandardCharsets;
@RestController @RequestMapping("/api") public class PasswordApi {
 final JdbcTemplate jdbc;final Accounts accounts;final Access access;final PasswordEncoder encoder;final TransactionalMail mail;
 public PasswordApi(JdbcTemplate jdbc,Accounts accounts,Access access,PasswordEncoder encoder,TransactionalMail mail){this.jdbc=jdbc;this.accounts=accounts;this.access=access;this.encoder=encoder;this.mail=mail;}
 record Forgot(@NotBlank @Email @Size(max=254) String email){}
 record Reset(@NotBlank @Size(max=100) String token,@NotBlank @Size(min=10,max=72) String password){}
 record Change(@NotBlank @Size(max=200) String currentPassword,@NotBlank @Size(min=10,max=72) String password){}
 static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException();}}
 static void password(String value){if(value.getBytes(StandardCharsets.UTF_8).length>72)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"La contrasena admite maximo 72 bytes UTF-8");}
 @PostMapping("/public/password/forgot") @Transactional Object forgot(@RequestBody @Valid Forgot input){
  if(!mail.enabled())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"La recuperacion por correo aun no esta configurada. Contacta con administracion.");
  var account=accounts.findByEmailIgnoreCase(input.email().trim());
  if(account.isPresent()&&account.get().active&&mail.enabled()){
   Account user=account.get();jdbc.queryForList("SELECT id FROM accounts WHERE id=? FOR UPDATE",user.id);
   Integer recent=jdbc.queryForObject("SELECT count(*) FROM password_reset_tokens WHERE account_id=? AND created_at>now()-interval '1 minute'",Integer.class,user.id);
   if(recent==0){
    byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    jdbc.update("DELETE FROM password_reset_tokens WHERE account_id=? OR expires_at<now()",user.id);
    jdbc.update("INSERT INTO password_reset_tokens(token_hash,account_id,expires_at) VALUES(?,?,now()+interval '20 minutes')",hash(token),user.id);
    String url=String.valueOf(mail.settings().get("frontend_url"));
    mail.enqueue(user.email,"Recupera tu acceso a VITNA","Recibimos una solicitud para recuperar tu acceso. Abre este enlace en los siguientes 20 minutos:\n"+url+"/#/restablecer/"+token+"\nSi no solicitaste el cambio, ignora este mensaje. El enlace solo se puede usar una vez.");
   }
  }
  return Map.of("message","Si existe una cuenta activa, recibira un enlace de recuperacion. Revisa tambien correo no deseado.");
 }
 @PostMapping("/public/password/reset") @Transactional Object reset(@RequestBody @Valid Reset input){
  password(input.password());var tokens=jdbc.queryForList("SELECT account_id FROM password_reset_tokens WHERE token_hash=? AND expires_at>now()",hash(input.token()));
  if(tokens.isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"El enlace vencio o ya fue usado. Solicita uno nuevo.");
  long id=((Number)tokens.get(0).get("account_id")).longValue();
  jdbc.queryForList("SELECT id FROM accounts WHERE id=? FOR UPDATE",id);
  if(jdbc.queryForList("SELECT token_hash FROM password_reset_tokens WHERE token_hash=? AND expires_at>now() FOR UPDATE",hash(input.token())).isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"El enlace vencio o ya fue usado. Solicita uno nuevo.");
  if(jdbc.update("UPDATE accounts SET password_hash=?,credential_version=credential_version+1 WHERE id=? AND active=true",encoder.encode(input.password()),id)!=1)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"La cuenta no esta activa");
  jdbc.update("DELETE FROM password_reset_tokens WHERE account_id=?",id);
  return Map.of("message","Contrasena actualizada. Ingresa con tu nueva clave.");
 }
 @PostMapping("/account/password") @Transactional Object change(@RequestBody @Valid Change input,Authentication auth,HttpServletRequest request){
  Account user=access.actor(auth);password(input.password());
  jdbc.queryForList("SELECT id FROM accounts WHERE id=? FOR UPDATE",user.id);
  String current=jdbc.queryForObject("SELECT password_hash FROM accounts WHERE id=?",String.class,user.id);
  if(!encoder.matches(input.currentPassword(),current))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"La contrasena actual no coincide");
  jdbc.update("UPDATE accounts SET password_hash=?,credential_version=credential_version+1 WHERE id=?",encoder.encode(input.password()),user.id);
  jdbc.update("DELETE FROM password_reset_tokens WHERE account_id=?",user.id);
  mail.enqueue(user.email,"Tu contrasena VITNA cambio","Se actualizo tu contrasena. Si no autorizaste el cambio, contacta con la administracion de VITNA.");
  var session=request.getSession(false);if(session!=null)session.invalidate();org.springframework.security.core.context.SecurityContextHolder.clearContext();
  return Map.of("message","Contrasena actualizada. Vuelve a iniciar sesion.");
 }
}
