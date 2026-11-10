# GourMeet para iOS

Dejé esta estructura para preparar una futura versión de iOS sin mezclarla con el proyecto Android. Todavía no hay una aplicación ejecutable ni un proyecto de Xcode.

La idea inicial es trabajar con Swift y SwiftUI. Antes de empezar hay que acordar la versión mínima de iOS, el identificador de la aplicación, la firma y el acceso al servidor de pruebas. No fijé esas decisiones todavía.

## Organización

| Carpeta | Uso previsto |
| --- | --- |
| `GourMeet/App` | Inicio de la aplicación y navegación |
| `GourMeet/Core/Networking` | Cliente HTTP, configuración de entorno y errores |
| `GourMeet/Core/Session` | Estado de sesión y almacenamiento seguro de credenciales |
| `GourMeet/Core/DesignSystem` | Colores, tipografía y componentes compartidos por las pantallas iOS |
| `GourMeet/Features/Account` | Acceso, cuenta y perfil |
| `GourMeet/Features/Recipes` | Búsqueda, detalle, compartir y calendario |
| `GourMeet/Features/Pantry` | Alacena e ingredientes |
| `GourMeet/Features/Household` | Hogares, miembros e invitaciones |
| `GourMeet/Features/Planner` | Planeador semanal |
| `GourMeet/Features/ShoppingList` | Lista de compras |
| `GourMeet/Features/Collections` | Colecciones y favoritos |
| `GourMeet/Resources` | Recursos propios de iOS |
| `Tests` | Pruebas de contratos, reglas y navegación cuando exista implementación |
| `Documentation` | Plan e inventario de API |

Las carpetas vacías se conservan con `.gitkeep`. Los colores de `Core/DesignSystem/tokens.json` toman como referencia el diseño que dejé en Android. Los tamaños son referencias de diseño; habrá que adaptarlos a iOS y a sus opciones de accesibilidad.

## Qué se puede aprovechar

El backend PHP/MariaDB, los contratos de API y los datos ficticios pueden servir a las dos aplicaciones. Las vistas XML, las Activities y el código de interfaz Kotlin quedan en Android; no se copiaron como implementación de iOS.

`Documentation/api-inventory.json` enumera las rutas declaradas actualmente en ApiService, con sus parámetros y tipos Android de referencia. No es un SDK Swift, una especificación OpenAPI ni una garantía de que todas las rutas estén completas o protegidas.

## Antes de implementar

Hay que confirmar la autenticación y autorización del backend, el esquema definitivo y una URL HTTPS de pruebas. La colección de datos del laboratorio no es el catálogo real. La tabla provisional de cocineros y las migraciones faltantes de compras siguen pendientes de revisión con Fran.

La integración con el calendario y las compras de iOS se resolverá para esa plataforma. El calendario del teléfono y el planeador semanal del servidor son funciones distintas; hay que definir si se conectarán y cómo.

El orden propuesto está en `Documentation/PLAN.md`. Esta preparación no cambia la compilación ni el comportamiento de Android.
