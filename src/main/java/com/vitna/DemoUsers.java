package com.vitna;
import com.vitna.Domain.*;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
@Configuration @Profile("demo") public class DemoUsers {
 @Bean CommandLineRunner demoAccounts(Accounts users,PasswordEncoder encoder,@Value("${VITNA_DEMO_PASSWORD}") String password){return args->{
  if(password.length()<12)throw new IllegalArgumentException("La contraseña demo debe tener mínimo 12 caracteres");
  String[][] entries={{"ADMIN","admin.demo@vitna.pe","Administrador de prueba"},{"CLIENTE","cliente.demo@vitna.pe","Cliente de prueba"},{"ALMACENERO","almacen.demo@vitna.pe","Almacenero de prueba"},{"JEFE_PRODUCCION","produccion.demo@vitna.pe","Jefe de producción de prueba"},{"TRABAJADOR","trabajador.demo@vitna.pe","Trabajador de prueba"},{"CALIDAD","calidad.demo@vitna.pe","Responsable de calidad de prueba"}};
  for(var row:entries){if(users.findByEmail(row[1]).isPresent())continue;Account a=new Account();a.name=row[2];a.email=row[1];a.role=Domain.Role.valueOf(row[0]);a.passwordHash=encoder.encode(password);a.address="Dirección de prueba, Ayacucho";users.save(a);}
 };}
}
