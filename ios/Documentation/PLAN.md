# Plan de la versión iOS

## 1. Acordar el entorno

El proyecto de Xcode usa SwiftUI, iOS 17 como mínimo y el identificador de desarrollo `mx.com.gourmeet.ios`. Queda por confirmar la cuenta de desarrollo y la firma para distribución.

## 2. Cerrar el contrato con el servidor

Tomar el inventario de ApiService como punto de partida y contrastarlo con el PHP. Confirmar campos obligatorios, tipos, errores, sesión y permisos por recurso. No trasladar la confianza en un CLI_ID enviado por el teléfono como mecanismo de autenticación.

Preparar servidor de pruebas por HTTPS, con datos ficticios y configuración separada de producción. Documentar la URL y el acceso privado; no incorporar la dirección del Mac ni secretos al código iOS.

## 3. Empezar por lectura de recetas

La app ya tiene navegación, listado, búsqueda por nombre y detalle, con modo demostración y conexión al laboratorio. Queda por validar la interfaz en el simulador y en un iPhone.

## 4. Incorporar la cuenta y los datos personales

Una vez definido el contrato de identidad, implementar acceso, sesión y cierre. Añadir alacena, favoritos y colecciones con persistencia verificada. Distinguir cuenta invitada, inventario vacío y error de conexión.

## 5. Incorporar funciones del hogar

Agregar hogar, invitaciones, planeador y lista de compras con el esquema oficial. Confirmar qué acciones están permitidas para cada miembro y probar cambios entre usuarios.

## 6. Resolver integraciones de la plataforma

Compartir como texto ya está en el detalle de la receta. Queda por definir calendario, permisos y compras para iOS. El guardado de un evento en el calendario no implica sincronización con el planeador del backend. Acordar ese comportamiento antes de implementarlo.

## 7. Validar antes de distribuir

Probar en simuladores y un iPhone físico, con distintos tamaños de texto, teclado y estados de red. Verificar recuperación de sesión, persistencia, permisos y ausencia de secretos en el paquete. Preparar la distribución de pruebas cuando el equipo acuerde iniciarla.
