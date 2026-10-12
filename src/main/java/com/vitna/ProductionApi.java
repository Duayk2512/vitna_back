package com.vitna;
import com.vitna.Domain.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@RestController @RequestMapping("/api/production") @Transactional
public class ProductionApi {
 final Api api; final ProductionWorkflow workflow;
 public ProductionApi(Api api,ProductionWorkflow workflow){this.api=api;this.workflow=workflow;}
 @GetMapping Object list(Authentication auth){api.allow(auth,Role.ADMIN,Role.JEFE_PRODUCCION,Role.TRABAJADOR,Role.CALIDAD);return workflow.list(api.user(auth));}
 @GetMapping("/workers") Object workers(Authentication auth){api.allow(auth,Role.JEFE_PRODUCCION);return workflow.workers();}
 record Plan(@NotNull Long productId,@NotNull @Positive @Digits(integer=10,fraction=2) BigDecimal planned,@NotBlank @Size(max=255) String route,Long workerId){}
 @PostMapping Object plan(@RequestBody @Valid Plan r,Authentication auth){api.allow(auth,Role.JEFE_PRODUCCION);return workflow.view(workflow.plan(r.productId(),r.planned(),r.route(),r.workerId()));}
 record Assignment(@NotNull Long workerId){}
 @PutMapping("/{id}/assignment") Object assign(@PathVariable long id,@RequestBody @Valid Assignment r,Authentication auth){api.allow(auth,Role.JEFE_PRODUCCION);return workflow.view(workflow.assign(id,r.workerId()));}
 @PostMapping("/{id}/cancel") Object cancel(@PathVariable long id,Authentication auth){api.allow(auth,Role.JEFE_PRODUCCION);return workflow.view(workflow.cancel(id));}
 record Advance(@NotBlank String action,@Positive @Digits(integer=10,fraction=2) BigDecimal produced){}
 @PostMapping("/{id}/steps/{stepId}") Object advance(@PathVariable long id,@PathVariable long stepId,@RequestBody @Valid Advance r,Authentication auth){api.allow(auth,Role.TRABAJADOR);return workflow.view(workflow.advance(id,stepId,r.action(),r.produced(),api.user(auth)));}
}
