package com.vitna;
import com.vitna.Domain.Role;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequestMapping("/api/orders") @Transactional(readOnly=true)
public class PickingApi {
 final Api api; final Allocations allocations;
 public PickingApi(Api api,Allocations allocations){this.api=api;this.allocations=allocations;}
 @GetMapping("/picking") Object picking(Authentication auth){api.allow(auth,Role.ADMIN,Role.ALMACENERO);boolean admin=api.user(auth).role==Role.ADMIN;return allocations.findAll().stream().filter(a->admin||java.util.List.of("CONFIRMADO","EN_PREPARACION").contains(a.order.status)).map(a->Map.of("orderId",a.order.id,"product",a.lot.product.name,"lotCode",a.lot.code,"warehouse",a.lot.warehouse,"expiry",a.lot.expiry,"quantity",a.quantity,"status",a.lot.status)).toList();}
}
