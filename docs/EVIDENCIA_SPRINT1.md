# Evidencia técnica reproducible — Sprint 1

**Proyecto:** NullPointer POS — Sistema de Gestión de Pedidos y Caja  
**Organización:** Universidad Peruana Unión (UPeU) — Célula G5  
**Solución:** Backend REST Java/Spring Boot para POS móvil y KDS de Wendy's Food  
**Objetivo del Sprint 1:** gestionar acceso y toma de comandas en mesa desde dispositivos móviles.

## 1. Archivos que permiten reproducir la evidencia

| Artefacto | Ubicación | Evidencia que puede generar el equipo |
|---|---|---|
| Aplicación backend | `backend-java/src/main/java/` | Código de controladores, validaciones, servicios, JWT, permisos y persistencia. |
| Migración Flyway | `backend-java/src/main/resources/db/migration/V1__create_nullpointer_pos_schema.sql` | Esquemas, tablas, relaciones, índices, restricciones, funciones, triggers y datos iniciales; Spring Boot la ejecuta automáticamente. |
| Docker Compose | `docker-compose.yml` | Levantamiento coordinado de PostgreSQL y backend; no necesita montar ni ejecutar un SQL externo. |
| Colección de Postman | `postman/NullPointer_POS_Sprint1.postman_collection.json` | Códigos HTTP y pruebas automáticas para HU-01 a HU-07 y HU-26. |
| Entorno Postman | `postman/NullPointer_POS_Local.postman_environment.json` | URL local y variables de tokens e IDs. |
| Prueba automatizada JWT | `backend-java/src/test/java/pe/upeu/nullpointerpos/JwtServiceTest.java` | Comprobación de generación/firma y lectura de claims. |

## 2. Secuencia sugerida para las capturas reales

No se adjuntan capturas falsas: el contenedor de creación no dispone de Docker ni Maven y la API no fue arrancada aquí. Después de ejecutar `docker compose up --build -d`, genera capturas de estas pruebas reales de Postman:

| Evidencia | Solicitud | Resultado esperado al ejecutar |
|---|---|---|
| EV-01 | `00 - Health sin token` | HTTP 200 y `status: UP`. |
| EV-02 | `01 - Login MOZO` | HTTP 200, rol MOZO y JWT. |
| EV-03 | `02 - Consultar mesas` | HTTP 200; mesas M01–M12 y estados. |
| EV-04 | `05/06 - Menú` | HTTP 200; categorías y productos por Pollos. |
| EV-05 | `07 - Crear comanda` | HTTP 201, estado BORRADOR, nota por plato y total calculado. |
| EV-06 | `09 - Editar detalles` | HTTP 200, nueva cantidad, nota actualizada y total recalculado. |
| EV-07 | `10 - Transferir mesa` | HTTP 200, mesa origen liberada y mesa destino ocupada. |
| EV-08 | `11 - Enviar cocina` | HTTP 200, estado ENVIADO_COCINA y marca de tiempo. |
| EV-09 | `12 - Cola de cocina` | HTTP 200, aparece la comanda enviada. |
| EV-10 | `14/15 - Administración de usuarios` | Alta HTTP 201 y luego cuenta activa=false. |
| EV-11 | `16 - Login incorrecto` | HTTP 401; no entrega token. |

**Recomendación:** utiliza el runner de Postman y exporta el resultado de la ejecución o toma capturas individuales. Incluye una captura de `docker compose ps` y otra de los logs cuando el backend haya iniciado satisfactoriamente.

## 3. Relación con el desglose técnico

