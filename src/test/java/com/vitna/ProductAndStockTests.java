package com.vitna;

import com.vitna.Domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest @Transactional
class ProductAndStockTests {
 @Autowired Api api; @Autowired Accounts accounts; @Autowired Products products; @Autowired Lots lots;
 Authentication actor(Role role){Account a=new Account();a.name="Test";a.email=role+"-"+java.util.UUID.randomUUID()+"@test.pe";a.role=role;accounts.save(a);return new UsernamePasswordAuthenticationToken(a.email,"unused");}
 Api.ProductRequest request(Long id,String sku,String barcode){return new Api.ProductRequest(id,sku,"Cereal","Cereales","250 g","Descripción",new BigDecimal("10.50"),true,barcode);}
 @Test void generatesPreservesAndRejectsDuplicateBarcodes(){
  Authentication admin=actor(Role.ADMIN);Product p=api.saveProduct(request(null,"  BAR-TEST  ",null),admin);products.flush();
  assertEquals("BAR-TEST",p.sku);assertTrue(p.barcode.matches("VT[A-F0-9]{32}"));String original=p.barcode;
  api.saveProduct(request(p.id,"BAR-TEST",""),admin);assertEquals(original,p.barcode);
  assertThrows(ResponseStatusException.class,()->api.saveProduct(request(null,"OTHER",original),admin));
  assertThrows(ResponseStatusException.class,()->api.saveProduct(request(null,"BAR-TEST","123456789"),admin));
  assertThrows(ResponseStatusException.class,()->api.saveProduct(request(null,"BAD","á código"),admin));
 }
 @Test void existingBarcodeAndRoleProtection(){Authentication admin=actor(Role.ADMIN);Product p=api.saveProduct(request(null,"EXTERNAL","7751234567890"),admin);assertEquals("7751234567890",p.barcode);Authentication client=actor(Role.CLIENTE);assertThrows(ResponseStatusException.class,()->api.generateBarcode(client));assertThrows(ResponseStatusException.class,()->api.saveProduct(request(null,"DENIED",null),client));}
 @Test void repeatedCheckoutItemsCannotExceedStock(){Product p=api.saveProduct(request(null,"STOCK",null),actor(Role.ADMIN));Lot l=new Lot();l.product=p;l.code="TEST";l.expiry=LocalDate.now().plusDays(10);l.quantity=new BigDecimal("5");l.status="APROBADO";lots.save(l);Authentication client=actor(Role.CLIENTE);var items=List.of(new Api.Item(p.id,new BigDecimal("3")),new Api.Item(p.id,new BigDecimal("3")));assertThrows(ResponseStatusException.class,()->api.checkout(new Api.Checkout("Dirección",items),client));}
 @Test void qualityCannotApproveExpiredOrBlockReservedLot(){Product p=api.saveProduct(request(null,"QUALITY",null),actor(Role.ADMIN));Lot l=new Lot();l.product=p;l.expiry=LocalDate.now().minusDays(1);l.quantity=BigDecimal.TEN;lots.save(l);Authentication quality=actor(Role.CALIDAD);assertThrows(ResponseStatusException.class,()->api.quality(l.id,java.util.Map.of("status","APROBADO"),quality));l.expiry=LocalDate.now().plusDays(1);l.status="APROBADO";l.reserved=BigDecimal.ONE;assertThrows(ResponseStatusException.class,()->api.quality(l.id,java.util.Map.of("status","RECHAZADO"),quality));assertEquals("APROBADO",l.status);}
}
