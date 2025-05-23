# Security Middleware - Cumplimiento POC - Opción 2

Este proyecto es un **middleware de autenticación y autorización** que implementa los requerimientos de seguridad definidos por las buenas prácticas para aplicaciones SPA (Angular) que acceden a backends REST, utilizando el modelo de protección con **token JWT en almacenamiento local y cookie de fingerprint**.

---

## 🧩 Funcionalidad General

- Autenticación en dos pasos mediante redirección desde el SSO.
- Generación y validación de tokens JWT: sesión, refresco y acceso.
- Token de acceso en `Authorization` protegido con cookie de fingerprint (`Proteccion-{idsession}`).
- Expiración y refresco automático de tokens.
- Endpoints REST protegidos, preparados para uso interno y futura migración a microservicios.
- Validación de certificados de cliente mediante cabecera `X-Cert-Auth`.

---

## 📌 Endpoints Implementados

| Endpoint            | Descripción                                                        |
|---------------------|--------------------------------------------------------------------|
| `/loginBegin`       | Genera cookie temporal `Sessiontmp` a partir de cookie SSO.        |
| `/loginEnd`         | Genera cookies `Session-{idsession}` y `Proteccion-{idsession}` y devuelve JWT de acceso cifrado en cabecera `Authorization`. |
| `/refresco`         | Regenera token de acceso si ha expirado (cabecera `Authorization`).|
| `/logoff`           | Invalida sesión, borra cookies y limpia cabeceras.                 |
| `/obtenerclaims`    | Devuelve los claims de los tokens actuales.                        |
| `/estadosession`    | Verifica si la sesión está activa.                                 |
| `/validartoken`     | Valida un token JWT de acceso (uso opcional).                      |

---

## 🔐 Seguridad

- **Cookies seguras**: `HttpOnly`, `Secure`, `SameSite=Strict`, `Path=/`, `Domain=.tudominio.es`
- **Tokens JWT**:
  - Firmados con `RS256`
  - Token de sesión + refresco en cookie cifrada
  - Token de acceso cifrado y enviado por cabecera `Authorization`
- **Claims obligatorios**: `iss`, `sub`, `exp`, `jti`, `typ`, `idsession`, etc.
- **Claims personalizados**: `NumeroRefresco`, `fingerprintcookie`, roles, permisos
- **Refresco de tokens**: controlado con `MaxRefrescos` y `NumeroRefresco`
- **Cabeceras personalizadas**:
  - `X-Cert-Auth`: certificado en formato PEM
  - `X-Idsession`: identificador de sesión
  - `Authorization`: JWT de acceso cifrado (obligatorio)
- **Filtros**:
  - Validación de fingerprint en cookie
  - Restricción de acceso directo a endpoints (`BlockBrowserAccessFilter`)

---

## ✅ Cumplimiento POC

El middleware cumple con los requerimientos de seguridad establecidos por la POC para la opción 2:

- Flujo completo de autenticación protegido con token y fingerprint.
- Validación y encriptación rigurosa de tokens.
- Mecanismos de refresco robustos.
- Gestión segura de cookies y cabeceras.

---

## 🧱 Arquitectura Técnica

- **Lenguaje**: Java 17
- **Framework**: Spring Boot
- **JWT**: `jjwt`, `java.security`
- **Encriptación**: `AES/CBC/PKCS5Padding` con clave de 256 bits
- **Persistencia temporal**: Redis para tokens
- **Certificados**: Validación vía cabecera PEM (simulado en desarrollo)
- **Excepciones**: Centralizadas con `@ControllerAdvice`

---

## 📂 Organización del Código

- `controller/`: controladores REST para cada endpoint
- `service/`: lógica de negocio (JWT, fingerprint, refresco)
- `config/`: configuración de seguridad y CORS
- `filter/`: validadores de fingerprint y protección directa
- `exception/`: gestión global de errores y excepciones
- `repository/`: interfaz para almacenamiento auxiliar (ej. Redis)

---

## 🚀 Preparado para el futuro

- Compatible con arquitectura sin estado (stateless)
- Preparado para transición a OAuth2 / OpenID Connect
- Base sólida para despliegue en microservicios vía API Gateway

---

## 📝 Requisitos Externos

- Certificados de cliente correctamente configurados en F5 o servidor equivalente
- Integración con SSO actual para paso 1 del login
- Aplicaciones bajo dominio común `.tudominio.es` para política de cookies

---

## 🧪 Pruebas

- Requiere de pruebas unitarias básicas
- Requiere de Validación de endpoints críticos
- Requiere de Validación de seguridad y cabeceras con herramientas como Postman y curl