| Grupo de tarea técnica | Implementación incluida |
|---|---|
| HT-01 — Arquitectura/base de datos | `pom.xml`, estructura de paquetes, `application.yml`, HikariCP y migración Flyway integrada en el backend. La app Compose/KMP no se incluye en este ZIP de backend. |
| HT-02 — Seguridad JWT | `JwtService`, `JwtAuthenticationFilter`, `SecurityConfig`, BCrypt y `AuthService`. |
| HU-01 — Login | `AuthController`, `AuthService`, `LoginRequest`. |
| HU-02 — Mesas | `TableController`, `TableService`. |
| HU-03 — Productos/categorías | `MenuController`, `MenuService`. |
| HU-04/HU-05 — Comanda | `OrderController`, `OrderService`, DTO de líneas, cantidades y notas. |
| HU-06 — Mover mesa | `TableTransferController`, transacción y `table_transfers`. |
| HU-07 — Envío cocina | `OrderController`, `WebSocketConfig`, `outbox_events` y `/topic/cocina`. |
| HU-26 — Usuarios | `UserController`, `UserService`, filtros por rol y logs de auditoría. |

## 4. Registro del proyecto que proporcionó el equipo

El registro siguiente conserva el calendario y las actividades comunicadas por el equipo; no representa una medición automática de ejecución del ZIP.

| Día | Fecha | Colaborador | Rol | Hora | Actividad registrada | Tarea/HU |
|---|---|---|---|---|---|---|
| Día 1 | 23/10/2026 | Emanuel Janampa | Backend / Scrum Master | 5:00 PM | Configuración inicial del repositorio KMP y estructura base del servidor Spring Boot. | T-01 |
| Día 1 | 23/10/2026 | Daniel Mamani | Product Owner / Analista | 5:15 PM | Revisión y validación inicial de requerimientos con Wendy's Food. | HU-01 |
| Día 1 | 23/10/2026 | Randy Huamán | Developer / QA | 5:30 PM | Instalación y configuración del entorno Compose Multiplatform. | T-02 |
| Día 2 | 25/10/2026 | Emanuel Janampa | Backend / Scrum Master | 10:00 PM | Creación inicial de esquemas y tablas de autenticación en PostgreSQL. | T-03 |
| Día 2 | 25/10/2026 | Daniel Mamani | Product Owner / Analista | 10:15 PM | Elaboración de bocetos y mockups para Login y vistas móviles. | HU-01 |
| Día 2 | 25/10/2026 | Randy Huamán | Developer / QA | 10:30 PM | Maquetación visual preliminar de la interfaz de inicio de sesión. | T-08 |
| Día 3 | 26/10/2026 | Emanuel Janampa | Backend / Scrum Master | 9:45 PM | Desarrollo preliminar de controladores REST y seguridad en Spring Boot. | T-10 |
| Día 3 | 26/10/2026 | Daniel Mamani | Product Owner / Analista | 10:00 PM | Análisis de campos para selección de platos y notas. | HU-04 |
| Día 3 | 26/10/2026 | Randy Huamán | Developer / QA | 10:15 PM | Estructuración local de vistas en la app móvil. | T-09 |
| Día 4 | 27/10/2026 | Emanuel Janampa | Backend / Scrum Master | 10:00 PM | Pruebas de endpoints del backend de forma aislada usando Postman. | T-12 |
| Día 4 | 27/10/2026 | Daniel Mamani | Product Owner / Analista | 10:15 PM | Evaluación de distribución visual del salón y mesas. | HU-02 |
| Día 4 | 27/10/2026 | Randy Huamán | Developer / QA | 10:30 PM | Pruebas de diseño responsivo para el plano de mesas. | T-11 |
| Día 5 | 28/10/2026 | Emanuel Janampa | Backend / Scrum Master | 10:00 PM | Definición de rutas y lógica para la recepción de pedidos. | T-28 |
| Día 5 | 28/10/2026 | Daniel Mamani | Product Owner / Analista | 10:15 PM | Seguimiento y organización de tareas pendientes para cierre de semana. | HU-07 |
| Día 5 | 28/10/2026 | Randy Huamán | Developer / QA | 10:30 PM | Verificación de componentes visuales; aún sin integración con API. | T-25 |

**Importante:** antes de entregar esta evidencia al docente, ejecuten las pruebas, guarden los resultados reales y actualicen los estados según lo que efectivamente pase en su equipo.
