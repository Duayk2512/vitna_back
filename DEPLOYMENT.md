# GitHub + Railway + Vercel

Archivos locales preparados. La publicación requiere los enlaces GitHub y acceso a Railway/Vercel. No se han creado repositorios, dominios ni credenciales externos.

## Backend en Railway

Conectar el repositorio y seleccionar Root Directory `backend` en monorepositorio, o la raíz en repositorio independiente. Configurar Railway Config File Path `/backend/railway.json` o `/railway.json`, respectivamente. El Dockerfile usa Java 17 y un usuario sin privilegios. La aplicación escucha el PORT asignado por Railway.

Crear PostgreSQL persistente en el mismo proyecto. Configurar variables del backend mediante referencias al servicio PostgreSQL real (sustituir el nombre Postgres si corresponde):

```text
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}
DB_USER=${{Postgres.PGUSER}}
DB_PASSWORD=${{Postgres.PGPASSWORD}}
VITNA_ADMIN_PASSWORD=<contraseña elegida de al menos 10 caracteres>
VITNA_WHATSAPP=<número internacional solo dígitos>
```

Usar red privada para PostgreSQL. Generar dominio HTTPS público del backend para el proxy Vercel. Flyway aplica V1–V4. Una instalación vacía crea admin@vitna.pe con el secreto configurado, sin productos de ejemplo. Cambiar la variable después no cambia una contraseña existente. No activar demo o preview en producción.

## Frontend en Vercel

Dentro de frontend, con el dominio real de Railway:

```sh
node configure-vercel.mjs https://DOMINIO-REAL-DEL-BACKEND
```

Incluir el vercel.json generado en GitHub antes de desplegar; Vercel lee configuración antes del build. El dominio público del backend no es un secreto. Seleccionar Root Directory frontend en monorepositorio, o raíz en repositorio independiente. Node 24, build npm run build, salida dist/frontend/browser.

El navegador llama /api bajo el dominio Vercel. El rewrite externo reenvía solicitudes a Railway. Las cookies sin Domain pertenecen así al frontend; conservan Secure, HttpOnly y SameSite=Lax. Mantener CSRF activo y obtener /api/csrf antes de operaciones de escritura. No configurar llamadas directas desde el navegador al dominio Railway ni abrir CORS. Las sesiones están en memoria en una instancia y se pierden al reiniciar backend.

## Validación y datos

Frontend: npm ci y npm run build con Node 24. Backend: Java 17 y mvnw.cmd clean verify en Windows o ./mvnw clean verify en Linux. Las pruebas requieren DB_URL, DB_USER y DB_PASSWORD de una base exclusivamente de pruebas; nunca apuntarlas a datos operativos. El build Docker omite pruebas que necesitan esa base; ejecutarlas por separado.

Después de publicar, probar desde Vercel login, /api/me, token CSRF, recarga, logout y los seis roles. Verificar cookies sin Domain del backend y respuestas privadas sin caché. Esta validación requiere dominios reales y está pendiente. Utilizar backend de pruebas para Preview si se harán escrituras.

Antes de actualizar datos existentes, obtener pg_dump -Fc y comprobar restauración aislada. Restaurar con pg_restore --no-owner --no-privileges antes de iniciar backend. No copiar directorios físicos entre versiones PostgreSQL distintas. Configurar respaldos de Railway y mantener copias fuera del servicio. V4 vincula producción y lote; recibir una vez en cuarentena y liberar por calidad antes de vender.

Referencias: [Spring Boot en Railway](https://docs.railway.com/guides/spring-boot), [proxy externo Vercel](https://vercel.com/docs/routing/rewrites).
