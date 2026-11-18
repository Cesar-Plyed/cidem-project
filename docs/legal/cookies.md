# Política de cookies

**Última actualización:** 2026-11-18
**Ruta en la aplicación:** `/legal/cookies`

CIDEM Integrity no utiliza cookies publicitarias, de analítica ni de perfilado.
Solo emplea almacenamiento local estrictamente necesario para mantener la sesión
autenticada.

## 1. Qué son las cookies y el almacenamiento local

Las cookies son pequeños archivos que un sitio web guarda en el navegador. El
almacenamiento local (localStorage e IndexedDB) cumple una función similar, pero
no se envía automáticamente al servidor en cada petición.

## 2. Cookies que utilizamos

Ninguna. La plataforma no instala cookies propias ni de terceros, no usa Google
Analytics ni herramientas equivalentes y no comparte identificadores con redes
publicitarias.

## 3. Almacenamiento local estrictamente necesario

| Clave | Finalidad | Duración |
|---|---|---|
| `auth_access_token` | Token de acceso JWT que identifica la sesión. | 15 minutos; se renueva con la sesión activa. |
| `auth_refresh_token` | Renovar el acceso sin volver a introducir la contraseña. | 7 días o hasta cerrar sesión. |
| Ionic Storage | Réplica de los tokens en IndexedDB (web) o almacenamiento nativo (escritorio). | Igual que el token correspondiente. |

## 4. Base legal

Este almacenamiento está exento del deber de consentimiento previo porque es
técnicamente imprescindible para prestar el servicio solicitado (artículo 22.2 de
la LSSI-CE). Por ese motivo no mostramos un banner de cookies: no hay cookies
opcionales que aceptar.

## 5. Cómo eliminarlo

Puedes borrar estos datos desde la configuración de tu navegador (Privacidad →
Borrar datos de navegación). Si lo haces, se cerrará la sesión y tendrás que
iniciar sesión de nuevo. En la aplicación de escritorio, cerrar sesión elimina
los tokens almacenados.

## 6. Recursos de terceros

Las tipografías, iconos y hojas de estilo se empaquetan con la propia aplicación;
no se cargan desde CDN externas. El backend solo se contacta con la API
configurada para la plataforma.

## 7. Actualizaciones

Si en el futuro incorporamos cookies opcionales, actualizaremos esta política y
solicitaremos el consentimiento previo antes de instalarlas.
