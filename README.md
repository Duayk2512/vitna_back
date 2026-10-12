# VITNA backend

Java 17, Spring Boot 4 y PostgreSQL con Flyway V1–V4. Configurar DB_URL (JDBC), DB_USER y DB_PASSWORD; .env.example es una referencia y Spring no lo carga automáticamente. Ejecutar ./mvnw spring-boot:run o mvnw.cmd en Windows.

La primera cuenta admin@vitna.pe requiere VITNA_ADMIN_PASSWORD (mínimo 10 caracteres). Producción requiere SPRING_PROFILES_ACTIVE=prod, no genera productos de ejemplo y rechaza perfiles demo/preview. Nunca se incluye contraseña predeterminada.

Railway: raíz de este repositorio, Dockerfile y /railway.json. PostgreSQL privado en el mismo proyecto. El puerto proviene de PORT. Consultar DEPLOYMENT.md para variables y conexión con Vercel.

Seis roles con permisos en servidor, cuentas y productos, códigos de barras únicos CODE128, pedidos y reservas FEFO, inventario, producción asignada por etapas y recepción una sola vez en cuarentena para revisión de calidad.

Pruebas: ./mvnw clean verify con DB_URL/DB_USER/DB_PASSWORD de una base PostgreSQL exclusiva de pruebas. No utilizar la base operativa. Última verificación local: 27 pruebas aprobadas y una de demostración omitida.

Pendientes funcionales: recuperación de contraseña, fichas/fotos comerciales completas, comprobantes de pago, recetas/consumos/genealogía, kardex completo y auditoría persistente.
