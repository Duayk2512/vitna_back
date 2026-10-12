package com.vitna;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.net.URI;
import java.util.*;
import static com.vitna.Domain.Role.*;
@RestController @RequestMapping("/api/admin") public class SettingsApi {
 final Access access;final JdbcTemplate jdbc;final SecretCipher cipher;final TransactionalMail mail;
 public SettingsApi(Access access,JdbcTemplate jdbc,SecretCipher cipher,TransactionalMail mail){this.access=access;this.jdbc=jdbc;this.cipher=cipher;this.mail=mail;}
 @GetMapping("/audit") Object audit(Authentication auth){access.actor(auth,ADMIN);return jdbc.queryForList("SELECT * FROM audit_events ORDER BY id DESC LIMIT 500");}
 @GetMapping("/mail") Object settings(Authentication auth){access.actor(auth,ADMIN);var settings=mail.settings();var result=new LinkedHashMap<String,Object>();for(String key:List.of("enabled","sender_email","sender_name","frontend_url"))result.put(key,settings.get(key));result.put("keyConfigured",settings.get("key_cipher")!=null);result.put("encryptionReady",cipher.ready());result.put("recent",jdbc.queryForList("SELECT id,subject,status,attempts,created_at FROM mail_outbox ORDER BY id DESC LIMIT 20"));return result;}
 record Config(boolean enabled,@Email @NotBlank @Size(max=254) String senderEmail,@NotBlank @Size(max=150) String senderName,@NotBlank @Size(max=300) String frontendUrl,@Size(max=500) String apiKey){}
 @PutMapping("/mail") @Transactional Object configure(@RequestBody @Valid Config config,Authentication auth){
  access.actor(auth,ADMIN);URI uri;try{uri=URI.create(config.frontendUrl());}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"URL publica invalida");}
  if(!"https".equals(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null||!(uri.getPath().isEmpty()||uri.getPath().equals("/")))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Configura solo el origen HTTPS publico del frontend");
  String key=(String)mail.settings().get("key_cipher");
  if(config.apiKey()!=null&&!config.apiKey().isBlank()){if(!cipher.ready())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Configura VITNA_SETTINGS_KEY en Railway antes de guardar la API de Brevo");key=cipher.encrypt(config.apiKey().trim());}
  if(config.enabled()&&(key==null||!cipher.ready()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Configura la clave de cifrado Railway y la API de Brevo antes de activar");
  jdbc.update("UPDATE mail_settings SET enabled=?,key_cipher=?,sender_email=?,sender_name=?,frontend_url=?,updated_at=now() WHERE id=1",config.enabled(),key,config.senderEmail().trim(),config.senderName().trim(),uri.getScheme()+"://"+uri.getAuthority());return Map.of("message","Configuracion de correo guardada");
 }
 @PostMapping("/mail/test") @Transactional Object test(Authentication auth){var actor=access.actor(auth,ADMIN);if(!mail.enabled())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Activa y configura Brevo antes de enviar la prueba");mail.enqueue(actor.email,"Prueba de correo VITNA","La configuracion de correo transaccional de VITNA esta lista. Este envio fue solicitado desde administracion.");return Map.of("message","Correo de prueba en cola. Revisa su estado y tu bandeja.");}
}
