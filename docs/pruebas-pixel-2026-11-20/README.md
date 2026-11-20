# Pruebas en Pixel · 20 de noviembre de 2026

Probé la versión local en un Pixel 10 Pro XL con Android 17, conectado por Wi-Fi a PHP y MariaDB del laboratorio.

| Configuración | Resultado |
| --- | --- |
| Modo claro, letra al 130 % | 20 de 20 pruebas correctas |
| Modo oscuro, letra al 130 % | 20 de 20 pruebas correctas |
| Diálogos de colecciones y reportes después del ajuste visual | 2 de 2 en claro y 2 de 2 en oscuro |
| Formulario de reporte después del ajuste de campos | 1 de 1 en oscuro |
| API local existente | 8 de 8 pruebas correctas |

Las pruebas incluyen áreas seguras en vertical y horizontal, teclado, navegación de alacena, recomendaciones desde el inventario, detalle, favoritos, colecciones, compartir, calendario, productos de proveedores y envío de reportes. La prueba de voz comprueba que la lectura pause el reconocimiento y que una finalización anterior no reactive la escucha. La pantalla de acceso se comprueba por su disposición, sin capturar su ventana en las repeticiones finales.

## Evidencia

- [Inicio claro](Pixel-inicio-claro.png)
- [Inicio oscuro](Pixel-inicio-oscuro.png)
- [Menú horizontal](Pixel-menu-horizontal.png)
- [Búsqueda y teclado](Pixel-busqueda-teclado.png)
- [Productos y precios](Pixel-productos-precios.png)
- [Perfil del proveedor](Pixel-perfil-proveedor.png)
- [Reporte del proveedor](Pixel-reporte-proveedor.png)
- [Detalle de receta](Pixel-detalle-receta.png)
- [Resultado en claro](Pruebas-claro.txt)
- [Resultado en oscuro](Pruebas-oscuro.txt)

## Cambios

El inicio prioriza coincidencias de la alacena y solicita selección aleatoria al servidor para las ideas generales. Los productos cercanos abren el perfil de su proveedor y muestran el precio en MXN y su presentación cuando están publicados. Corregí el agrupado que ocultaba categorías de productos y adapté la presentación del perfil al ancho disponible.

La web permite publicar precios y consultar reportes con una sesión de administrador de nivel 3. La migración está en `dev/004_supplier_prices_reports.sql` de la rama `anmiel/local-environment` de gourmeet-web. Debe aplicarse en el entorno del equipo antes de probar esta función allí. Los reportes pueden enviarse sin datos de cuenta y quedan como avisos que requiere revisar el administrador.

Los proveedores, precios y coordenadas usados en la prueba son ficticios. La ubicación 0,0 se inyectó al método de recomendaciones durante la prueba, sin cambiar la ubicación del teléfono. Falta contrastar los resultados con el catálogo de la oficina y confirmar el alcance de 10 km de las recomendaciones cercanas.

Restauré el tamaño de letra y el modo de color previos del teléfono al terminar. La app instalada es GourMeet Local, con identificador `mx.com.gourmeet.app.local`.
