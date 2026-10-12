package com.vitna;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import com.vitna.Domain.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BootstrapDiagnosticTests {
 @Test void reportsMatchAndMismatchWithoutWritingAccounts() {
  Accounts accounts=mock(Accounts.class); var encoder=new BCryptPasswordEncoder();
  Account admin=new Account(); admin.role=Role.ADMIN; admin.passwordHash=encoder.encode("synthetic+&%=ñ test");
  when(accounts.findByEmailIgnoreCase("admin@vitna.pe")).thenReturn(Optional.of(admin));
  assertEquals("BOOTSTRAP_MATCH",Seed.bootstrapDiagnostic(accounts,encoder,"synthetic+&%=ñ test"));
  assertEquals("BOOTSTRAP_MISMATCH",Seed.bootstrapDiagnostic(accounts,encoder,"synthetic-other"));
  verify(accounts,never()).save(any());
 }
 @Test void separatesAccountAndEncodingProblemsWithoutRevealingSecrets() {
  Accounts accounts=mock(Accounts.class); var encoder=new BCryptPasswordEncoder();
  assertEquals("BOOTSTRAP_VARIABLE_MISSING",Seed.bootstrapDiagnostic(accounts,encoder,null));
  when(accounts.findByEmailIgnoreCase("admin@vitna.pe")).thenReturn(Optional.empty());
  assertEquals("ADMIN_NOT_FOUND",Seed.bootstrapDiagnostic(accounts,encoder,"synthetic"));
  Account admin=new Account(); admin.role=Role.ADMIN; admin.active=false;
  when(accounts.findByEmailIgnoreCase("admin@vitna.pe")).thenReturn(Optional.of(admin));
  assertEquals("ADMIN_NOT_ACTIVE",Seed.bootstrapDiagnostic(accounts,encoder,"synthetic"));
  admin.active=true; admin.passwordHash="invalid";
  assertEquals("HASH_FORMAT_INVALID",Seed.bootstrapDiagnostic(accounts,encoder,"synthetic"));
 }
}
