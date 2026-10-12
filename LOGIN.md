# Diagnóstico de acceso

## Sin terminal: diagnostico desde el panel Railway

En el servicio vitna_back, Variables, agregar VITNA_AUTH_DIAGNOSTICS=true y desplegar el ultimo commit. Abrir Deploy Logs y buscar VITNA_AUTH_DIAGNOSTIC. Este modo consulta la misma base del backend y compara el valor actual de VITNA_ADMIN_PASSWORD con el hash, sin imprimir secretos ni cambiar cuentas.

- BOOTSTRAP_MATCH: la variable actual coincide con la cuenta almacenada. No demuestra que el navegador este enviando esa misma contrasena.
- BOOTSTRAP_MISMATCH: la variable actual no coincide. Cambiarla no actualiza la contrasena de una cuenta ya creada.
- ADMIN_NOT_FOUND, ADMIN_NOT_ACTIVE o HASH_FORMAT_INVALID: revisar el estado indicado antes de modificar contrasenas.
- ACCOUNT_CHECK_FAILED: fallo de consulta o verificacion, distinto de una discrepancia de contrasena.

Compartir solo esa linea del diagnostico. Desactivar la variable despues. No pegar secretos en SQL; el editor PostgreSQL no ejecuta comandos Java o Railway CLI.

El administrador inicial usa **admin@vitna.pe**. Se crea únicamente cuando la tabla accounts está vacía y VITNA_ADMIN_PASSWORD tiene al menos 10 caracteres. La contraseña se almacena con BCrypt. Modificar esa variable y volver a desplegar NO actualiza una cuenta existente. Las cuentas demo del equipo local no se copian a Railway.

Desde el frontend real, /api/csrf debe devolver JSON y establecer JSESSIONID bajo el dominio Vercel. Una respuesta 401 de /api/me sin sesión es normal. Un 401 de login con CSRF válido indica credenciales no aceptadas o cuenta inactiva; un token incorrecto debe devolver 403. La corrección de Security evita que el procesamiento secundario de /error convierta ese 403 en un 401 genérico.

1. Desplegar el último commit del backend Railway.
2. Recargar la página de Vercel para obtener una nueva sesión/token.
3. Usar admin@vitna.pe y la contraseña que estaba configurada cuando se creó esa cuenta. No enviar contraseñas al chat ni guardarlas en GitHub.
4. Si falla, comprobar la cuenta mediante esta consulta de solo lectura en la base existente:

```sql
SELECT email, role, active
FROM accounts
WHERE lower(email) = 'admin@vitna.pe';
```

No consultar ni compartir password_hash. Si no existe la cuenta pero hay otras cuentas, el bootstrap no crea otro administrador automáticamente. Si existe y está inactiva, un administrador autorizado puede revisar su estado. Si se perdió la contraseña original y no hay otro administrador con acceso, se necesita una recuperación controlada por el propietario de la base: respaldo y actualización puntual de la cuenta con un hash BCrypt generado por PasswordEncoder, sin borrar cuentas ni pedidos. La aplicación aún no incluye recuperación por correo ni cambio automático de contraseña desde una variable.

No borrar tablas, volúmenes ni desactivar CSRF. No habilitar demo en producción. La validez de una contraseña real solo puede verificarla el propietario al iniciar sesión; las pruebas de integración usan identidades sintéticas.

## Verificacion dentro del backend y recuperacion del propietario

VITNA usa formLogin de Spring Security, BCrypt y sesion JSESSIONID, no JWT. El formulario envia email/password como application/x-www-form-urlencoded. El navegador llama al mismo origen Vercel /api; no falta una variable CORS ni un token JWT.

Tras desplegar el commit que incorpora AdminAccessTool, abrir una terminal interactiva dentro de vitna_back con Railway CLI autenticado:

```sh
railway ssh --service vitna_back --environment production
```

Dentro de esa terminal ejecutar (sin pegar contrasenas):

```sh
java -Dloader.main=com.vitna.AdminAccessTool -cp /app/vitna.jar org.springframework.boot.loader.launch.PropertiesLauncher --check-bootstrap
```

Este modo compara VITNA_ADMIN_PASSWORD actual con el hash de la cuenta en la misma base DB_URL que usa el servidor. Solo muestra MATCH o MISMATCH. MISMATCH demuestra que cambiar la variable no sincronizo la cuenta; no prueba que la contrasena que recuerdas sea incorrecta.

Para comprobar la contrasena que intentas utilizar, cambiar --check-bootstrap por --verify. La pide de forma oculta en la terminal; no pasarla en argumentos ni compartirla. MATCH confirma coincidencia BCrypt, sin modificar datos. Si devuelve MATCH pero el login falla, revisar la respuesta exacta y que la terminal corresponda al servicio/entorno/instancia del dominio publico antes de cambiar datos.

Si necesitas recuperar acceso, obtener respaldo primero. Solo el propietario, al decidir cambiar la contrasena, puede ejecutar el mismo comando con --reset. Pide nueva contrasena oculta dos veces y la confirmacion literal RESET admin@vitna.pe. Actualiza exclusivamente el hash de la cuenta ADMIN activa existente y detecta cambios concurrentes. No crea cuentas, no activa cuentas y no modifica pedidos. No se ejecuta automaticamente al desplegar. Reiniciar backend despues para invalidar sesiones anteriores y luego entrar desde Vercel con la nueva contrasena. Esta operacion no actualiza VITNA_ADMIN_PASSWORD; la variable es solo bootstrap.

El ejecutable y el modo de solo lectura se validaron localmente en PostgreSQL aislado con valores sinteticos; no se ha ejecutado recuperacion en produccion ni accedido a secretos del usuario.

Referencia: [terminal de servicio Railway](https://docs.railway.com/cli/ssh).

## Comprobación mínima de la cuenta existente

Ejecutar diagnose-admin.sql en la base Postgres del mismo entorno que vitna_back. La consulta es de solo lectura y no devuelve hashes ni contraseñas. BCRYPT_FORMAT solo confirma estructura, no verifica que una contraseña coincida.

Con Railway CLI ya autenticado, ejecutar railway link y seleccionar el proyecto y entorno correctos; luego railway connect Postgres (requiere psql instalado). Pegar las dos consultas del archivo. Alternativamente conectar pgAdmin mediante los datos de conexión del servicio Postgres. No compartir la URL de conexión porque contiene credenciales.

Interpretación: admin_exists=false con account_count>0 explica por qué el bootstrap no crea admin. Una fila active=false impide login; MISSING/UNEXPECTED_FORMAT requiere reparación controlada. Si se devuelve una cuenta ADMIN activa con BCRYPT_FORMAT y sin espacios, falta verificar la coincidencia de la contraseña dentro del entorno propietario. No se puede concluir que sea incorrecta a partir del 401 solamente. No cambiar DB_URL a otra base ni eliminar cuentas para repetir el bootstrap.
