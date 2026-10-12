package com.vitna;
import com.vitna.Domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.persistence.EntityManager;
import java.util.*;
import java.math.*;
import java.time.*;
import java.util.regex.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="VITNA_SETTINGS_KEY=synthetic-encryption-test-key-not-production") @AutoConfigureMockMvc @Transactional
class OperationalControlsTests {
 @Autowired MockMvc mvc; @Autowired Accounts accounts; @Autowired Products products; @Autowired Lots lots; @Autowired Orders orders; @Autowired Allocations allocations;
 @Autowired JdbcTemplate jdbc; @Autowired PasswordEncoder encoder; @Autowired SecretCipher cipher; @Autowired EntityManager em;
 @Autowired ReservationExpiry expiry; @Autowired ProductionWorkflow production; @Autowired InventoryApi inventory;
 @Autowired Lines lines;
 @Autowired Api api;
 Account actor(Role role){Account user=new Account();user.email="ops-"+UUID.randomUUID()+"@example.invalid";user.name="Synthetic operations";user.role=role;user.passwordHash=encoder.encode("synthetic-current-password");accounts.save(user);return user;}
 org.springframework.security.core.Authentication auth(Account user){return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(user.email,"unused");}
 Product product(){Product p=new Product();p.sku="OPS-"+UUID.randomUUID();p.name="Synthetic product";p.category="Test";p.presentation="Unidad";p.description="Test";p.price=BigDecimal.ONE;products.save(p);return p;}
 Lot lot(Product p){Lot l=new Lot();l.product=p;l.code="OPS-"+UUID.randomUUID();l.warehouse="Test";l.expiry=LocalDate.now().plusDays(30);l.quantity=new BigDecimal("10");l.status="APROBADO";lots.save(l);return l;}
 @Test void brevoConfigurationIsAdminOnlyAndNeverReturnsApiKey() throws Exception {
  Account admin=actor(Role.ADMIN),client=actor(Role.CLIENTE);
  mvc.perform(get("/api/admin/mail").with(user(new Security.SessionUser(client)))).andExpect(status().isForbidden());
  mvc.perform(put("/api/admin/mail").with(user(new Security.SessionUser(admin))).with(csrf()).contentType("application/json").content("{\"enabled\":false,\"senderEmail\":\"sender@example.invalid\",\"senderName\":\"Synthetic\",\"frontendUrl\":\"https://frontend.example.invalid\",\"apiKey\":\"synthetic-provider-key\"}")).andExpect(status().isOk());
  String result=mvc.perform(get("/api/admin/mail").with(user(new Security.SessionUser(admin)))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  assertFalse(result.contains("synthetic-provider-key"));assertFalse(result.contains("key_cipher"));
  String stored=jdbc.queryForObject("SELECT key_cipher FROM mail_settings WHERE id=1",String.class);assertNotEquals("synthetic-provider-key",stored);assertEquals("synthetic-provider-key",cipher.decrypt(stored));
 }
 @Test void recoveryLinkIsSingleUseAndDoesNotRevealAccountInResponse() throws Exception {
  Account account=actor(Role.CLIENTE);em.flush();jdbc.update("UPDATE mail_settings SET enabled=true,key_cipher=?,sender_email='sender@example.invalid',sender_name='Test' WHERE id=1",cipher.encrypt("synthetic-key"));
  String response=mvc.perform(post("/api/public/password/forgot").with(csrf()).contentType("application/json").content("{\"email\":\""+account.email+"\"}")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  assertFalse(response.contains(account.email));assertFalse(response.contains("token"));
  String text=cipher.decrypt(jdbc.queryForObject("SELECT text_cipher FROM mail_outbox WHERE recipient=? ORDER BY id DESC LIMIT 1",String.class,account.email));
  Matcher matcher=Pattern.compile("restablecer/([A-Za-z0-9_-]+)").matcher(text);assertTrue(matcher.find());String token=matcher.group(1);
  String body="{\"token\":\""+token+"\",\"password\":\"synthetic-new-password\"}";
  mvc.perform(post("/api/public/password/reset").with(csrf()).contentType("application/json").content(body)).andExpect(status().isOk());
  assertTrue(encoder.matches("synthetic-new-password",jdbc.queryForObject("SELECT password_hash FROM accounts WHERE id=?",String.class,account.id)));
  mvc.perform(post("/api/public/password/reset").with(csrf()).contentType("application/json").content(body)).andExpect(status().isBadRequest());
 }
 @Test void credentialRotationInvalidatesPreviouslyAuthenticatedSessions() throws Exception {
  Account account=actor(Role.CLIENTE);em.flush();Security.SessionUser prior=new Security.SessionUser(account);
  jdbc.update("UPDATE accounts SET credential_version=credential_version+1 WHERE id=?",account.id);em.clear();
  mvc.perform(get("/api/me").with(user(prior))).andExpect(status().isUnauthorized());
 }
 @Test void adjustmentsRequireApprovalAndCannotReduceReservedStock() {
  Account admin=actor(Role.ADMIN),warehouse=actor(Role.ALMACENERO);Lot lot=lot(product());lot.reserved=new BigDecimal("3");lots.save(lot);em.flush();
  inventory.request(new InventoryApi.Adjustment(lot.id,"MERMA",new BigDecimal("-2"),"Synthetic loss"),auth(warehouse));
  assertEquals(0,lot.quantity.compareTo(new BigDecimal("10")));
  long id=jdbc.queryForObject("SELECT id FROM stock_adjustments WHERE lot_id=?",Long.class,lot.id);
  inventory.review(id,Map.of("approve",true),auth(admin));assertEquals(0,lot.quantity.compareTo(new BigDecimal("8")));
  assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM stock_movements WHERE lot_id=? AND kind='MERMA'",Integer.class,lot.id));
  inventory.request(new InventoryApi.Adjustment(lot.id,"MERMA",new BigDecimal("-6"),"Cannot consume reservations"),auth(warehouse));
  long invalid=jdbc.queryForObject("SELECT max(id) FROM stock_adjustments WHERE lot_id=?",Long.class,lot.id);
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->inventory.review(invalid,Map.of("approve",true),auth(admin)));
 }
 @Test void expiredPaymentReservationsReleaseStockWithoutDispatch() {
  Lot lot=lot(product());lot.reserved=new BigDecimal("3");lots.save(lot);Order order=new Order();order.customer=actor(Role.CLIENTE);order.status="PENDIENTE_PAGO";order.reservationExpiresAt=Instant.now().minusSeconds(20);orders.save(order);
  Allocation allocation=new Allocation();allocation.order=order;allocation.lot=lot;allocation.quantity=new BigDecimal("3");allocations.save(allocation);em.flush();expiry.release();
  assertEquals("RESERVA_VENCIDA",jdbc.queryForObject("SELECT status FROM customer_orders WHERE id=?",String.class,order.id));
  assertEquals(0,jdbc.queryForObject("SELECT reserved FROM lots WHERE id=?",BigDecimal.class,lot.id).signum());
  assertEquals(0,jdbc.queryForObject("SELECT quantity FROM lots WHERE id=?",BigDecimal.class,lot.id).compareTo(new BigDecimal("10")));
 }
 @Test void productionConsumesApprovedMaterialFefoAndRetainsTraceability() {
  Product p=product();Account worker=actor(Role.TRABAJADOR);em.flush();
  long material=jdbc.queryForObject("INSERT INTO raw_materials(name,unit) VALUES('Synthetic grain','kg') RETURNING id",Long.class);
  long first=jdbc.queryForObject("INSERT INTO raw_lots(material_id,code,warehouse,quantity,expiry,status) VALUES(?,'FIRST','Test',1,current_date+2,'APROBADO') RETURNING id",Long.class,material);
  long second=jdbc.queryForObject("INSERT INTO raw_lots(material_id,code,warehouse,quantity,expiry,status) VALUES(?,'SECOND','Test',4,current_date+3,'APROBADO') RETURNING id",Long.class,material);
  long recipe=jdbc.queryForObject("INSERT INTO recipes(product_id,output_quantity) VALUES(?,1) RETURNING id",Long.class,p.id);jdbc.update("INSERT INTO recipe_items(recipe_id,material_id,quantity) VALUES(?,?,1)",recipe,material);
  Production plan=production.plan(p.id,new BigDecimal("2"),"Preparar",worker.id);
  long step=jdbc.queryForObject("SELECT id FROM production_steps WHERE production_id=?",Long.class,plan.id);
  production.advance(plan.id,step,"START",null,worker);
  assertEquals(0,jdbc.queryForObject("SELECT quantity FROM raw_lots WHERE id=?",BigDecimal.class,first).signum());
  assertEquals(0,jdbc.queryForObject("SELECT quantity FROM raw_lots WHERE id=?",BigDecimal.class,second).compareTo(new BigDecimal("3")));
  assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM raw_consumptions WHERE production_id=?",Integer.class,plan.id));
 }
 @Test void filesRejectExecutableFormatsAndOnlyAllowExplicitTypes(){assertThrows(org.springframework.web.server.ResponseStatusException.class,()->FilesApi.media("<svg onload='x'>".getBytes(),false));assertEquals("application/pdf",FilesApi.media("%PDF-test".getBytes(),true));}
 @Test void proofDownloadsAreRestrictedToOwnerAndAdministrator() throws Exception {
  Account owner=actor(Role.CLIENTE),other=actor(Role.CLIENTE),admin=actor(Role.ADMIN);Order order=new Order();order.customer=owner;order.status="PENDIENTE_PAGO";orders.save(order);em.flush();
  mvc.perform(multipart("/api/orders/"+order.id+"/proofs").file(new org.springframework.mock.web.MockMultipartFile("file","test.pdf","application/pdf","%PDF-synthetic-proof".getBytes())).with(user(new Security.SessionUser(owner))).with(csrf())).andExpect(status().isOk());
  long file=jdbc.queryForObject("SELECT id FROM stored_files WHERE order_id=?",Long.class,order.id);
  mvc.perform(get("/api/orders/"+order.id+"/proofs/"+file).with(user(new Security.SessionUser(other)))).andExpect(status().isForbidden());
  mvc.perform(get("/api/orders/"+order.id+"/proofs/"+file).with(user(new Security.SessionUser(admin)))).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));
 }
 @Test void paymentVerificationRequiresAProofOrExplicitReference() throws Exception {
  Account admin=actor(Role.ADMIN);Order order=new Order();order.customer=actor(Role.CLIENTE);order.status="PENDIENTE_PAGO";order.reservationExpiresAt=Instant.now().plusSeconds(600);orders.save(order);em.flush();
  mvc.perform(post("/api/orders/"+order.id+"/status").with(user(new Security.SessionUser(admin))).with(csrf()).contentType("application/json").content("{\"status\":\"CONFIRMADO\"}")).andExpect(status().isBadRequest());
  mvc.perform(post("/api/orders/"+order.id+"/status").with(user(new Security.SessionUser(admin))).with(csrf()).contentType("application/json").content("{\"status\":\"CONFIRMADO\",\"paymentNote\":\"Synthetic manual verification\"}")).andExpect(status().isOk());
 }
 @Test void returnsCannotExceedDeliveredQuantitiesAndRemainQuarantined(){
  Account warehouse=actor(Role.ALMACENERO);Product product=product();Order order=new Order();order.customer=actor(Role.CLIENTE);order.status="ENTREGADO";orders.save(order);OrderLine line=new OrderLine();line.order=order;line.product=product;line.quantity=new BigDecimal("3");line.unitPrice=BigDecimal.ONE;lines.save(line);em.flush();
  var request=new InventoryApi.Return(order.id,product.id,new BigDecimal("2"),"RETURN-SYNTHETIC","Test",LocalDate.now().minusDays(1),"Synthetic return");
  inventory.returns(request,auth(warehouse));
  assertEquals("CUARENTENA",jdbc.queryForObject("SELECT l.status FROM lots l JOIN order_returns r ON r.lot_id=l.id WHERE r.order_id=?",String.class,order.id));
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->inventory.returns(new InventoryApi.Return(order.id,product.id,new BigDecimal("2"),"RETURN-OVER","Test",LocalDate.now(),"Over-return"),auth(warehouse)));
 }
 @Test void nonzeroPricesCannotCreateFreeOrdersThroughFractionalRounding(){
  Account client=actor(Role.CLIENTE);Product product=product();product.price=new BigDecimal("0.01");product.published=true;products.save(product);lot(product());
  Lot available=lot(product);available.quantity=BigDecimal.ONE;lots.save(available);em.flush();
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->api.checkout(new Api.Checkout("Synthetic address",List.of(new Api.Item(product.id,new BigDecimal("0.01"),product.price))),auth(client)));
 }
}
