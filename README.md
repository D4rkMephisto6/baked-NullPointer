# NullPointer POS — Backend Sprint 1

**Universidad Peruana Unión (UPeU) · Célula G5 · Wendy's Food · 2026**

**Responsable backend:** Emanuel Janampa  
**Proyecto:** NullPointer POS – Sistema de Gestión de Pedidos y Caja  
**Objetivo del Sprint 1:** gestionar los accesos y la toma de comandas en mesa desde dispositivos móviles.

## 1. ¿Qué incluye este ZIP?

- Backend REST en **Java 21 + Spring Boot 3.5.16**.
- PostgreSQL con los esquemas `auth_schema` y `pos_schema`, incluyendo el modelo de datos previsto para los tres sprints.
- Autenticación por PIN con BCrypt y tokens JWT.
- Validación de roles (`MOZO`, `ADMINISTRADOR`, `COCINERO`, `CAJERO`, `ALMACENERO`).
- Mesas y su estado libre/ocupada.
- Catálogo de categorías y productos.
- Comandas en borrador, observaciones por plato, modificación de cantidades, transferencia de mesa y envío a cocina.
- Evento WebSocket/STOMP en `/topic/cocina` al enviar una comanda.
- Administración de cuentas: crear, listar, desactivar y reactivar usuarios.
- Migración automática de base de datos con Flyway guardada dentro de `backend-java/src/main/resources/db/migration/`; no tienes que abrir ni ejecutar SQL manualmente.
- `docker-compose.yml`, colección y entorno Postman, prueba unitaria del servicio JWT y guía de evidencias.

**Alcance:** el esquema SQL reserva tablas para los sprints 2 y 3, pero esta API implementa las funcionalidades backend del Sprint 1. Caja, cobros, recetas, inventario, facturación y reportes aún no tienen sus endpoints funcionales en esta entrega.

## 2. Requisito mínimo

Instala e inicia **Docker Desktop**. Para probar por HTTP también necesitas Postman. Con el modo Docker no hace falta instalar PostgreSQL, Java o Maven en Windows: Docker construye el backend y ejecuta PostgreSQL en contenedores.

## 3. Arranque en Windows

1. Descomprime todo el ZIP en una carpeta, por ejemplo `C:\Proyectos\NullPointer_POS_Sprint1`.
2. Abre Docker Desktop y espera a que indique que está funcionando.
3. Haz doble clic en `scripts\iniciar-en-windows.bat`, o abre una terminal dentro de la carpeta y ejecuta:

   ```powershell
   docker compose up --build -d
   ```

Al iniciar, Docker levanta PostgreSQL y construye la API. **Spring Boot ejecuta automáticamente la migración Flyway incluida dentro del backend**; crea los esquemas, tablas, relaciones, índices, funciones, triggers y datos de catálogo. No necesitas abrir pgAdmin ni ejecutar otro archivo `.sql`. La migración se ejecuta una sola vez por base de datos y Flyway registra su versión en `flyway_schema_history`.

4. Comprueba que los contenedores estén arriba:

   ```powershell
   docker compose ps
   docker compose logs -f backend
   ```

5. Abre `http://localhost:8080/api/v1/health`. La respuesta esperada es similar a:

   ```json
   {
     "status": "UP",
     "application": "NullPointer POS API",
     "sprint": 1
   }
   ```

La primera compilación necesita conexión a Internet para descargar las imágenes de Docker y las dependencias Maven. Espera a que el backend termine de iniciar antes de ejecutar las pruebas Postman.

## 4. Usuarios locales de demostración

El backend crea estos usuarios si no existen, con el PIN guardado mediante BCrypt:

| Código | Rol | PIN local |
|---|---|---|
| `ADM001` | ADMINISTRADOR | `1234` |
| `MOZ001` | MOZO | `1234` |
| `MOZ002` | MOZO | `1234` |
| `COC001` | COCINERO | `1234` |
| `CAJ001` | CAJERO | `1234` |

Son credenciales **solo para pruebas locales**; no deben usarse en un restaurante real ni en un despliegue expuesto a Internet. Cambia los PIN y el secreto JWT antes de un despliegue real.

## 5. Probar con Postman

Importa ambos archivos desde `postman/`:

- `NullPointer_POS_Sprint1.postman_collection.json`
- `NullPointer_POS_Local.postman_environment.json`

Selecciona el entorno **NullPointer POS - Local Docker** y ejecuta las solicitudes en el orden numerado `00` a `16`. La colección guarda tokens, IDs de mesa, ID del producto, ID de la comanda y el ID del usuario de prueba automáticamente.

Para repetir todo el flujo desde cero (se eliminan datos locales, incluidos los pedidos de prueba), ejecuta `scripts\restablecer-base-local.bat`, o desde la terminal:

```powershell
docker compose down -v
docker compose up --build -d
```

**Advertencia:** `docker compose down -v` elimina el volumen de PostgreSQL de esta instalación de desarrollo.

## 6. Endpoints del Sprint 1

