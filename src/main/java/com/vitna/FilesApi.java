package com.vitna;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import static com.vitna.Domain.Role.*;
@RestController @RequestMapping("/api") public class FilesApi {
 final Access access;final JdbcTemplate jdbc;final Products products;final Orders orders;
 public FilesApi(Access access,JdbcTemplate jdbc,Products products,Orders orders){this.access=access;this.jdbc=jdbc;this.products=products;this.orders=orders;}
 static String media(byte[] bytes,boolean pdf){
  if(bytes.length>=8&&bytes[0]==(byte)0x89&&bytes[1]==0x50&&bytes[2]==0x4e&&bytes[3]==0x47&&bytes[4]==13&&bytes[5]==10&&bytes[6]==26&&bytes[7]==10)return "image/png";
  if(bytes.length>=3&&bytes[0]==(byte)0xff&&bytes[1]==(byte)0xd8&&bytes[2]==(byte)0xff)return "image/jpeg";
  if(bytes.length>=12&&new String(bytes,0,4,StandardCharsets.US_ASCII).equals("RIFF")&&new String(bytes,8,4,StandardCharsets.US_ASCII).equals("WEBP"))return "image/webp";
  if(pdf&&bytes.length>=5&&new String(bytes,0,5,StandardCharsets.US_ASCII).equals("%PDF-"))return "application/pdf";
  throw new ResponseStatusException(HttpStatus.BAD_REQUEST,pdf?"Adjunta JPG, PNG, WebP o PDF":"Selecciona una foto JPG, PNG o WebP");
 }
 byte[] data(MultipartFile file,int limit){if(file.isEmpty()||file.getSize()>limit)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Archivo vacio o demasiado grande");try{return file.getBytes();}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"No se pudo leer el archivo");}}
 @PostMapping("/products/{id}/photo") @Transactional Object photo(@PathVariable long id,@RequestParam("file") MultipartFile file,Authentication auth){var actor=access.actor(auth,ADMIN);products.lockProduct(id).orElseThrow();byte[] bytes=data(file,2*1024*1024);String type=media(bytes,false);jdbc.update("INSERT INTO stored_files(product_id,media_type,content,uploaded_by) VALUES(?,?,?,?) ON CONFLICT(product_id) WHERE product_id IS NOT NULL DO UPDATE SET media_type=excluded.media_type,content=excluded.content,uploaded_by=excluded.uploaded_by,created_at=now()",id,type,bytes,actor.id);return Map.of("message","Foto real del producto actualizada");}
 @GetMapping({"/public/products/{id}/photo","/products/{id}/photo"}) Object image(@PathVariable long id,jakarta.servlet.http.HttpServletRequest request,Authentication auth){var product=products.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));if(request.getRequestURI().contains("/public/")&&!product.published)throw new ResponseStatusException(HttpStatus.NOT_FOUND);if(!request.getRequestURI().contains("/public/"))access.actor(auth);var rows=jdbc.queryForList("SELECT media_type,content FROM stored_files WHERE product_id=?",id);if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND);return ResponseEntity.ok().header("X-Content-Type-Options","nosniff").cacheControl(CacheControl.noCache()).contentType(MediaType.parseMediaType((String)rows.get(0).get("media_type"))).body((byte[])rows.get(0).get("content"));}
 void owner(long id,Authentication auth){var actor=access.actor(auth,ADMIN,CLIENTE);var order=orders.findById(id).orElseThrow();if(actor.role==CLIENTE&&!order.customer.id.equals(actor.id))throw new ResponseStatusException(HttpStatus.FORBIDDEN);}
 @GetMapping("/orders/{id}/proofs") Object proofs(@PathVariable long id,Authentication auth){owner(id,auth);return jdbc.queryForList("SELECT id,media_type,created_at FROM stored_files WHERE order_id=? ORDER BY id DESC",id);}
 @PostMapping("/orders/{id}/proofs") @Transactional Object proof(@PathVariable long id,@RequestParam("file") MultipartFile file,Authentication auth){owner(id,auth);var actor=access.actor(auth);jdbc.queryForList("SELECT id FROM customer_orders WHERE id=? FOR UPDATE",id);String status=jdbc.queryForObject("SELECT status FROM customer_orders WHERE id=?",String.class,id);if(!List.of("PENDIENTE_REVISION","PENDIENTE_PAGO").contains(status))throw new ResponseStatusException(HttpStatus.CONFLICT,"El pedido ya no admite nuevos comprobantes");if(jdbc.queryForObject("SELECT count(*) FROM stored_files WHERE order_id=?",Integer.class,id)>=5)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Maximo cinco comprobantes por pedido");byte[] bytes=data(file,3*1024*1024);String type=media(bytes,true);jdbc.update("INSERT INTO stored_files(order_id,media_type,content,uploaded_by) VALUES(?,?,?,?)",id,type,bytes,actor.id);return Map.of("message","Comprobante adjunto. El administrador debe verificar el pago.");}
 @GetMapping("/orders/{id}/proofs/{fileId}") Object download(@PathVariable long id,@PathVariable long fileId,Authentication auth){owner(id,auth);var rows=jdbc.queryForList("SELECT media_type,content FROM stored_files WHERE id=? AND order_id=?",fileId,id);if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND);return ResponseEntity.ok().header("Content-Disposition","attachment; filename=\"comprobante-"+fileId+".bin\"").header("X-Content-Type-Options","nosniff").cacheControl(CacheControl.noStore()).contentType(MediaType.parseMediaType((String)rows.get(0).get("media_type"))).body((byte[])rows.get(0).get("content"));}
 record Details(@NotBlank @Size(max=30) String unit,@Size(max=2000) String ingredients,@Size(max=1000) String allergens,@Size(max=1000) String storageInfo,@Size(max=100) String netWeight){}
 @PutMapping("/products/{id}/details") @Transactional Object details(@PathVariable long id,@RequestBody @Valid Details input,Authentication auth){access.actor(auth,ADMIN);var product=products.lockProduct(id).orElseThrow();product.unit=input.unit().trim();product.ingredients=input.ingredients();product.allergens=input.allergens();product.storageInfo=input.storageInfo();product.netWeight=input.netWeight();return products.save(product);}
}
