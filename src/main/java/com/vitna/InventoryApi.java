package com.vitna;
import com.vitna.Domain.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static com.vitna.Domain.Role.*;
@RestController @RequestMapping("/api/inventory") @Transactional public class InventoryApi {
 final Access access;final JdbcTemplate jdbc;final Lots lots;final Products products;final Orders orders;final Lines lines;final InventoryLedger ledger;
 public InventoryApi(Access access,JdbcTemplate jdbc,Lots lots,Products products,Orders orders,Lines lines,InventoryLedger ledger){this.access=access;this.jdbc=jdbc;this.lots=lots;this.products=products;this.orders=orders;this.lines=lines;this.ledger=ledger;}
 @GetMapping("/movements") Object movements(Authentication auth){access.actor(auth,ADMIN,ALMACENERO);return jdbc.queryForList("SELECT m.*,l.code,p.name AS product_name,a.name AS actor_name FROM stock_movements m JOIN lots l ON l.id=m.lot_id JOIN products p ON p.id=l.product_id LEFT JOIN accounts a ON a.id=m.actor_id ORDER BY m.id DESC LIMIT 500");}
 @GetMapping("/adjustments") Object adjustments(Authentication auth){access.actor(auth,ADMIN,ALMACENERO);return jdbc.queryForList("SELECT s.*,l.code,p.name AS product_name,a.name AS requested_name FROM stock_adjustments s JOIN lots l ON l.id=s.lot_id JOIN products p ON p.id=l.product_id JOIN accounts a ON a.id=s.requested_by ORDER BY s.id DESC LIMIT 300");}
 record Adjustment(@NotNull Long lotId,@NotBlank String kind,@NotNull @Digits(integer=10,fraction=2) BigDecimal delta,@NotBlank @Size(max=1000) String reason){}
 @PostMapping("/adjustments") Object request(@RequestBody @Valid Adjustment input,Authentication auth){var actor=access.actor(auth,ADMIN,ALMACENERO);lots.findById(input.lotId()).orElseThrow();if(!Set.of("AJUSTE","MERMA","VENCIMIENTO").contains(input.kind())||input.delta().signum()==0||(!input.kind().equals("AJUSTE")&&input.delta().signum()>0))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Selecciona ajuste, merma o vencimiento y una cantidad valida. Mermas y vencimientos deben descontar.");jdbc.update("INSERT INTO stock_adjustments(lot_id,kind,delta,reason,requested_by) VALUES(?,?,?,?,?)",input.lotId(),input.kind(),input.delta(),input.reason().trim(),actor.id);return Map.of("message","Solicitud registrada; requiere aprobacion del administrador");}
 @PostMapping("/adjustments/{id}/review") Object review(@PathVariable long id,@RequestBody Map<String,Boolean> input,Authentication auth){
  var actor=access.actor(auth,ADMIN);var rows=jdbc.queryForList("SELECT * FROM stock_adjustments WHERE id=? FOR UPDATE",id);if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  var adjustment=rows.get(0);if(!"PENDIENTE".equals(adjustment.get("status")))throw new ResponseStatusException(HttpStatus.CONFLICT,"La solicitud ya fue revisada");
  boolean approve=Boolean.TRUE.equals(input.get("approve"));
  if(approve){
   long lotId=((Number)adjustment.get("lot_id")).longValue();jdbc.queryForList("SELECT id FROM lots WHERE id=? FOR UPDATE",lotId);Lot lot=lots.findById(lotId).orElseThrow();BigDecimal delta=(BigDecimal)adjustment.get("delta");
   if("VENCIMIENTO".equals(adjustment.get("kind"))&&!lot.expiry.isBefore(LocalDate.now()))throw new ResponseStatusException(HttpStatus.CONFLICT,"El lote todavia no esta vencido");
   BigDecimal after=lot.quantity.add(delta);if(after.compareTo(lot.reserved)<0)throw new ResponseStatusException(HttpStatus.CONFLICT,"El ajuste no puede reducir stock reservado ni dejar cantidad negativa");
   lot.quantity=after;lots.save(lot);ledger.record(lot,(String)adjustment.get("kind"),delta,BigDecimal.ZERO,(String)adjustment.get("reason"),actor.id,null);
  }
  jdbc.update("UPDATE stock_adjustments SET status=?,reviewed_by=?,reviewed_at=now() WHERE id=?",approve?"APROBADO":"RECHAZADO",actor.id,id);return Map.of("message",approve?"Ajuste aplicado y registrado en kardex":"Solicitud rechazada");
 }
 record Return(@NotNull Long orderId,@NotNull Long productId,@NotNull @Positive @Digits(integer=10,fraction=2) BigDecimal quantity,@NotBlank @Size(max=100) String code,@NotBlank @Size(max=150) String warehouse,@NotNull LocalDate expiry,@NotBlank @Size(max=1000) String reason){}
 @PostMapping("/returns") Object returns(@RequestBody @Valid Return input,Authentication auth){
  var actor=access.actor(auth,ADMIN,ALMACENERO);jdbc.queryForList("SELECT id FROM customer_orders WHERE id=? FOR UPDATE",input.orderId());Order order=orders.findById(input.orderId()).orElseThrow();
  if(!"ENTREGADO".equals(order.status))throw new ResponseStatusException(HttpStatus.CONFLICT,"Solo se reciben devoluciones de pedidos entregados");
  var line=lines.findByOrderId(order.id).stream().filter(l->l.product.id.equals(input.productId())).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Producto no incluido en el pedido"));
  BigDecimal returned=jdbc.queryForObject("SELECT coalesce(sum(quantity),0) FROM order_returns WHERE order_id=? AND product_id=?",BigDecimal.class,order.id,input.productId());
  if(returned.add(input.quantity()).compareTo(line.quantity)>0)throw new ResponseStatusException(HttpStatus.CONFLICT,"La devolucion supera la cantidad entregada pendiente de devolver");
  Product product=products.lockProduct(input.productId()).orElseThrow();String code=input.code().trim().toUpperCase(Locale.ROOT);
  if(lots.findByProductId(product.id).stream().anyMatch(l->l.code.equalsIgnoreCase(code)))throw new ResponseStatusException(HttpStatus.CONFLICT,"Codigo de lote ya utilizado");
  Lot lot=new Lot();lot.product=product;lot.quantity=input.quantity();lot.code=code;lot.warehouse=input.warehouse().trim();lot.expiry=input.expiry();lots.save(lot);
  jdbc.update("INSERT INTO order_returns(order_id,product_id,lot_id,quantity,reason,actor_id) VALUES(?,?,?,?,?,?)",order.id,product.id,lot.id,input.quantity(),input.reason().trim(),actor.id);
  ledger.record(lot,"DEVOLUCION",input.quantity(),BigDecimal.ZERO,input.reason().trim(),actor.id,order.id);return Map.of("message","Devolucion recibida en cuarentena para calidad","lotId",lot.id);
 }
}
