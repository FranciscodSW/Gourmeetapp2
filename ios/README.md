# GourMeet para iOS

La primera versión en SwiftUI incluye el catálogo de **Qué cocinar**, búsqueda por nombre y el detalle con ingredientes, porciones, preparación y la opción de compartir como texto. El modo demostración usa dos recetas de ejemplo y funciona sin conexión.

## Cómo probarla

1. Abre `GourMeet.xcodeproj` en Xcode y elige el esquema **GourMeet**.
2. Selecciona un iPhone simulado con iOS 17 o posterior y pulsa **Run**. La app abre en modo demostración.
3. Para usar el laboratorio, arranca PHP y MariaDB en el Mac. En **Product → Scheme → Edit Scheme → Run → Arguments**, habilita `GOURMEET_API_URL`. El valor del esquema apunta a `http://127.0.0.1:8081/api/` para el simulador.
4. En un iPhone físico, selecciona tu equipo de firma en **Signing & Capabilities**. Para usar el servidor desde el teléfono, pon la URL HTTPS de pruebas, con el prefijo `/api/`, en la misma variable y ejecuta desde Xcode.

El identificador de desarrollo es `mx.com.gourmeet.ios`. El servidor del laboratorio usa datos ficticios. Si la API falla, la app muestra un error con la opción de reintentar.

## Siguiente etapa

Cuenta, favoritos sincronizados, alacena, lista de compras y planeador requieren conectar los flujos autenticados. El orden está en [PLAN.md](Documentation/PLAN.md). El [inventario de API](Documentation/api-inventory.json) recoge las rutas de Android que se pueden revisar para esa integración.
