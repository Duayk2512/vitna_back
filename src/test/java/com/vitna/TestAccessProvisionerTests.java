package com.vitna;
import com.vitna.Domain.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class TestAccessProvisionerTests {
 Map<Role,String> passwords(){var values=new EnumMap<Role,String>(Role.class);for(Role role:Role.values())values.put(role,"synthetic-distinct-"+role.name());return values;}
 @Test void configuresExactlySixTargetsAndDoesNotRewriteMatchingPasswords(){
  Accounts accounts=mock(Accounts.class);var encoder=new BCryptPasswordEncoder();var saved=new HashMap<String,Account>();
  when(accounts.findByEmailIgnoreCase(anyString())).thenAnswer(i->Optional.ofNullable(saved.get(i.getArgument(0))));
  when(accounts.save(any())).thenAnswer(i->{Account a=i.getArgument(0);a.id=(long)saved.size()+1;saved.put(a.email,a);return a;});
  var service=new TestAccessProvisioner(accounts,encoder);var credentials=passwords();service.apply(credentials);
  assertEquals(6,saved.size());for(Role role:Role.values()){var account=saved.get(TestAccessProvisioner.email(role));assertEquals(role,account.role);assertTrue(account.active);assertTrue(encoder.matches(credentials.get(role),account.passwordHash));}
  service.apply(credentials);verify(accounts,times(6)).save(any());
 }
 @Test void missingPasswordOrRoleConflictPreventsAllWrites(){
  Accounts accounts=mock(Accounts.class);var service=new TestAccessProvisioner(accounts,new BCryptPasswordEncoder());
  var incomplete=passwords();incomplete.remove(Role.CALIDAD);assertThrows(IllegalStateException.class,()->service.apply(incomplete));
  Account existing=new Account();existing.role=Role.CLIENTE;
  when(accounts.findByEmailIgnoreCase("admin@vitna.pe")).thenReturn(Optional.of(existing));
  assertThrows(IllegalStateException.class,()->service.apply(passwords()));verify(accounts,never()).save(any());
 }
}
