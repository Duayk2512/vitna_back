package com.vitna;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class LoginDiagnosticTests {
 @Test void distinguishesMissingFieldsWrongEmailAndSubmittedPasswordWithoutWrites() {
  Accounts accounts=mock(Accounts.class); var encoder=new BCryptPasswordEncoder();
  assertEquals("EMAIL_FIELD_MISSING",Security.loginDiagnostic(accounts,encoder,null,"synthetic"));
  assertEquals("PASSWORD_FIELD_MISSING",Security.loginDiagnostic(accounts,encoder,"admin@vitna.pe",null));
  assertEquals("EMAIL_DIFFERS_FROM_BOOTSTRAP_ADMIN",Security.loginDiagnostic(accounts,encoder,"other@example.invalid","synthetic"));
  var admin=new Domain.Account(); admin.role=Domain.Role.ADMIN; admin.passwordHash=encoder.encode("synthetic+&%= ñ test");
  when(accounts.findByEmailIgnoreCase("admin@vitna.pe")).thenReturn(Optional.of(admin));
  assertEquals("SUBMITTED_PASSWORD_MISMATCH",Security.loginDiagnostic(accounts,encoder,"admin@vitna.pe","synthetic-other"));
  assertEquals("PASSWORD_MATCH_PROVIDER_REJECTED",Security.loginDiagnostic(accounts,encoder,"admin@vitna.pe","synthetic+&%= ñ test"));
  verify(accounts,never()).save(any());
 }
}
