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
