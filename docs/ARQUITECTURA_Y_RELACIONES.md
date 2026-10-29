# Arquitectura del backend Sprint 1

## Inicialización automática de PostgreSQL

La estructura de datos está incluida como migración Flyway en `backend-java/src/main/resources/db/migration/V1__create_nullpointer_pos_schema.sql`. Al levantar el proyecto con Docker Compose, Spring Boot conecta con PostgreSQL y ejecuta esta migración automáticamente; no hay que abrir pgAdmin ni ejecutar scripts SQL de manera manual. Flyway registra la versión aplicada en `flyway_schema_history`.

## Contexto

- Organización: Universidad Peruana Unión (UPeU), Célula G5.
- Cliente: Wendy's Food.
- Software: NullPointer POS — Sistema de Gestión de Pedidos y Caja.
- Backend: Java 21, Spring Boot, JDBC Template, Spring Security, JWT, BCrypt y WebSocket/STOMP.
- Base de datos: PostgreSQL, conexión mediante HikariCP.
- Pruebas: Postman; empaquetado local mediante Docker Compose.

## Flujo principal de negocio

1. El mozo inicia sesión con `employeeCode` y un PIN de 4 a 8 dígitos.
2. El backend compara el PIN con su hash BCrypt y responde con un JWT temporal.
3. El cliente consulta las mesas, categorías y productos con el token.
4. El mozo crea una comanda `BORRADOR` para una mesa libre. La cabecera se guarda en `pos_schema.orders` y cada plato en `pos_schema.order_items`.
5. El backend toma los precios desde `pos_schema.products`, conserva el nombre y precio en la línea de comanda, y calcula los totales en servidor.
6. Antes de enviar, puede sustituir los detalles para ajustar cantidades y observaciones; una comanda enviada ya no puede editarse con ese endpoint.
7. Una transferencia valida que la mesa destino esté libre y guarda su historial en `pos_schema.table_transfers`.
8. En el envío a cocina, el backend cambia el estado a `ENVIADO_COCINA`, actualiza `sent_at`, registra evento/outbox y publica el pedido en `/topic/cocina`.
9. La base de datos registra cambios de estado mediante triggers definidos en el SQL.

## Relaciones importantes

- `auth_schema.roles` 1:N `auth_schema.users`.
- `auth_schema.roles` N:M `auth_schema.permissions`, resuelta mediante `auth_schema.role_permissions`.
- `pos_schema.dining_tables` 1:N `pos_schema.orders` a través del tiempo (solo una comanda activa por mesa, controlada mediante un índice parcial único).
- `auth_schema.users` 1:N `pos_schema.orders` como mozo responsable.
- `pos_schema.orders` 1:N `pos_schema.order_items`.
- `pos_schema.products` 1:N `pos_schema.order_items`.
- `pos_schema.product_categories` 1:N `pos_schema.products`.
- `pos_schema.orders` 1:N `pos_schema.order_status_history`.
- `pos_schema.orders` 1:N `pos_schema.table_transfers`.

## Contratos HTTP

Todas las respuestas usan JSON. Los errores incluyen `timestamp`, `status`, `error`, `message` y `path`. Códigos esperados habituales: 200, 201, 400, 401, 403, 404 y 409.

| Operación | Validación importante |
|---|---|
| Login | Cuenta activa y PIN BCrypt correcto. |
| Crear comanda | Mesa existente/libre, productos activos/disponibles, cantidades positivas. |
| Editar detalles | Solo propietario o administrador; estado `BORRADOR`; al menos un plato. |
| Transferir mesa | Comanda no pagada/anulada y destino libre. |
| Enviar a cocina | Comanda en `BORRADOR` y con al menos un plato. |
| Crear usuario | Solo administrador; código único, PIN hasheado, rol existente. |
| Desactivar usuario | Solo administrador; la siguiente llamada protegida con un JWT antiguo ya no pasa el filtro de cuenta activa. |

## Cobertura de historias

| Historia | Backend en esta entrega |
|---|---|
| HU-01 | `POST /api/v1/auth/login`; BCrypt, JWT, registro de eventos de login. |
| HU-02 | `GET /api/v1/mesas`; estado LIBRE/OCUPADA leído de PostgreSQL. |
| HU-03 | GET categorías y productos con filtro por categoría/búsqueda. |
| HU-04 | Campo `notes` por elemento de comanda, guardado en `order_items`. |
| HU-05 | Endpoint de reemplazo de detalles solo antes del envío; total recalculado. |
| HU-06 | `PUT /api/v1/mesas/transferir`; destino libre e historial de transferencia. |
| HU-07 | `POST /api/v1/pedidos/{id}/enviar-cocina`; cambio de estado, outbox y evento STOMP. |
| HU-26 | Endpoints administrativos de alta, listado, desactivación y reactivación de cuentas. |

## Fuera de alcance en esta entrega

Las tablas para Sprint 2 y 3 se incluyen en el modelo SQL consolidado, pero la API de cobros/caja, pantalla KDS con actualización de platos, recetas, compras, mermas, stock automático, comprobantes, facturación y reportes queda pendiente para sus sprints correspondientes.
