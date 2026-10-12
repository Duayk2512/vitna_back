package com.vitna;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
@Service public class ReservationExpiry {
 final JdbcTemplate jdbc;
 public ReservationExpiry(JdbcTemplate jdbc){this.jdbc=jdbc;}
 @Scheduled(fixedDelay=60000,initialDelay=60000) @Transactional public void release(){
  var orders=jdbc.queryForList("SELECT id FROM customer_orders WHERE status='PENDIENTE_PAGO' AND reservation_expires_at<=now() ORDER BY id FOR UPDATE SKIP LOCKED");
  for(var order:orders){
   var reserved=jdbc.queryForList("SELECT a.lot_id,sum(a.quantity) AS quantity FROM allocations a WHERE a.order_id=? GROUP BY a.lot_id ORDER BY a.lot_id",order.get("id"));
   for(var item:reserved){jdbc.queryForList("SELECT id FROM lots WHERE id=? FOR UPDATE",item.get("lot_id"));int changed=jdbc.update("UPDATE lots SET reserved=reserved-?,version=version+1 WHERE id=? AND reserved>=?",item.get("quantity"),item.get("lot_id"),item.get("quantity"));if(changed!=1)throw new IllegalStateException("Reserva inconsistente; no se liberaron pedidos");jdbc.update("INSERT INTO stock_movements(lot_id,kind,quantity_delta,reserved_delta,quantity_after,reserved_after,reason,order_id) SELECT id,'RESERVA_VENCIDA',0,?,quantity,reserved,'Plazo de pago agotado',? FROM lots WHERE id=?",((java.math.BigDecimal)item.get("quantity")).negate(),order.get("id"),item.get("lot_id"));}
   jdbc.update("DELETE FROM allocations WHERE order_id=?",order.get("id"));jdbc.update("UPDATE customer_orders SET status='RESERVA_VENCIDA',version=version+1 WHERE id=?",order.get("id"));
   jdbc.update("INSERT INTO audit_events(action,resource,status) VALUES('SYSTEM',?,200)","Reserva vencida de pedido "+order.get("id"));
  }
 }
}
