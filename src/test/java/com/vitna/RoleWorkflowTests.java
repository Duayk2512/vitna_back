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
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest @Transactional
class RoleWorkflowTests {
 @Autowired Api api; @Autowired ProductionApi productionApi; @Autowired ProductionWorkflow workflow;
 @Autowired PickingApi pickingApi; @Autowired Allocations allocations;
 @Autowired ProductionSteps productionSteps; @Autowired Productions productionOrders; @Autowired Accounts accounts; @Autowired Products products; @Autowired Lots lots; @Autowired Orders orders;
 Account actor(Role role){Account a=new Account();a.name=role.name();a.email=UUID.randomUUID()+"@test.pe";a.role=role;return accounts.save(a);}
 Authentication auth(Account a){return new UsernamePasswordAuthenticationToken(a.email,"unused");}
 Product product(){Product p=new Product();p.name="Test";p.sku="ROLE-"+UUID.randomUUID();p.category="Cereales";p.presentation="250 g";p.description="Prueba";p.price=BigDecimal.TEN;p.published=true;return products.save(p);}
 long plan(Account chief,Account worker){var result=(Map<?,?>)productionApi.plan(new ProductionApi.Plan(product().id,new BigDecimal("10"),"Selección → Empaque",worker.id),auth(chief));return ((Number)result.get("id")).longValue();}
 @Test void assignedWorkerExecutesStagesInOrderAndReportsYield(){
  Account chief=actor(Role.JEFE_PRODUCCION),worker=actor(Role.TRABAJADOR),other=actor(Role.TRABAJADOR);long id=plan(chief,worker);var stages=productionSteps.findByProductionIdOrderByStepPosition(id);long first=stages.get(0).id,last=stages.get(1).id;
  assertTrue(((List<?>)productionApi.list(auth(other))).isEmpty());assertEquals(1,((List<?>)productionApi.list(auth(worker))).size());
  assertThrows(ResponseStatusException.class,()->productionApi.advance(id,first,new ProductionApi.Advance("START",null),auth(other)));
  assertThrows(ResponseStatusException.class,()->productionApi.advance(id,last,new ProductionApi.Advance("START",null),auth(worker)));
  assertThrows(ResponseStatusException.class,()->productionApi.advance(id,first,new ProductionApi.Advance("COMPLETE",null),auth(worker)));
  productionApi.advance(id,first,new ProductionApi.Advance("START",null),auth(worker));
  assertThrows(ResponseStatusException.class,()->productionApi.assign(id,new ProductionApi.Assignment(other.id),auth(chief)));
  assertThrows(ResponseStatusException.class,()->productionApi.cancel(id,auth(chief)));
  productionApi.advance(id,first,new ProductionApi.Advance("COMPLETE",null),auth(worker));productionApi.advance(id,last,new ProductionApi.Advance("START",null),auth(worker));
  assertThrows(ResponseStatusException.class,()->productionApi.advance(id,last,new ProductionApi.Advance("COMPLETE",new BigDecimal("11")),auth(worker)));
  productionApi.advance(id,last,new ProductionApi.Advance("COMPLETE",new BigDecimal("9.5")),auth(worker));Production p=productionOrders.findById(id).orElseThrow();assertEquals("TERMINADA",p.status);assertEquals(0,new BigDecimal("9.5").compareTo(p.produced));assertTrue(stages.stream().allMatch(s->s.completedAt!=null&&s.status.equals("COMPLETADA")));
 }
 @Test void chiefCanAssignAndCancelOnlyPlannedOrders(){Account chief=actor(Role.JEFE_PRODUCCION),worker=actor(Role.TRABAJADOR);var result=(Map<?,?>)productionApi.plan(new ProductionApi.Plan(product().id,BigDecimal.TEN,"Empaque",null),auth(chief));long id=((Number)result.get("id")).longValue();assertThrows(ResponseStatusException.class,()->productionApi.assign(id,new ProductionApi.Assignment(chief.id),auth(chief)));productionApi.assign(id,new ProductionApi.Assignment(worker.id),auth(chief));productionApi.cancel(id,auth(chief));assertThrows(ResponseStatusException.class,()->productionApi.advance(id,productionSteps.findByProductionIdOrderByStepPosition(id).get(0).id,new ProductionApi.Advance("START",null),auth(worker)));}
 @Test void productionViewDoesNotExposeWorkerCredentials(){Account chief=actor(Role.JEFE_PRODUCCION),worker=actor(Role.TRABAJADOR);worker.passwordHash="private-hash";plan(chief,worker);String view=productionApi.list(auth(chief)).toString();assertFalse(view.contains("passwordHash"));assertFalse(view.contains("private-hash"));assertFalse(view.contains(worker.email));}
 @Test void rolePermissionsAreEnforcedInServer(){for(Role role:Role.values()){Account account=actor(role);Authentication a=auth(account);if(List.of(Role.ADMIN,Role.CLIENTE,Role.ALMACENERO).contains(role))assertNotNull(api.orders(a));else assertThrows(ResponseStatusException.class,()->api.orders(a));if(List.of(Role.ADMIN,Role.ALMACENERO,Role.JEFE_PRODUCCION,Role.CALIDAD).contains(role))assertNotNull(api.lots(a));else assertThrows(ResponseStatusException.class,()->api.lots(a));if(List.of(Role.ADMIN,Role.JEFE_PRODUCCION,Role.TRABAJADOR,Role.CALIDAD).contains(role))assertNotNull(productionApi.list(a));else assertThrows(ResponseStatusException.class,()->productionApi.list(a));if(role!=Role.JEFE_PRODUCCION){assertThrows(ResponseStatusException.class,()->productionApi.plan(new ProductionApi.Plan(product().id,BigDecimal.ONE,"Empaque",null),a));assertThrows(ResponseStatusException.class,()->productionApi.workers(a));}}}
 @Test void clientCannotCancelOthersOrReviewedOrders(){Account client=actor(Role.CLIENTE),other=actor(Role.CLIENTE);Order o=new Order();o.customer=client;o.address="Dirección";orders.save(o);assertThrows(ResponseStatusException.class,()->api.cancelOwnOrder(o.id,auth(other)));o.status="PENDIENTE_PAGO";assertThrows(ResponseStatusException.class,()->api.cancelOwnOrder(o.id,auth(client)));o.status="PENDIENTE_REVISION";api.cancelOwnOrder(o.id,auth(client));assertEquals("CANCELADO",o.status);}
 @Test void stockExcludesQuarantineExpiredAndReservedUnits(){Product p=product();for(int i=0;i<3;i++){Lot l=new Lot();l.product=p;l.quantity=BigDecimal.TEN;l.expiry=LocalDate.now().plusDays(5);l.status=i==0?"CUARENTENA":"APROBADO";if(i==1)l.expiry=LocalDate.now().minusDays(1);if(i==2)l.reserved=new BigDecimal("3");lots.save(l);}var rows=(List<?>)api.products(auth(actor(Role.CLIENTE)));Product visible=(Product)rows.stream().filter(x->((Product)x).id.equals(p.id)).findFirst().orElseThrow();assertEquals(0,new BigDecimal("7").compareTo(visible.availableStock));}
 @Test void qualityRequiresReasonAndRecordsReviewer(){Lot l=new Lot();l.product=product();l.expiry=LocalDate.now().plusDays(2);l.quantity=BigDecimal.TEN;lots.save(l);Account quality=actor(Role.CALIDAD);assertThrows(ResponseStatusException.class,()->api.quality(l.id,Map.of("status","RECHAZADO"),auth(quality)));api.quality(l.id,Map.of("status","RECHAZADO","note","Envase dañado"),auth(quality));assertEquals("Envase dañado",l.qualityNote);assertEquals(quality.name,l.reviewedBy);assertNotNull(l.reviewedAt);assertThrows(ResponseStatusException.class,()->api.quality(l.id,Map.of("status","APROBADO"),auth(actor(Role.ALMACENERO))));}
 @Test void pickingPlanIsLimitedToWarehouseAndAdmin(){Account client=actor(Role.CLIENTE),warehouse=actor(Role.ALMACENERO);Lot lot=new Lot();lot.product=product();lot.code="PICK-TEST";lot.warehouse="Almacén A";lot.expiry=LocalDate.now().plusDays(5);lot.quantity=BigDecimal.TEN;lot.status="APROBADO";lots.save(lot);Order order=new Order();order.customer=client;order.status="CONFIRMADO";orders.save(order);Allocation allocation=new Allocation();allocation.order=order;allocation.lot=lot;allocation.quantity=new BigDecimal("2");allocations.save(allocation);var plan=(List<?>)pickingApi.picking(auth(warehouse));var item=(Map<?,?>)plan.stream().filter(r->((Map<?,?>)r).get("orderId").equals(order.id)).findFirst().orElseThrow();assertEquals("PICK-TEST",item.get("lotCode"));assertEquals("Almacén A",item.get("warehouse"));assertEquals(new BigDecimal("2"),item.get("quantity"));assertThrows(ResponseStatusException.class,()->pickingApi.picking(auth(client)));warehouse.active=false;assertThrows(ResponseStatusException.class,()->pickingApi.picking(auth(warehouse)));}
 @Test void warehouseOnlySeesPaidOperationalOrders(){Account client=actor(Role.CLIENTE),warehouse=actor(Role.ALMACENERO),admin=actor(Role.ADMIN);for(String state:List.of("PENDIENTE_REVISION","PENDIENTE_PAGO","CONFIRMADO","EN_PREPARACION","DESPACHADO","ENTREGADO","CANCELADO")){Order o=new Order();o.customer=client;o.address="Dirección";o.status=state;orders.save(o);}var visible=(List<?>)api.orders(auth(warehouse));long count=visible.stream().filter(r->((Map<?,?>)((Map<?,?>)r).get("customer")).get("id").equals(client.id)).count();assertEquals(4,count);var all=(List<?>)api.orders(auth(admin));assertEquals(7,all.stream().filter(r->((Map<?,?>)((Map<?,?>)r).get("customer")).get("id").equals(client.id)).count());}
}

