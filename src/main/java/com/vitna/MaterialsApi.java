package com.vitna;
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
@RestController @RequestMapping("/api/materials") @Transactional public class MaterialsApi {
 final Access access;final JdbcTemplate jdbc;final Products products;
 public MaterialsApi(Access access,JdbcTemplate jdbc,Products products){this.access=access;this.jdbc=jdbc;this.products=products;}
 @GetMapping Object list(Authentication auth){access.actor(auth,ADMIN,ALMACENERO,JEFE_PRODUCCION,CALIDAD);return jdbc.queryForList("SELECT m.*,coalesce(sum(CASE WHEN r.status='APROBADO' AND r.expiry>=(now() AT TIME ZONE 'America/Lima')::date THEN r.quantity ELSE 0 END),0) AS available FROM raw_materials m LEFT JOIN raw_lots r ON r.material_id=m.id GROUP BY m.id ORDER BY m.name");}
 @GetMapping("/lots") Object lots(Authentication auth){access.actor(auth,ADMIN,ALMACENERO,JEFE_PRODUCCION,CALIDAD);return jdbc.queryForList("SELECT r.*,m.name,m.unit FROM raw_lots r JOIN raw_materials m ON m.id=r.material_id ORDER BY r.expiry,r.id");}
 record Material(@NotBlank @Size(max=150) String name,@NotBlank @Size(max=30) String unit){}
 @PostMapping Object create(@RequestBody @Valid Material input,Authentication auth){access.actor(auth,ADMIN,JEFE_PRODUCCION);jdbc.update("INSERT INTO raw_materials(name,unit) VALUES(?,?)",input.name().trim(),input.unit().trim());return Map.of("message","Materia prima creada");}
 record Receipt(@NotNull Long materialId,@NotBlank @Size(max=100) String code,@NotBlank @Size(max=150) String warehouse,@NotNull @FutureOrPresent LocalDate expiry,@NotNull @Positive @Digits(integer=10,fraction=2) BigDecimal quantity){}
 @PostMapping("/lots") Object receive(@RequestBody @Valid Receipt input,Authentication auth){access.actor(auth,ADMIN,ALMACENERO);jdbc.update("INSERT INTO raw_lots(material_id,code,warehouse,expiry,quantity) VALUES(?,?,?,?,?)",input.materialId(),input.code().trim().toUpperCase(Locale.ROOT),input.warehouse().trim(),input.expiry(),input.quantity());return Map.of("message","Materia prima recibida en cuarentena");}
 record Quality(@NotBlank String status,@Size(max=1000) String note){}
 @PostMapping("/lots/{id}/quality") Object quality(@PathVariable long id,@RequestBody @Valid Quality input,Authentication auth){access.actor(auth,CALIDAD);if(!Set.of("APROBADO","RECHAZADO","CUARENTENA").contains(input.status())||(!input.status().equals("APROBADO")&&(input.note()==null||input.note().isBlank())))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Estado u observacion invalidos");if(jdbc.update("UPDATE raw_lots SET status=?,quality_note=? WHERE id=? AND (?<>'APROBADO' OR expiry>=(now() AT TIME ZONE 'America/Lima')::date)",input.status(),input.note(),id,input.status())!=1)throw new ResponseStatusException(HttpStatus.CONFLICT,"Materia prima inexistente o vencida");return Map.of("message","Revision de materia prima registrada");}
 record Ingredient(@NotNull Long materialId,@NotNull @Positive @Digits(integer=10,fraction=2) BigDecimal quantity){}
 record Recipe(@NotNull Long productId,@NotNull @Positive @Digits(integer=10,fraction=2) BigDecimal outputQuantity,@NotEmpty @Size(max=30) List<@Valid Ingredient> items){}
 @GetMapping("/recipes") Object recipes(Authentication auth){access.actor(auth,ADMIN,JEFE_PRODUCCION,CALIDAD);var rows=jdbc.queryForList("SELECT r.*,p.name AS product_name FROM recipes r JOIN products p ON p.id=r.product_id WHERE r.active=true ORDER BY r.id DESC");for(var row:rows)row.put("items",jdbc.queryForList("SELECT i.*,m.name,m.unit FROM recipe_items i JOIN raw_materials m ON m.id=i.material_id WHERE i.recipe_id=? ORDER BY i.material_id",row.get("id")));return rows;}
 @PostMapping("/recipes") Object recipe(@RequestBody @Valid Recipe input,Authentication auth){access.actor(auth,ADMIN,JEFE_PRODUCCION);products.lockProduct(input.productId()).orElseThrow();if(input.items().stream().map(Ingredient::materialId).distinct().count()!=input.items().size())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Cada materia prima debe aparecer una sola vez");jdbc.update("UPDATE recipes SET active=false WHERE product_id=? AND active=true",input.productId());Long id=jdbc.queryForObject("INSERT INTO recipes(product_id,output_quantity) VALUES(?,?) RETURNING id",Long.class,input.productId(),input.outputQuantity());for(var item:input.items())jdbc.update("INSERT INTO recipe_items(recipe_id,material_id,quantity) VALUES(?,?,?)",id,item.materialId(),item.quantity());return Map.of("message","Receta guardada. Las ordenes ya planificadas conservan su receta anterior.");}
}
