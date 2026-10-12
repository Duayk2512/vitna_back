# Accesos de prueba por activacion explicita

Desactivados por defecto. El propietario autoriza la activacion mediante VITNA_TEST_ACCESS_ENABLED=true y seis secretos VITNA_TEST_PASSWORD_ADMIN, VITNA_TEST_PASSWORD_CLIENTE, VITNA_TEST_PASSWORD_ALMACENERO, VITNA_TEST_PASSWORD_JEFE_PRODUCCION, VITNA_TEST_PASSWORD_TRABAJADOR y VITNA_TEST_PASSWORD_CALIDAD. Minimo 16 caracteres, maximo 72 bytes UTF-8, todos distintos. No incluir secretos en GitHub ni logs.

Destinos exactos:

| Rol | Correo |
| --- | --- |
| ADMIN | admin@vitna.pe |
| CLIENTE | cliente.test@vitna.pe |
| ALMACENERO | almacenero.test@vitna.pe |
| JEFE_PRODUCCION | jefe_produccion.test@vitna.pe |
| TRABAJADOR | trabajador.test@vitna.pe |
| CALIDAD | calidad.test@vitna.pe |

Al activar y desplegar, crea destinos ausentes, establece el hash BCrypt y activa solo estas cuentas. Conserva identificadores y datos de cuentas existentes; rechaza cualquier destino existente con rol diferente. Valida todos los secretos antes de cambios y usa una transaccion. No modifica otros usuarios, pedidos, productos o stock. No registra secretos. Repetir con las mismas claves es idempotente.

En Railway, vitna_back > Variables > RAW Editor: importar el JSON privado preparado localmente por el propietario. Conservar DB_URL/DB_USER/DB_PASSWORD y SPRING_PROFILES_ACTIVE=prod existentes. El JSON solo agrega variables de acceso; comprobar que el editor no elimine las variables previas al guardar. Desplegar el ultimo commit de main. Esperar VITNA_TEST_ACCESS_APPLIED en Deploy Logs y verificar login en Vercel.

Despues cambiar VITNA_TEST_ACCESS_ENABLED=false y eliminar las seis VITNA_TEST_PASSWORD_*. Mantener VITNA_ADMIN_PASSWORD sincronizada con la nueva clave admin y desactivar VITNA_AUTH_DIAGNOSTICS. Desplegar otra vez. Ese reinicio invalida sesiones antiguas. Las nuevas contrasenas permanecen en la base como hashes; no se restablecen al desactivar.

Estas cuentas tienen permisos reales y pueden cambiar datos; trabajar con registros de prueba identificables. Antes de uso operativo cambiar la clave admin y desactivar las cinco cuentas de prueba desde Usuarios. No volver a activar la provision sin decidir explicitamente un nuevo cambio de claves. No existe endpoint publico para esta operacion.

Estado local: credenciales preparadas en archivo privado. No afirmar que se aplicaron hasta verificar la ejecucion en Railway y un login real. La aplicacion remota necesita acceso al panel por el propietario.
