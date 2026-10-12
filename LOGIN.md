# Diagnóstico de acceso

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

## Comprobación mínima de la cuenta existente

Ejecutar diagnose-admin.sql en la base Postgres del mismo entorno que vitna_back. La consulta es de solo lectura y no devuelve hashes ni contraseñas. BCRYPT_FORMAT solo confirma estructura, no verifica que una contraseña coincida.

Con Railway CLI ya autenticado, ejecutar railway link y seleccionar el proyecto y entorno correctos; luego railway connect Postgres (requiere psql instalado). Pegar las dos consultas del archivo. Alternativamente conectar pgAdmin mediante los datos de conexión del servicio Postgres. No compartir la URL de conexión porque contiene credenciales.

Interpretación: admin_exists=false con account_count>0 explica por qué el bootstrap no crea admin. Una fila active=false impide login; MISSING/UNEXPECTED_FORMAT requiere reparación controlada. Si se devuelve una cuenta ADMIN activa con BCRYPT_FORMAT y sin espacios, falta verificar la coincidencia de la contraseña dentro del entorno propietario. No se puede concluir que sea incorrecta a partir del 401 solamente. No cambiar DB_URL a otra base ni eliminar cuentas para repetir el bootstrap.
