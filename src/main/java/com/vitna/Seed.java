package com.vitna;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.vitna.Domain.*;
import java.math.BigDecimal;
@Configuration public class Seed {
 @Bean CommandLineRunner initialData(Accounts users,Products products,PasswordEncoder encoder,org.springframework.core.env.Environment environment){return args->{
  boolean production=environment.matchesProfiles("prod");
  if(production && environment.matchesProfiles("demo","preview")) throw new IllegalStateException("Produccion no admite perfiles demo o preview");
  String password=System.getenv("VITNA_ADMIN_PASSWORD");
  if(production && users.count()==0 && (password==null || password.length()<10)) throw new IllegalStateException("Configurar VITNA_ADMIN_PASSWORD de al menos 10 caracteres");
  if(users.count()==0 && password!=null && password.length()>=10){Account a=new Account();a.email="admin@vitna.pe";a.name="Administración VITNA";a.role=Role.ADMIN;a.passwordHash=encoder.encode(password);users.save(a);}
  if(environment.getProperty("VITNA_AUTH_DIAGNOSTICS",Boolean.class,false)) {
   String result=bootstrapDiagnostic(users,encoder,password);
   org.slf4j.LoggerFactory.getLogger(Seed.class).info("VITNA_AUTH_DIAGNOSTIC: {}",result);
  }
  if(!production && products.count()==0){String[][] data={{"CER-001","Cereal de quinua","Cereales","Bolsa 250 g","Producto de ejemplo. Completar su ficha comercial.","12.50"},{"CHO-001","Chocolate para taza","Chocolates","Tableta 100 g","Producto de ejemplo. Validar ingredientes y presentación.","8.00"},{"AVE-001","Avena fortificada","Cereales","Bolsa 500 g","Producto de ejemplo. Validar la ficha con VITNA.","15.00"}};for(var row:data){Product p=new Product();p.sku=row[0];p.name=row[1];p.category=row[2];p.presentation=row[3];p.description=row[4];p.price=new BigDecimal(row[5]);p.published=true;products.save(p);}}
 };}
 static String bootstrapDiagnostic(Accounts users,PasswordEncoder encoder,String password) {
  if(password==null || password.isBlank()) return "BOOTSTRAP_VARIABLE_MISSING";
  try {
   var account=users.findByEmailIgnoreCase("admin@vitna.pe");
   if(account.isEmpty()) return "ADMIN_NOT_FOUND";
   Account admin=account.get();
   if(admin.role!=Role.ADMIN || !admin.active) return "ADMIN_NOT_ACTIVE";
   if(admin.passwordHash==null || !admin.passwordHash.matches("^\\$2[aby]\\$[0-9]{2}\\$[./A-Za-z0-9]{53}$")) return "HASH_FORMAT_INVALID";
   return encoder.matches(password,admin.passwordHash) ? "BOOTSTRAP_MATCH" : "BOOTSTRAP_MISMATCH";
  } catch(Exception exception) { return "ACCOUNT_CHECK_FAILED"; }
 }
}


