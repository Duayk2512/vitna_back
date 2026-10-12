package com.vitna;
import com.vitna.Domain.Production;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.math.*;
@Service public class ProductionMaterials {
 final JdbcTemplate jdbc;
 public ProductionMaterials(JdbcTemplate jdbc){this.jdbc=jdbc;}
 void plan(Production production){
  var recipes=jdbc.queryForList("SELECT * FROM recipes WHERE product_id=? AND active=true",production.product.id);
  if(recipes.isEmpty())return;var recipe=recipes.get(0);BigDecimal basis=(BigDecimal)recipe.get("output_quantity");
  for(var item:jdbc.queryForList("SELECT * FROM recipe_items WHERE recipe_id=? ORDER BY material_id",recipe.get("id"))){BigDecimal quantity=((BigDecimal)item.get("quantity")).multiply(production.planned).divide(basis,2,RoundingMode.UP);jdbc.update("INSERT INTO production_material_plan(production_id,material_id,quantity,recipe_id) VALUES(?,?,?,?)",production.id,item.get("material_id"),quantity,recipe.get("id"));}
 }
 void consume(Production production){
  if(jdbc.queryForObject("SELECT count(*) FROM raw_consumptions WHERE production_id=?",Integer.class,production.id)>0)throw new ResponseStatusException(HttpStatus.CONFLICT,"La orden ya tiene consumos registrados");
  for(var item:jdbc.queryForList("SELECT * FROM production_material_plan WHERE production_id=? ORDER BY material_id",production.id)){
   BigDecimal remaining=(BigDecimal)item.get("quantity");
   var lots=jdbc.queryForList("SELECT * FROM raw_lots WHERE material_id=? AND status='APROBADO' AND expiry>=(now() AT TIME ZONE 'America/Lima')::date AND quantity>0 ORDER BY expiry,id FOR UPDATE",item.get("material_id"));
   for(var lot:lots){BigDecimal take=((BigDecimal)lot.get("quantity")).min(remaining);jdbc.update("UPDATE raw_lots SET quantity=quantity-? WHERE id=?",take,lot.get("id"));jdbc.update("INSERT INTO raw_consumptions(production_id,raw_lot_id,quantity) VALUES(?,?,?)",production.id,lot.get("id"),take);remaining=remaining.subtract(take);if(remaining.signum()==0)break;}
   if(remaining.signum()>0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Materia prima aprobada insuficiente; no se inicio la fabricacion ni se descontaron consumos");
  }
 }
 Object view(long productionId){return jdbc.queryForList("SELECT c.quantity,r.code,r.expiry,m.name,m.unit FROM raw_consumptions c JOIN raw_lots r ON r.id=c.raw_lot_id JOIN raw_materials m ON m.id=r.material_id WHERE c.production_id=? ORDER BY c.id",productionId);}
}
