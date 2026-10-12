package com.vitna;
import org.springframework.stereotype.Component;
import org.springframework.core.env.Environment;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
@Component public class SecretCipher {
 final Environment env;
 public SecretCipher(Environment env){this.env=env;}
 public boolean ready(){String key=env.getProperty("VITNA_SETTINGS_KEY");return key!=null&&key.length()>=32;}
 private SecretKeySpec key() throws Exception {if(!ready())throw new IllegalStateException("Configurar VITNA_SETTINGS_KEY de minimo 32 caracteres en Railway para proteger la API de correo");return new SecretKeySpec(MessageDigest.getInstance("SHA-256").digest(env.getProperty("VITNA_SETTINGS_KEY").getBytes(StandardCharsets.UTF_8)),"AES");}
 public String encrypt(String value){try{byte[] iv=new byte[12];new SecureRandom().nextBytes(iv);Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key(),new GCMParameterSpec(128,iv));byte[] data=cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));byte[] all=new byte[iv.length+data.length];System.arraycopy(iv,0,all,0,iv.length);System.arraycopy(data,0,all,iv.length,data.length);return Base64.getEncoder().encodeToString(all);}catch(IllegalStateException e){throw e;}catch(Exception e){throw new IllegalStateException("No se pudo proteger la configuracion de correo");}}
 public String decrypt(String value){try{byte[] data=Base64.getDecoder().decode(value);Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Arrays.copyOfRange(data,0,12)));return new String(cipher.doFinal(Arrays.copyOfRange(data,12,data.length)),StandardCharsets.UTF_8);}catch(Exception e){throw new IllegalStateException("No se pudo abrir la configuracion de correo; revisar clave de cifrado");}}
}
