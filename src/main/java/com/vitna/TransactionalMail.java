package com.vitna;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import tools.jackson.databind.ObjectMapper;
@Service public class TransactionalMail {
 final JdbcTemplate jdbc;final SecretCipher cipher;final ObjectMapper json;
 private HttpClient http;
 synchronized HttpClient client(){if(http==null)http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();return http;}
 public TransactionalMail(JdbcTemplate jdbc,SecretCipher cipher,ObjectMapper json){this.jdbc=jdbc;this.cipher=cipher;this.json=json;}
 Map<String,Object> settings(){return jdbc.queryForMap("SELECT * FROM mail_settings WHERE id=1");}
 public boolean enabled(){var settings=settings();return cipher.ready()&&Boolean.TRUE.equals(settings.get("enabled"))&&settings.get("key_cipher")!=null;}
 public void enqueue(String email,String subject,String text){if(!enabled())return;jdbc.update("INSERT INTO mail_outbox(recipient,subject,text_cipher) VALUES(?,?,?)",email,subject,cipher.encrypt(text));}
 @Scheduled(fixedDelay=10000,initialDelay=10000) @Transactional public void deliver(){
  if(!enabled())return;var settings=settings();
  var pending=jdbc.queryForList("SELECT * FROM mail_outbox WHERE status='PENDING' AND next_attempt<=now() ORDER BY id LIMIT 5 FOR UPDATE SKIP LOCKED");
  for(var mail:pending){
   int attempts=((Number)mail.get("attempts")).intValue()+1;boolean ok=false;
   try{
    String payload=json.writeValueAsString(Map.of("sender",Map.of("email",settings.get("sender_email"),"name",settings.get("sender_name")),"to",List.of(Map.of("email",mail.get("recipient"))),"subject",mail.get("subject"),"textContent",cipher.decrypt((String)mail.get("text_cipher"))));
    var request=HttpRequest.newBuilder(URI.create("https://api.brevo.com/v3/smtp/email")).timeout(Duration.ofSeconds(8)).header("api-key",cipher.decrypt((String)settings.get("key_cipher"))).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(payload)).build();
    int status=client().send(request,HttpResponse.BodyHandlers.discarding()).statusCode();ok=status>=200&&status<300;
   }catch(Exception e){if(e instanceof InterruptedException)Thread.currentThread().interrupt();}
   jdbc.update("UPDATE mail_outbox SET status=?,attempts=?,next_attempt=now()+interval '2 minutes',text_cipher=CASE WHEN ? THEN '' ELSE text_cipher END WHERE id=?",ok?"SENT":attempts>=3?"FAILED":"PENDING",attempts,ok||attempts>=3,mail.get("id"));
  }
 }
}
