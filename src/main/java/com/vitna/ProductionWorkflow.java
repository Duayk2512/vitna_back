package com.vitna;

import com.vitna.Domain.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service @Transactional
public class ProductionWorkflow {
 final Productions productions; final ProductionSteps steps; final Products products; final Accounts accounts; final EntityManager entityManager; final Lots lots;
 public ProductionWorkflow(Productions productions,ProductionSteps steps,Products products,Accounts accounts,EntityManager entityManager,Lots lots){this.productions=productions;this.steps=steps;this.products=products;this.accounts=accounts;this.entityManager=entityManager;this.lots=lots;}
 List<Map<String,Object>> list(Account actor){var rows=actor.role==Role.TRABAJADOR?productions.findByAssignedToId(actor.id):productions.findAll();return rows.stream().map(this::view).toList();}
 Object workers(){return accounts.findAll().stream().filter(a->a.active&&a.role==Role.TRABAJADOR).map(a->Map.of("id",a.id,"name",a.name)).toList();}
 Account worker(Long id){Account w=accounts.findById(id).orElseThrow();if(!w.active||w.role!=Role.TRABAJADOR)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Selecciona un trabajador activo");return w;}
 Production plan(Long productId,BigDecimal planned,String route,Long workerId){
  var names=Arrays.stream(route.split("\\s*(?:→|->|>|,)\\s*",-1)).map(String::trim).toList();
  if(names.size()>12||names.stream().anyMatch(s->s.isBlank()||s.length()>120))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Define entre 1 y 12 etapas, de máximo 120 caracteres, separadas por →");
  Production p=new Production();p.product=products.findById(productId).orElseThrow();p.planned=planned;p.route=String.join(" → ",names);if(workerId!=null)p.assignedTo=worker(workerId);productions.save(p);
  for(int i=0;i<names.size();i++){ProductionStep step=new ProductionStep();step.production=p;step.name=names.get(i);step.stepPosition=i+1;steps.save(step);}return p;
 }
 Production assign(long id,long workerId){Production p=productions.findById(id).orElseThrow();if(!p.status.equals("PLANIFICADA"))throw new ResponseStatusException(HttpStatus.CONFLICT,"Solo puedes asignar una orden que todavía no inició");p.assignedTo=worker(workerId);return p;}
 Production cancel(long id){Production p=productions.findById(id).orElseThrow();if(!p.status.equals("PLANIFICADA"))throw new ResponseStatusException(HttpStatus.CONFLICT,"Solo puedes cancelar órdenes sin iniciar");p.status="CANCELADA";return p;}
 Production advance(long id,long stepId,String action,BigDecimal produced,Account actor){
  Production p=productions.findById(id).orElseThrow();
  if(actor.role!=Role.TRABAJADOR||!actor.active||p.assignedTo==null||!Objects.equals(p.assignedTo.id,actor.id))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Esta orden no está asignada a tu cuenta");
  if(!List.of("PLANIFICADA","EN_CURSO").contains(p.status))throw new ResponseStatusException(HttpStatus.CONFLICT,"La orden ya está cerrada");
  entityManager.lock(p,LockModeType.OPTIMISTIC_FORCE_INCREMENT);
  var route=steps.findByProductionIdOrderByStepPosition(id);ProductionStep current=route.stream().filter(s->!s.status.equals("COMPLETADA")).findFirst().orElseThrow();
  if(!current.id.equals(stepId))throw new ResponseStatusException(HttpStatus.CONFLICT,"Completa las etapas anteriores primero");
  if("START".equals(action)&&current.status.equals("PENDIENTE")){current.status="EN_CURSO";current.startedAt=Instant.now();p.status="EN_CURSO";}
  else if("COMPLETE".equals(action)&&current.status.equals("EN_CURSO")){
   boolean last=current.stepPosition==route.get(route.size()-1).stepPosition;
   if(last){if(produced==null||produced.signum()<=0||produced.compareTo(p.planned)>0||produced.scale()>2)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"La cantidad obtenida debe ser mayor que cero y no superar la planificada, con máximo dos decimales");p.produced=produced;p.status="TERMINADA";}
   current.status="COMPLETADA";current.completedAt=Instant.now();
  }else throw new ResponseStatusException(HttpStatus.CONFLICT,"Acción no permitida para esta etapa");
  return p;
 }
 Map<String,Object> view(Production p){Map<String,Object> v=new LinkedHashMap<>();v.put("id",p.id);v.put("product",p.product);v.put("planned",p.planned);v.put("produced",p.produced);v.put("route",p.route);v.put("status",p.status);Lot output=lots.findByProductionId(p.id).orElse(null);v.put("outputLot",output==null?null:Map.of("id",output.id,"code",output.code,"status",output.status,"warehouse",output.warehouse));v.put("assignedTo",p.assignedTo==null?null:Map.of("id",p.assignedTo.id,"name",p.assignedTo.name));v.put("steps",steps.findByProductionIdOrderByStepPosition(p.id).stream().map(s->{Map<String,Object> step=new LinkedHashMap<>();step.put("id",s.id);step.put("name",s.name);step.put("position",s.stepPosition);step.put("status",s.status);step.put("startedAt",s.startedAt);step.put("completedAt",s.completedAt);return step;}).toList());return v;}
}
