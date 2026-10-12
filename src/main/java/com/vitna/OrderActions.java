package com.vitna;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
@RestController @RequestMapping("/api/orders") public class OrderActions {
 final OrderWorkflow workflow;final Accounts accounts;
 public OrderActions(OrderWorkflow w,Accounts a){workflow=w;accounts=a;}
 @PostMapping("/{id}/status") Object change(@PathVariable long id,@RequestBody Map<String,String> body,Authentication auth){var account=accounts.findByEmail(auth.getName()).orElseThrow();if(!account.active)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Cuenta desactivada");var role=account.role;if(role!=Domain.Role.ADMIN&&role!=Domain.Role.ALMACENERO)throw new ResponseStatusException(HttpStatus.FORBIDDEN);var o=workflow.transition(id,body.getOrDefault("status",""),role);return Map.of("id",o.id,"status",o.status);}
}
