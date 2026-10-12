package com.vitna;

import com.vitna.Domain.*;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestAccessProvisioner {
 private final Accounts accounts;
 private final PasswordEncoder encoder;
 public TestAccessProvisioner(Accounts accounts,PasswordEncoder encoder){this.accounts=accounts;this.encoder=encoder;}
 static String email(Role role){return role==Role.ADMIN ? "admin@vitna.pe" : role.name().toLowerCase(Locale.ROOT)+".test@vitna.pe";}
 @Transactional
 public void apply(Map<Role,String> passwords){
  Set<String> unique=new HashSet<>();
  Map<Role,Account> targets=new EnumMap<>(Role.class);
  for(Role role:Role.values()){
   String password=passwords.get(role);
   if(password==null || password.length()<16 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72 || !unique.add(password))
    throw new IllegalStateException("Configurar seis contrasenas de prueba distintas, minimo 16 caracteres y maximo 72 bytes UTF-8");
   Account account=accounts.findByEmailIgnoreCase(email(role)).orElse(null);
   if(account!=null && account.role!=role) throw new IllegalStateException("Una cuenta destino de pruebas tiene otro rol; no se realizaron cambios");
   if(account==null){account=new Account();account.email=email(role);account.name="Pruebas VITNA - "+role;account.role=role;}
   targets.put(role,account);
  }
  for(Role role:Role.values()){
   Account account=targets.get(role);String password=passwords.get(role);
   boolean changed=account.id==null || !account.active;
   if(account.passwordHash==null || !encoder.matches(password,account.passwordHash)){account.passwordHash=encoder.encode(password);changed=true;}
   account.active=true;
   if(changed) accounts.save(account);
  }
 }
}