| HU | Método y endpoint | Para qué sirve |
|---|---|---|
| HU-01 | `POST /api/v1/auth/login` | Validar el código y PIN; devuelve JWT. |
| HU-02 | `GET /api/v1/mesas` | Consultar estado y capacidad de las mesas. |
| HU-03 | `GET /api/v1/menu/categorias` | Listar categorías de menú. |
| HU-03 | `GET /api/v1/menu/productos?category=Pollos` | Buscar/filtrar productos del catálogo. |
| HU-04 | `POST /api/v1/pedidos` | Crear comanda en borrador con notas por plato. |
| HU-05 | `PUT /api/v1/pedidos/{id}/detalles` | Reemplazar los platos de una comanda en borrador y recalcular el total. |
| HU-06 | `PUT /api/v1/mesas/transferir` | Transferir una comanda activa a una mesa libre. |
| HU-07 | `POST /api/v1/pedidos/{id}/enviar-cocina` | Enviar comanda y emitir evento WebSocket. |
| HU-07 | `GET /api/v1/cocina/pedidos` | Consultar comandas enviadas/activas en cocina (rol cocinero/admin). |
| HU-26 | `GET /api/v1/admin/usuarios` | Listar usuarios (solo administrador). |
| HU-26 | `POST /api/v1/admin/usuarios` | Crear cuenta y asignar rol. |
| HU-26 | `PATCH /api/v1/admin/usuarios/{id}/desactivar` | Desactivar cuenta. |
| HU-26 | `PATCH /api/v1/admin/usuarios/{id}/activar` | Reactivar cuenta. |

Tras iniciar sesión, envía el token en cada endpoint protegido: `Authorization: Bearer <token>`. Los usuarios desactivados se bloquean aunque intenten reutilizar un JWT emitido antes de la desactivación.

### Ejemplo de login

`POST http://localhost:8080/api/v1/auth/login`

```json
{
  "employeeCode": "MOZ001",
  "pin": "1234"
}
```

### Ejemplo de creación de comanda

`POST http://localhost:8080/api/v1/pedidos` (requiere JWT de MOZO o ADMINISTRADOR).

```json
{
  "tableId": 1,
  "notes": "Comanda de prueba",
  "items": [
    { "productId": 1, "quantity": 2, "notes": "Sin mayonesa" }
  ]
}
```

Los IDs reales se obtienen de `GET /api/v1/mesas` y `GET /api/v1/menu/productos`; los números del ejemplo son ilustrativos. El backend consulta los precios actuales en la base de datos y calcula el total en el servidor.

## 7. WebSocket de cocina

- Handshake: `ws://localhost:8080/ws/kitchen`
- Suscripción STOMP: `/topic/cocina`
- En el handshake incluye `Authorization: Bearer <JWT de MOZO o ADMINISTRADOR>`.
- Al enviar una comanda, el backend publica `ORDER_SENT_TO_KITCHEN` y registra una fila en `pos_schema.outbox_events` dentro de la transacción de pedido.

El broker STOMP simple incluido es para desarrollo local y demostración del Sprint 1. Para producción habría que diseñar persistencia/recuperación de eventos, autorización de destinos STOMP y estrategia de reconexión.

## 8. Integración desde Kotlin / Compose Multiplatform

La app móvil consume el backend a través de HTTP/JSON, no accede directamente a PostgreSQL. Base URL según entorno:

- Postman o navegador en la PC: `http://localhost:8080`
- Emulador Android de Android Studio: `http://10.0.2.2:8080`
- Dispositivo físico: `http://IP_LOCAL_DE_LA_PC:8080` (ambos deben estar en la misma red y el firewall debe permitir el puerto).

Hay un pequeño ejemplo Ktor en `kotlin-sample/`. No es la aplicación Compose completa; sirve como referencia para la futura conexión de Randy con el endpoint de login.

## 9. Estructura de carpetas

```text
NullPointer_POS_Sprint1/
├── backend-java/        # Código Java/Spring Boot, prueba JWT y migración Flyway embebida
├── postman/             # Colección de pruebas y entorno local
├── docs/                # Arquitectura, cobertura/evidencia y registro
├── kotlin-sample/       # Ejemplo mínimo de consumo HTTP desde Ktor
├── docker-compose.yml   # PostgreSQL + backend
└── scripts/             # Inicio y reinicio de entorno Windows
```

## 10. Problemas comunes

- **Puerto 5432 ocupado:** detén el PostgreSQL local u otro contenedor, o modifica el lado izquierdo de `"5432:5432"` en `docker-compose.yml`.
- **Puerto 8080 ocupado:** cambia el lado izquierdo de `"8080:8080"`; recuerda cambiar `baseUrl` en Postman.
- **El backend no encuentra tablas o falla Flyway:** ejecuta `docker compose logs backend` y busca el primer error de migración. La estructura se crea desde `backend-java/src/main/resources/db/migration/V1__create_nullpointer_pos_schema.sql`. En una base existente, Flyway la ejecuta automáticamente solo si no está registrada en `flyway_schema_history`. Para reiniciar la BD de demo usa el script de restablecimiento, que borra los datos y el historial de migraciones al eliminar el volumen.
- **401:** vuelve a ejecutar la solicitud de login y confirma que Postman haya guardado la variable de token.
- **409 mesa ocupada:** usa una mesa libre o reinicia la base de demo. El backend impide abrir una segunda comanda activa para una mesa.

## Nota sobre verificación

Los archivos de código, configuración Docker, migración Flyway dentro del backend y colección Postman están incluidos. No hay un SQL independiente que debas ejecutar manualmente. Este paquete se preparó en un entorno que no tiene Docker ni Maven instalados, por lo que **no pude ejecutar aquí la compilación ni la colección contra una instancia real**. Usa las respuestas de Postman como evidencia después de ejecutar el proyecto; no se han inventado capturas ni resultados de ejecución.
