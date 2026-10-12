package com.vitna;
import com.vitna.Domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
@Service public class OrderWorkflow {
 @org.springframework.beans.factory.annotation.Autowired InventoryLedger ledger;
 @org.springframework.beans.factory.annotation.Autowired TransactionalMail mail;
 @org.springframework.beans.factory.annotation.Value("${vitna.reservation-hours:24}") long reservationHours=24;
 final Orders orders; final Lines lines; final Lots lots; final Allocations allocations;
 public OrderWorkflow(Orders o,Lines li,Lots l,Allocations a){orders=o;lines=li;lots=l;allocations=a;}
 @Transactional public Order transition(long id,String next,Role role){
 if(next==null||next.isBlank())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Selecciona el estado del pedido");
 Order o=orders.findById(id).orElseThrow();String old=o.status;
 boolean admin=role==Role.ADMIN, warehouse=role==Role.ALMACENERO;
 if(next.equals("CANCELADO") && admin && List.of("PENDIENTE_REVISION","PENDIENTE_PAGO","CONFIRMADO").contains(old)){release(o,false);}
 else if(old.equals("PENDIENTE_REVISION") && next.equals("PENDIENTE_PAGO") && admin){
  for(var line:lines.findByOrderId(id)){BigDecimal remaining=line.quantity;
   var eligible=lots.findByProductId(line.product.id).stream().filter(l->l.status.equals("APROBADO")&&!l.expiry.isBefore(LocalDate.now())).sorted(Comparator.comparing((Lot l)->l.expiry).thenComparing(l->l.id)).toList();
   for(var lot:eligible){BigDecimal take=lot.quantity.subtract(lot.reserved).min(remaining);if(take.signum()<=0)continue;lot.reserved=lot.reserved.add(take);lots.save(lot);Allocation al=new Allocation();al.order=o;al.lot=lot;al.quantity=take;allocations.save(al);if(ledger!=null)ledger.record(lot,"RESERVA",BigDecimal.ZERO,take,"Reserva de pedido",null,o.id);remaining=remaining.subtract(take);if(remaining.signum()==0)break;}
   if(remaining.signum()>0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Stock insuficiente para confirmar");
  }
  o.reservationExpiresAt=java.time.Instant.now().plus(java.time.Duration.ofHours(reservationHours));
 }else if(old.equals("PENDIENTE_PAGO") && next.equals("CONFIRMADO") && admin){if(o.reservationExpiresAt!=null&&!o.reservationExpiresAt.isAfter(java.time.Instant.now()))throw new ResponseStatusException(HttpStatus.CONFLICT,"La reserva vencio; espera su liberacion y crea un pedido nuevo");}
 else if(old.equals("CONFIRMADO") && next.equals("EN_PREPARACION") && warehouse){}
 else if(old.equals("EN_PREPARACION") && next.equals("DESPACHADO") && warehouse){release(o,true);}
 else if(old.equals("DESPACHADO") && next.equals("ENTREGADO") && admin){}
 else throw new ResponseStatusException(HttpStatus.CONFLICT,"Transición no permitida para este estado o rol");
 o.status=next;if(mail!=null&&o.customer!=null&&o.customer.email!=null)mail.enqueue(o.customer.email,"Actualizacion de pedido VITNA","Tu pedido #"+o.id+" ahora esta en estado "+next+". Revisa el detalle en tu cuenta.");return orders.save(o);
 }
 void release(Order o,boolean dispatch){for(var al:allocations.findByOrderId(o.id)){Lot l=al.lot;if(dispatch && (!l.status.equals("APROBADO")||l.expiry.isBefore(LocalDate.now())))throw new ResponseStatusException(HttpStatus.CONFLICT,"Lote bloqueado o vencido");l.reserved=l.reserved.subtract(al.quantity);if(dispatch)l.quantity=l.quantity.subtract(al.quantity);lots.save(l);if(ledger!=null)ledger.record(l,dispatch?"DESPACHO":"LIBERACION",dispatch?al.quantity.negate():BigDecimal.ZERO,al.quantity.negate(),dispatch?"Salida por despacho":"Reserva liberada",null,o.id);allocations.delete(al);}}
}
