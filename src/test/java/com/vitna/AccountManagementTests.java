package com.vitna;
import com.vitna.Domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class AccountManagementTests {
 Accounts accounts; Api api; Account admin,target; Authentication auth;
 @BeforeEach void setup(){accounts=mock(Accounts.class);api=new Api(accounts,mock(Products.class),mock(Lots.class),mock(Orders.class),mock(Lines.class),mock(Productions.class),mock(PasswordEncoder.class));admin=new Account();admin.id=1L;admin.email="admin@test.pe";admin.role=Role.ADMIN;admin.name="Admin";target=new Account();target.id=2L;target.email="client@test.pe";target.name="Client";target.role=Role.CLIENTE;target.passwordHash="unchanged-hash";auth=new UsernamePasswordAuthenticationToken(admin.email,"unused");when(accounts.findByEmail(admin.email)).thenReturn(Optional.of(admin));when(accounts.findByEmailIgnoreCase(admin.email)).thenReturn(Optional.of(admin));when(accounts.findById(1L)).thenReturn(Optional.of(admin));when(accounts.findById(2L)).thenReturn(Optional.of(target));when(accounts.save(any(Account.class))).thenAnswer(i->i.getArgument(0));}
 @Test void editsDetailsWithoutChangingPassword(){when(accounts.findByEmail("updated@test.pe")).thenReturn(Optional.empty());api.updateUser(2L,new Api.UserUpdate("Updated","updated@test.pe",Role.CLIENTE,"123","456","Address"),auth);assertEquals("Updated",target.name);assertEquals("updated@test.pe",target.email);assertEquals("Address",target.address);assertEquals("unchanged-hash",target.passwordHash);}
 @Test void duplicateEmailIsRejected(){assertThrows(ResponseStatusException.class,()->api.updateUser(2L,new Api.UserUpdate("Client",admin.email,Role.CLIENTE,null,null,null),auth));assertEquals("client@test.pe",target.email);}
 @Test void cannotDeactivateOrDemoteOwnAccount(){assertThrows(ResponseStatusException.class,()->api.accountStatus(1L,new Api.AccountStatus(false),auth));assertThrows(ResponseStatusException.class,()->api.updateUser(1L,new Api.UserUpdate("Admin",admin.email,Role.CLIENTE,null,null,null),auth));assertTrue(admin.active);assertEquals(Role.ADMIN,admin.role);}
 @Test void disabledAccountCannotUseExistingAuthentication(){api.accountStatus(2L,new Api.AccountStatus(false),auth);when(accounts.findByEmail(target.email)).thenReturn(Optional.of(target));Authentication existing=new UsernamePasswordAuthenticationToken(target.email,"unused");assertThrows(ResponseStatusException.class,()->api.me(existing));api.accountStatus(2L,new Api.AccountStatus(true),auth);assertNotNull(api.me(existing));}
 @Test void nonAdminCannotManageAccounts(){admin.role=Role.CLIENTE;assertThrows(ResponseStatusException.class,()->api.updateUser(2L,new Api.UserUpdate("Client",target.email,Role.CLIENTE,null,null,null),auth));assertThrows(ResponseStatusException.class,()->api.accountStatus(2L,new Api.AccountStatus(false),auth));}
 @Test void inactiveAccountCannotChangeOrderStatus(){admin.active=false;OrderWorkflow workflow=mock(OrderWorkflow.class);OrderActions actions=new OrderActions(workflow,accounts);assertThrows(ResponseStatusException.class,()->actions.change(1L,java.util.Map.of("status","CONFIRMADO"),auth));verifyNoInteractions(workflow);}
}
