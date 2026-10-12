package com.vitna;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
@RestController @RequestMapping("/api/orders") public class OrderActions {
 @org.springframework.beans.factory.annotation.Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
 final OrderWorkflow workflow;final Accounts accounts;
 public OrderActions(OrderWorkflow w,Accounts a){workflow=w;accounts=a;}
 @PostMapping("/{id}/status") @org.springframework.transaction.annotation.Transactional Object change(@PathVariable long id,@RequestBody Map<String,String> body,Authentication auth){var account=accounts.findByEmail(auth.getName()).orElseThrow();if(!account.active)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Cuenta desactivada");var role=account.role;if(role!=Domain.Role.ADMIN&&role!=Domain.Role.ALMACENERO)throw new ResponseStatusException(HttpStatus.FORBIDDEN);String note=body.get("paymentNote");if(note!=null&&note.length()>1000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Observacion de pago demasiado larga");if(role==Domain.Role.ADMIN&&"CONFIRMADO".equals(body.get("status"))&&(note==null||note.isBlank())&&(jdbc==null||jdbc.queryForObject("SELECT count(*) FROM stored_files WHERE order_id=?",Integer.class,id)==0))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Adjunta un comprobante o indica una referencia de pago antes de verificar");var o=workflow.transition(id,body.getOrDefault("status",""),role);if(role==Domain.Role.ADMIN&&note!=null)o.paymentNote=note.trim();return Map.of("id",o.id,"status",o.status);}
}
