# Desarrollo y pruebas de GourMeet

Actualizado el 9 de noviembre de 2026. Rama `anmiel/local-adaptive-ui`, con los cambios de `origin/master` hasta `9ddbd28`. El backend se trabaja en `anmiel/local-environment` del repositorio gourmeet-web.

## Entorno

El laboratorio de macOS está en `/Volumes/SSD-Externo/GourmeetLab`. Incluye JDK21, SDK Android, Gradle, emuladores, PHP8.2 y MariaDB13. No necesita Windows ni una extensión de VS Code para ejecutar los emuladores.

PHP y MariaDB se ejecutan en localhost8081 y3307. Los esquemas y datos ficticios están en `web/dev`. La tabla de cocineros sigue siendo provisional. La lista de compras necesita las migraciones oficiales que faltan en el dump aportado. No importar las fixtures en producción.

## Emulador

```sh
source /Volumes/SSD-Externo/GourmeetLab/env.sh
bash gradlew :app:assembleDebug :app:assembleDebugAndroidTest \
  -Pgourmeet.apiBaseUrl=http://10.0.2.2:8081/api/
```

La URL debug es configurable. Release conserva el destino de Fran, actualmente192.168.1.116:3001. No apuntar pruebas destructivas a ese servidor.

## Pixel físico por USB

1. Activar Opciones para desarrolladores tocando siete veces Número de compilación en Información del teléfono.
2. Activar Depuración USB, conectar el Pixel al Mac con un cable de datos y autorizar el equipo en el teléfono desbloqueado.
3. Mantener PHP/MariaDB del laboratorio encendidos.
4. Compilar e instalar la variante pixel:

```sh
source /Volumes/SSD-Externo/GourmeetLab/env.sh
bash gradlew :app:assemblePixel
adb -d reverse tcp:8081 tcp:8081
adb -d install -r app/build/outputs/apk/pixel/app-pixel.apk
adb -d shell am start -n mx.com.gourmeet.app.local/mx.com.gourmeet.app.MainActivity
```

En el laboratorio también existen `scripts/build-pixel.sh` y `scripts/install-pixel.sh`.

La app se llama GourMeet Local y su applicationId es `mx.com.gourmeet.app.local`. Se instala junto a `mx.com.gourmeet.app`, sin sustituir su firma ni sus datos. No desinstalar la app original para resolver errores de instalación.

La API pixel es127.0.0.1:8081: se alcanza mediante adb reverse. Repetir el reverse tras reconectar o reiniciar el teléfono. El laboratorio necesita el Mac y el cable; no es una APK autónoma con servidor incluido.

`src/pixel/google-services.json` contiene identificadores sintéticos, no credenciales de Firebase. En pixel se desactiva la inicialización de Firebase y se avisa al pulsar Google. Usar el acceso por correo del laboratorio o continuar sin cuenta. No se valida Google/Premium con esta variante. La cuenta ficticia es local@example.invalid, contraseña GourmeetLocal-2026!; solo existe en las fixtures locales.

## Interfaz

El menú usa accesos fijos a Cocinar, Alacena, Tiendas y Perfil. Cocinar y Alacena son destinos explícitos; repetir un toque no alterna de sección. Se conservan categorías, búsqueda, filtros, detalles y acciones del inventario.

La paleta mantiene el azul con superficies neutras y texto oscuro. La tipografía principal usa sans-serif del sistema. Las tarjetas muestran primero fotografía y después título, estadísticas e ingredientes, con alturas por contenido. El inventario agrupa filas por categoría y destaca por separado los ingredientes para consumir primero. Los estados se muestran también mediante texto. El botón Agregar ingrediente identifica la acción.

Las fotos y nombres del catálogo ficticio son ejemplos de dimensiones y pueden no corresponder entre sí. Los fallbacks usan vectores. Esto no mejora por sí solo la resolución de las fotografías originales.

## Verificación

SafeDrawingTest comprueba área segura, padding no acumulativo, contraste, textos, tarjetas, invitado y navegación explícita. La suite, incluida la prueba del paquete, contiene10 pruebas. Para pixel:

```sh
bash gradlew :app:assemblePixel :app:assemblePixelAndroidTest -Pgourmeet.testBuildType=pixel
adb -d install -r app/build/outputs/apk/androidTest/pixel/app-pixel-androidTest.apk
adb -d shell am instrument -w mx.com.gourmeet.app.local.test/androidx.test.runner.AndroidJUnitRunner
```

Las pruebas crean una sesión ficticia exclusivamente en la app de laboratorio. No prueban una autenticación real. No ejecutarlas sobre la app de Play.

Se comprobó el Pixel10ProXL físico con Android17/API37, además de emulador API36. Evidencia y resultados por pasada en `GourmeetLab/evidence`; captura de pantalla real, no render de diseño.

Persisten pendientes del servidor: identidad y autorización de API, esquema definitivo, compras y migraciones. Lint tenía errores heredados documentados en los informes del laboratorio. Compilar y pasar estas comprobaciones no certifica todos los flujos de negocio.

Guía oficial de conexión: https://developer.android.com/studio/run/device/.
