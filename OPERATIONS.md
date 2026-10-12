# Mejoras de operacion VITNA

La migracion V6 agrega controles operativos sin modificar hashes, correos ni roles existentes. V5 conserva su checksum y su ejecucion unica. El frontend conserva el componente principal: no se reorganizo en modulos. Los respaldos y la restauracion quedan fuera de esta entrega, por solicitud del propietario.

## Brevo, recuperacion y seguridad

En Railway, servicio vitna_back, configurar **VITNA_SETTINGS_KEY** con una clave aleatoria de al menos 32 caracteres. Se preparo una clave privada local en `.secrets/brevo-settings-key.txt`; no se publica en GitHub. Conservarla: protege la API key y el contenido de la cola mediante AES-GCM. Cambiarla sin volver a configurar la API impide leer los secretos anteriores.

En Administrador → **Correos · Brevo**, ingresar API key, correo remitente verificado en Brevo, nombre y origen HTTPS del frontend. La API key es de escritura: la consulta solo muestra si existe. Guardar, activar y solicitar una prueba a la cuenta del administrador. La cola muestra PENDING, SENT o FAILED; SENT significa aceptado por el proveedor, no garantiza entrega a la bandeja. Los envios se intentan tres veces con espera de dos minutos y no registran claves ni enlaces en logs. [API oficial de correo Brevo](https://developers.brevo.com/docs/send-a-transactional-email).

El login ofrece recuperacion con enlace de un solo uso que vence en 20 minutos. El token solo se guarda como hash y el correo pendiente queda cifrado. No se revela si un correo esta registrado. Si Brevo no esta configurado, se informa indisponibilidad general. Seguridad de mi cuenta permite cambiar la clave verificando la anterior; los cambios invalidan las sesiones anteriores. Las cuentas de prueba con dominio de VITNA solo recibiran recuperacion si esos buzones existen realmente.

Se mantienen sesiones y CSRF mediante el proxy Vercel; no se agrega JWT ni CORS abierto. Login y recuperacion limitan intentos por direccion de origen, en memoria de la unica instancia. La auditoria registra actor, operacion, recurso, fecha y resultado de mutaciones autenticadas; no almacena cuerpos de solicitudes, contrasenas, hashes de usuario, API keys, cookies ni tokens. No reconstruye acciones anteriores a V6.

## Inventario

**Kardex** conserva recepciones, reservas, liberaciones, despachos, ajustes, mermas, vencimientos y devoluciones de productos. Los lotes anteriores reciben un saldo inicial, identificado como apertura de historial. **Ajustes y mermas** permite solicitar cambios; solo ADMIN aprueba y aplica. No se puede descontar cantidad reservada ni dejar stock negativo. Vencimiento se aplica exclusivamente a lotes vencidos.

**Devoluciones** recibe mercancía de pedidos ENTREGADO, con un nuevo lote en CUARENTENA. El acumulado no supera lo entregado. Calidad decide su liberacion; una devolucion vencida no puede aprobarse. Este registro es fisico: no procesa reembolsos bancarios automaticamente.

## Recetas y materias primas

ADMIN y JEFE_PRODUCCION registran materias primas y recetas; almacen recibe lotes en cuarentena y CALIDAD los revisa. Los consumos utilizan solamente lotes aprobados y no vencidos, por FEFO. Las unidades se definen en la materia prima: usar la misma unidad al formular y recibir.

Una receta define ingredientes para una cantidad base de producto terminado. Al planificar una orden con receta activa se conserva una copia de cantidades e identificador de receta. Al iniciar la primera etapa se descuenta la cantidad planificada proporcionalmente, redondeada hacia arriba a dos decimales; falta de material revierte toda la operacion. Los lotes consumidos quedan vinculados a produccion y al lote terminado mediante la recepcion existente. Las ordenes anteriores o planificadas sin receta no descuentan materias primas: definir la receta activa antes de planificar cuando se requiere controlar consumos. No se convierten automaticamente kg/g/L ni se ajustan consumos por rendimiento real.

## Pagos y productos

El cliente o administrador adjunta hasta cinco comprobantes JPG, PNG, WebP o PDF por pedido, hasta 3 MB cada uno. Solo propietario y ADMIN los descargan. Adjuntar no confirma pago; ADMIN verifica con comprobante o referencia escrita. Las reservas PENDIENTE_PAGO vencen a las 24 horas y se liberan automaticamente, con estado RESERVA_VENCIDA. Se revisan cada minuto y al iniciar el servicio. Los pedidos pagados no expiran automaticamente.

Productos permite unidad, peso neto, ingredientes, alergenos, conservacion y foto real. La foto elegida se reduce a 1600 pixeles y se comprime antes de guardar; no se inventaron fotos comerciales. Los archivos persisten en PostgreSQL, con limites y permisos. Para volumen elevado conviene almacenamiento de objetos, fuera de esta entrega.

## Verificacion y despliegue

Backend: suite PostgreSQL aislada, 48 pruebas aprobadas y una de demostracion omitida. Incluye privacidad de comprobantes, recuperacion de un solo uso, cierre de sesiones, cifrado, permisos, FEFO, liberacion de reservas y limites de devolucion. No se enviaron correos reales: falta API key y remitente del propietario.

Frontend: compilacion production, revision local de los seis roles y de la auditoria real de una operacion sintetica; movil de 390 pixeles sin desbordamiento. Estilos extraidos a CSS sin separar modulos: JavaScript inicial de 460 KB y total inicial aproximado 579 KB con las funciones nuevas. Se mantienen advertencias de presupuesto total y formato CommonJS de JsBarcode, cargado de forma diferida.

Desplegar backend actualizado primero, esperar V6 y Started VitnaApplication, despues frontend actualizado. Probar login con las claves existentes, configurar Brevo y probar recuperacion desde un buzón real. Publicacion en GitHub no confirma por si sola la aplicacion de la migracion ni la entrega de correos en Railway/Vercel.
