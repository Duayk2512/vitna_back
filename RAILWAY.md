# Corregir conexión de vitna_back con Postgres

En Railway, abrir el servicio **vitna_back**, entorno **production**, pestaña **Variables**. La base **Postgres** existente debe permanecer intacta. Agregar estas variables en el backend, no en la base:

```text
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}
DB_USER=${{Postgres.PGUSER}}
DB_PASSWORD=${{Postgres.PGPASSWORD}}
```

Para la primera cuenta de administrador configurar además VITNA_ADMIN_PASSWORD con una contraseña elegida de al menos 10 caracteres. No pegar el secreto en GitHub ni en el chat. Si ya existen cuentas, la variable no cambia sus contraseñas.

Revisar los cambios pendientes de Railway y pulsar **Deploy** para aplicarlos. Usar el último commit de main. En este repositorio independiente Root Directory es la raíz y Config File Path es /railway.json. No añadir un Start Command que omita el entrypoint Docker.

No usar localhost para DB_URL y no copiar DATABASE_URL directamente: Spring espera JDBC. Las variables de Postgres no pasan automáticamente al backend; se enlazan con las referencias anteriores. Railway asigna PORT, que Spring utiliza con el perfil prod.

La imagen activa prod de forma predeterminada y valida las variables sin imprimir valores. No desactiva Flyway ni modifica datos para esconder errores. El registro debe mostrar perfil prod, conexión PostgreSQL y validación/aplicación Flyway; después debe aparecer Started VitnaApplication. Esta verificación en Railway está pendiente hasta aplicar las variables.

Prueba local de la validación: sh tests/entrypoint-test.sh. La prueba usa valores sintéticos y un ejecutable Java simulado; no abre conexiones ni sustituye una prueba real del despliegue.

Referencia: [variables entre servicios Railway](https://docs.railway.com/variables), [PostgreSQL en Railway](https://docs.railway.com/databases/postgresql).
