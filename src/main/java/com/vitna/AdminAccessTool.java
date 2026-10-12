package com.vitna;

import java.io.Console;
import java.sql.*;
import java.util.Arrays;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Owner-operated maintenance tool. Never starts Spring or changes data in check modes. */
public final class AdminAccessTool {
 static final String EMAIL = "admin@vitna.pe";
 static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();
 public static void main(String[] args) {
  try {
   if(args.length!=1 || !java.util.Set.of("--check-bootstrap","--verify","--reset").contains(args[0]))
    throw new IllegalArgumentException("Usar --check-bootstrap, --verify o --reset");
   try(Connection db=DriverManager.getConnection(required("DB_URL"),required("DB_USER"),required("DB_PASSWORD"))) {
    db.setReadOnly(!args[0].equals("--reset"));
    try(PreparedStatement query=db.prepareStatement("SELECT id,role,active,password_hash FROM accounts WHERE lower(email)=?")) {
     query.setString(1,EMAIL);
     try(ResultSet result=query.executeQuery()) {
      if(!result.next()) throw new IllegalStateException("Cuenta admin no encontrada en la base conectada");
      long id=result.getLong("id"); String role=result.getString("role"), hash=result.getString("password_hash"); boolean active=result.getBoolean("active");
      if(result.next()) throw new IllegalStateException("Hay correos duplicados: requiere revision manual");
      if(!"ADMIN".equals(role) || !active) throw new IllegalStateException("La cuenta no es ADMIN activa");
      if(args[0].equals("--check-bootstrap")) {
       System.out.println(ENCODER.matches(required("VITNA_ADMIN_PASSWORD"),hash) ? "MATCH: variable actual coincide con la cuenta" : "MISMATCH: variable actual no coincide con la cuenta; cambiarla no actualiza la cuenta");
       return;
      }
      Console console=System.console();
      if(console==null) throw new IllegalStateException("Requiere terminal interactiva; no pasar contrasenas como argumentos");
      char[] password=console.readPassword(args[0].equals("--verify") ? "Contrasena a verificar (oculta): " : "Nueva contrasena (oculta): ");
      if(password==null) throw new IllegalStateException("Operacion cancelada");
      try {
       if(args[0].equals("--verify")) {
        System.out.println(ENCODER.matches(new String(password),hash) ? "MATCH: contrasena coincide" : "MISMATCH: contrasena no coincide");
        return;
       }
       if(password.length<10 || new String(password).getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)
        throw new IllegalArgumentException("Usar al menos 10 caracteres y no superar 72 bytes UTF-8");
       char[] repeat=console.readPassword("Repetir nueva contrasena (oculta): ");
       boolean equal=repeat!=null && Arrays.equals(password,repeat);
       if(repeat!=null) Arrays.fill(repeat,'\0');
       if(!equal) throw new IllegalArgumentException("Las contrasenas no coinciden");
       if(!"RESET admin@vitna.pe".equals(console.readLine("Para autorizar el cambio escribir RESET admin@vitna.pe: ")))
        throw new IllegalStateException("Cambio cancelado");
       try(PreparedStatement update=db.prepareStatement("UPDATE accounts SET password_hash=? WHERE id=? AND password_hash=? AND role='ADMIN' AND active=true")) {
        update.setString(1,ENCODER.encode(new String(password))); update.setLong(2,id); update.setString(3,hash);
        if(update.executeUpdate()!=1) throw new IllegalStateException("La cuenta cambio durante la operacion; no se actualizo");
       }
       System.out.println("Contrasena actualizada para la cuenta admin existente; no se modificaron otros datos");
      } finally { Arrays.fill(password,'\0'); }
     }
    }
   }
  } catch(Exception error) {
   // JDBC exceptions can contain connection information; never print them.
   System.err.println(error instanceof SQLException ? "No se pudo consultar la base con las variables del servicio" : error.getMessage());
   System.exit(1);
  }
 }
 static String required(String name) {
  String value=System.getenv(name);
  if(value==null || value.isBlank()) throw new IllegalStateException("Falta "+name);
  return value;
 }
}
