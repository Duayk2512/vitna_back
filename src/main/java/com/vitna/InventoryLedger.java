package com.vitna;
import com.vitna.Domain.Lot;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import java.math.BigDecimal;
@Service public class InventoryLedger {
 final JdbcTemplate jdbc;
 public InventoryLedger(JdbcTemplate jdbc){this.jdbc=jdbc;}
 void record(Lot lot,String kind,BigDecimal delta,BigDecimal reservedDelta,String reason,Long actor,Long order){jdbc.update("INSERT INTO stock_movements(lot_id,kind,quantity_delta,reserved_delta,quantity_after,reserved_after,reason,actor_id,order_id) VALUES(?,?,?,?,?,?,?,?,?)",lot.id,kind,delta,reservedDelta,lot.quantity,lot.reserved,reason,actor,order);}
}
