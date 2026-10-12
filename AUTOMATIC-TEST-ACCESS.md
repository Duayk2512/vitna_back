# Accesos autorizados automaticamente una vez

V5__authorized_test_access.sql configura los seis destinos autorizados al desplegar backend. No requiere variables nuevas ni endpoint. Flyway guarda su ejecucion y no repite la modificacion en reinicios. No se modificaron migraciones anteriores.

Los destinos son admin@vitna.pe, cliente.test@vitna.pe, almacenero.test@vitna.pe, jefe_produccion.test@vitna.pe, trabajador.test@vitna.pe y calidad.test@vitna.pe. Las claves estan en el archivo privado local .secrets/vitna-test-access.md del propietario. El repositorio contiene solo hashes BCrypt, nunca claves en texto plano.

El administrador conserva identificador y datos; cambia solo el hash y estado activo. Los otros cinco destinos se insertan si faltan o se actualizan si tienen el rol esperado. Se aborta ante correos duplicados o roles diferentes. No se modifican usuarios ajenos, productos, pedidos ni stock. La migracion es transaccional.

Buscar V5 aplicada y Started VitnaApplication en Railway, luego probar login. Publicar el commit no demuestra que Railway ya aplico el insert. No activar VITNA_TEST_ACCESS_ENABLED: el mecanismo anterior por variables no es necesario. Si ya estaba activo, desactivarlo para impedir que otro conjunto de claves reescriba los destinos.

VITNA_ADMIN_PASSWORD es solo bootstrap de base vacia; BOOTSTRAP_MISMATCH con una variable antigua es esperable despues de cambiar la clave admin. Despues de las pruebas desactivar las cuentas dedicadas y cambiar la clave admin para uso operativo. No editar V5 aplicada ni borrar historial Flyway. Rotaciones futuras requieren acciones administrativas nuevas.
