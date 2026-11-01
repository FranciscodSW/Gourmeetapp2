# Laboratorio local y primera corrección de pantalla

Trabajo local en la rama `anmiel/local-adaptive-ui`, a partir de `4a11750`. Sin publicar cambios ni conectar la app de prueba al servidor de Fran.

## Compilar

El laboratorio está en `/Volumes/SSD-Externo/GourmeetLab`. Cargar su `env.sh` y ejecutar:

```sh
bash gradlew :app:assembleDebug :app:assembleDebugAndroidTest \
  -Pgourmeet.apiBaseUrl=http://10.0.2.2:8081/api/
```

La propiedad solo cambia el destino de debug. Release mantiene el destino que ya tenía el repositorio. No incluye acceso alternativo a Google ni cambios de autenticación. Las pruebas de interfaz preparan un usuario ficticio en las preferencias del emulador; eso es una fixture de pruebas y no una autenticación real de Firebase.

## Cambios

- `applySafeDrawingInsets` aplica barras del sistema y recorte de cámara al contenedor de contenido de siete Activities. Conserva los listeners propios de teclado y deriva siempre del padding original para no acumular márgenes.
- El encabezado del menú y de alacena usa 8 dp de espaciado interior en vez de asumir una barra de estado de 30 dp.
- Los botones iniciales de acceso pueden aumentar de altura con la tipografía del sistema.
- Cuatro recursos vectoriales resuelven referencias a iconos ausentes en el commit original. Son reemplazos locales que el equipo debe revisar visualmente.
- La prueba instrumentada de ejemplo actualiza el paquete esperado tras el cambio de applicationId.

## Verificación

`SafeDrawingTest` comprueba geometría real, redistribución idempotente de márgenes, renderizado y recorte de texto de los botones de acceso. Incluye acceso, registro, menú, alacena de presentación, alacena del hogar, hogar y planeador. Las llamadas usan exclusivamente la API del laboratorio y datos ficticios.

Perfiles: teléfono grande 1344x2992, teléfono compacto 1080x2340 (incluyendo ancho equivalente a 320 dp, fuente 130 % y navegación de tres botones), tableta horizontal 2560x1600. Todos con Android 16/API 36. El perfil grande usa la definición disponible Pixel 9 Pro XL, de resolución igual a la publicada para Pixel 10 Pro XL; no reproduce todo su hardware ni su recorte exacto.

Capturas, medidas y resultados completos en `GourmeetLab/evidence`. Existe una reproducción anterior al cambio del menú: root y=0 frente a un inset superior de 159 px. La corrección sitúa root y=159 en esa configuración.

## Límites y pendientes

Esta primera entrega resuelve áreas seguras y un recorte de texto, no rediseña todas las tarjetas ni convierte todas las pantallas en layouts adaptables. En tableta persiste un uso pobre del espacio horizontal; hay textos de tarjetas con medidas rígidas. Falta probar el Pixel físico, otros Android, teclado en todos los formularios, acceso real de Google y compras.

Lint detectó siete errores en archivos sin modificar: cinco MissingConstraints en layouts secundarios y dos UseAppTint en fragmentos. No se silenció lint ni se creó un baseline para ocultarlos. El detalle está en los logs del laboratorio. Compilar y pasar estas pruebas no equivale a una aprobación para producción.

## Segunda corrección: capturas aportadas por el usuario

Se reprodujo en modo oscuro del emulador la barra “Gourmeet2” encima del encabezado y texto blanco sobre el fondo claro de las secciones. La variante night heredaba DarkActionBar mientras el tema normal API23+ no lo hacía. Se unificó la paleta clara existente y NoActionBar para ambas configuraciones. Esto no implementa un diseño oscuro completo: evita mezclar superficies claras fijas con textos y barra oscuros automáticos.

La tarjeta compartida item_receta_new ahora separa título, imagen16:9, cuatro estadísticas de igual ancho y franja de ingredientes. La altura se adapta al texto. Los cuatro adapters que usan esa tarjeta ajustan el ancho al viewport real de su RecyclerView (incluidos diálogos), con límite de320dp; se ajustaron los contenedores de resultados que imponían350/365dp. Se quitaron márgenes negativos de ingredientes y se sustituyó el porcentaje fijo35% por el dato disponible o “--”.

La alacena de invitados muestra una explicación y acceso a iniciar sesión. No se inventa una sesión ni se permite guardar sin cuenta. El estado se valida en una nueva prueba de UI, además de contrastes, ausencia de ActionBar, títulos largos y estadísticas sin recortes.

El catálogo de laboratorio incluye nombres largos y más recetas mediante projects/web/dev/seed_recipe_list.sql. Las imágenes locales son ejemplos del repositorio, no una correspondencia culinaria validada con los nombres de las fixtures. Su URL solo se redirige al host local cuando GOURMEET_LOCAL=1.

Los textos del encabezado principal y botón central usan autosize con límites y padding para mantenerse dentro de su espacio en el perfil320dp con fuente130%. Verificación nueva en SafeDrawingTest.
