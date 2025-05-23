# Backend SPA - Cumplimiento Seguridad - Opción 2

Este proyecto actúa como backend de una aplicación SPA Angular, integrándose con un middleware de autenticación para gestionar tokens JWT y validación de sesión, según los requisitos de la POC (opción 2).

---

## 🧩 Funcionalidad General

- Recepción de la cookie temporal `Sessiontmp` y redirección segura.
- Comunicación con el middleware para generar tokens JWT y cookies protegidas.
- Validación de tokens en cabecera `Authorization` protegidos con cookie fingerprint.
- Refresco y revocación de sesión.
- Exposición de endpoints de negocio protegidos.

---

## 📌 Endpoints Implementados

| Endpoint            | Descripción                                                                 |
|---------------------|------------------------------------------------------------------------------|
| `/login2EndSPA`     | Llama al middleware `/loginEnd` y devuelve cookies y cabecera Authorization |
| `/refrescoSPA`      | Llama a `/refresco` en el middleware para renovar el token de acceso        |
| `/logoffSPA`        | Borra cookies y cabeceras, y llama a `/logoff` del middleware                |
| `/obtenerclaimsSPA` | Extrae y devuelve los claims desde el middleware                            |
| `/estadoSPA`        | Comprueba si la sesión está activa                                           |

---

## 🔐 Seguridad

- **Cookies**: `Session-{idsession}`, `Proteccion-{idsession}` (`HttpOnly`, `Secure`, `SameSite=Strict`)
- **Token de Acceso**: JWT cifrado entregado en la cabecera `Authorization`
- **Cabeceras personalizadas**:
  - `X-Cert-Auth`: certificado del cliente
  - `X-Idsession`: ID de sesión
- **Filtros personalizados**:
  - Validación de fingerprint (`JwtCookieAndTokenAuthenticationFilter`)
  - Autenticación basada en token descifrado

---

## ✅ Cumplimiento POC

Este backend cumple con:

- El flujo completo de autenticación en dos pasos
- Recepción y entrega segura de tokens
- Lógica de refresco y logoff según la opción 2
- Seguridad en cookies, cabeceras y validación

---

## 🧱 Arquitectura Técnica

- **Lenguaje**: Java 17
- **Framework**: Spring Boot
- **Seguridad**: Spring Security + filtros personalizados
- **JWT**: gestionado por middleware, validado indirectamente
- **Encriptación**: token JWT ya cifrado desde middleware

---

## 📂 Organización del Código

- `controller.middleware`: endpoints internos `/login2EndSPA`, `/logoffSPA`, etc.
- `controller.bank`: endpoints de negocio simulados (`/cuentas`, `/prestamos`, etc.)
- `filter`: validación y autenticación basada en token
- `exception`: manejo de errores
- `util`, `config`, `constants`: clases auxiliares

---

## 🧪 Pruebas

- Requiere pruebas funcionales con Postman o curl