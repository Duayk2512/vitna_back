package com.vitna;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.Map;
@RestControllerAdvice public class Errors {
 @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class) ResponseEntity<?> status(org.springframework.web.server.ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",e.getReason()==null?"Operación no permitida":e.getReason()));}
 @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class) ResponseEntity<?> invalid(org.springframework.web.bind.MethodArgumentNotValidException e){Map<String,String> fields=new java.util.LinkedHashMap<>();for(var error:e.getBindingResult().getFieldErrors()){String code=error.getCode();String hint=switch(code==null?"":code){case "NotNull","NotBlank","NotEmpty"->"Este campo es obligatorio.";case "Positive"->"Ingresa una cantidad mayor que cero.";case "PositiveOrZero"->"El valor no puede ser negativo.";case "FutureOrPresent"->"Selecciona hoy o una fecha futura.";case "Email"->"Ingresa un correo válido.";case "Digits"->"Usa hasta dos decimales y respeta el límite de cantidad.";case "Size"->"Revisa la longitud permitida para este campo.";default->"Revisa este valor.";};fields.putIfAbsent(error.getField(),hint);}return ResponseEntity.badRequest().body(Map.of("message","Revisa los campos indicados antes de guardar.","errors",fields));}
 @ExceptionHandler(org.springframework.dao.OptimisticLockingFailureException.class) ResponseEntity<?> stale(){return ResponseEntity.status(409).body(Map.of("message","Otro usuario actualizó este registro. Recarga los datos antes de continuar."));}
 @ExceptionHandler(DataIntegrityViolationException.class) ResponseEntity<?> conflict(){return ResponseEntity.status(409).body(Map.of("message","El registro ya existe o tiene relaciones inválidas."));}
 @ExceptionHandler(java.util.NoSuchElementException.class) ResponseEntity<?> missing(){return ResponseEntity.status(404).body(Map.of("message","Registro no encontrado."));}
}
