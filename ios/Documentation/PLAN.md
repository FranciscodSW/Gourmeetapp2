# Preparación de la versión iOS

## 1. Acordar el entorno

Confirmar versión mínima de iOS, Swift/SwiftUI como base, nombre e identificador, firma y cuenta de desarrollo. Crear el proyecto de Xcode dentro de ios cuando se apruebe empezar. Mantener los archivos personales de Xcode y las credenciales fuera de Git.

## 2. Cerrar el contrato con el servidor

Tomar el inventario de ApiService como punto de partida y contrastarlo con el PHP. Confirmar campos obligatorios, tipos, errores, sesión y permisos por recurso. No trasladar la confianza en un CLI_ID enviado por el teléfono como mecanismo de autenticación.

Preparar servidor de pruebas por HTTPS, con datos ficticios y configuración separada de producción. Documentar la URL y el acceso privado; no incorporar la dirección del Mac ni secretos al código iOS.

## 3. Empezar por lectura de recetas

Construir navegación, listado, búsqueda y detalle con cargas, errores y reintentos visibles. Usar la tipografía del sistema, tamaño de texto adaptable, áreas seguras y teclado. Contrastar el resultado con las pantallas Android y con los patrones propios de iOS.

## 4. Incorporar la cuenta y los datos personales

Una vez definido el contrato de identidad, implementar acceso, sesión y cierre. Añadir alacena, favoritos y colecciones con persistencia verificada. Distinguir cuenta invitada, inventario vacío y error de conexión.

## 5. Incorporar funciones del hogar

Agregar hogar, invitaciones, planeador y lista de compras con el esquema oficial. Confirmar qué acciones están permitidas para cada miembro y probar cambios entre usuarios.

## 6. Resolver integraciones de la plataforma

Definir compartir, calendario, permisos y compras para iOS. El guardado de un evento en el calendario no implica sincronización con el planeador del backend. Acordar ese comportamiento antes de implementarlo.

## 7. Validar antes de distribuir

Probar en simuladores y un iPhone físico, con distintos tamaños de texto, teclado y estados de red. Verificar recuperación de sesión, persistencia, permisos y ausencia de secretos en el paquete. Preparar la distribución de pruebas cuando el equipo acuerde iniciarla.

Esta lista define el orden de trabajo futuro. No implica que esas funciones estén implementadas ni que exista una compilación iOS.
