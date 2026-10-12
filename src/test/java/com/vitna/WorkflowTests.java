package com.vitna;
import com.vitna.Domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@Transactional class WorkflowTests {
 @Autowired Orders orders; @Autowired Products products; @Autowired Lots lots; @Autowired Lines lines; @Autowired OrderWorkflow workflow;
 @Test void reservesEarliestExpiryAndCancellationRestoresAvailability(){
  Product p=new Product();p.sku="TEST";p.name="Test";products.save(p);
  Lot later=new Lot();later.product=p;later.code="LATE";later.quantity=new BigDecimal("10");later.expiry=LocalDate.now().plusDays(20);later.status="APROBADO";lots.save(later);
  Lot early=new Lot();early.product=p;early.code="EARLY";early.quantity=new BigDecimal("5");early.expiry=LocalDate.now().plusDays(2);early.status="APROBADO";lots.save(early);
  Order o=new Order();orders.save(o);OrderLine line=new OrderLine();line.order=o;line.product=p;line.quantity=new BigDecimal("7");lines.save(line);
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->workflow.transition(o.id,"PENDIENTE_PAGO",Role.CLIENTE));
  workflow.transition(o.id,"PENDIENTE_PAGO",Role.ADMIN);
  assertEquals(0,early.reserved.compareTo(new BigDecimal("5")));assertEquals(0,later.reserved.compareTo(new BigDecimal("2")));
  workflow.transition(o.id,"CANCELADO",Role.ADMIN);assertEquals(0,early.reserved.signum());assertEquals(0,later.reserved.signum());
 }
}

