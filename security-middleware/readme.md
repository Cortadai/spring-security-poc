# Security Middleware - Cumplimiento POC - Opción 1

Este proyecto es un **middleware de autenticación y autorización** que implementa requerimientos de seguridad para aplicaciones SPA (Angular) que acceden a backends mediante interfaces REST.

---

## 🧩 Funcionalidad General

- Autenticación en dos pasos mediante redirección desde el SSO.
- Generación y validación de tokens JWT para sesión, acceso y refresco.
- Almacenamiento seguro de tokens en cookies encriptadas.
- Expiración y refresco automático de tokens.
- Endpoints REST protegidos, preparados para uso interno y futura migración a microservicios.
- Validación de certificados de cliente mediante cabecera `X-Cert-Auth`.

---

## 📌 Endpoints Implementados

| Endpoint            | Descripción                                                |
|---------------------|------------------------------------------------------------|
| `/loginBegin`       | Genera cookie temporal `Sessiontmp` a partir de SSO.       |
| `/loginEnd`         | Genera cookies `Session-{idsession}` y `Acceso-{idsession}`.|
| `/refresco`         | Regenera token de acceso si ha expirado.                   |
| `/logoff`           | Invalida sesión y borra cookies.                           |
| `/obtenerclaims`    | Devuelve los claims de los tokens actuales.                |
| `/estadosession`    | Verifica si la sesión está activa.                         |
| `/validartoken`     | Valida un token JWT (uso opcional).                        |

---

## 🔐 Seguridad

- **Cookies seguras**: `HttpOnly`, `Secure`, `SameSite=Strict`, `Path=/`, `Domain=.tudominio.es`
- **Tokens JWT**:
    - Firmados con `RS256`
    - Encriptados dentro de cookies usando `AES/CBC/PKCS5Padding`
- **Claims obligatorios**: `iss`, `sub`, `exp`, `jti`, `typ`, `idsession`, etc.
- **Claims personalizados**: `NumeroRefresco`, roles, permisos, etc.
- **Refresco de tokens**: basado en `MaxRefrescos` y `NumeroRefresco`.
- **Cabeceras personalizadas**:
    - `X-Cert-Auth`: certificado en formato PEM
    - `X-Idsession`: identificador único de sesión
    - `Authorization`: en uso interno si se necesita
- **Filtros**: bloqueo de acceso directo a endpoints desde el navegador (`BlockBrowserAccessFilter`)

---

## ✅ Cumplimiento POC

El middleware ha sido verificado punto por punto, cumpliendo con:

- Toda la lógica de autenticación esperada.
- Estructura y protección de cookies.
- Encriptación y firma de tokens.
- Gestión de sesiones temporales.
- Validez de claims y cabeceras requeridas.

---

## 🧱 Arquitectura Técnica

- **Lenguaje**: Java 17
- **Framework**: Spring Boot
- **JWT**: `jjwt`, `java.security`,
- **Encriptación**: `AES/CBC/PKCS5Padding` con clave de 256 bits
- **Persistencia temporal**: Redis para tokens
- **Certificados**: MTLS y validación PEM vía cabecera (Simulado en entorno de desarrollo)
- **Excepciones**: manejadas de forma centralizada (`@ControllerAdvice`)

---

## 📂 Organización del Código

- `controller/`: lógica HTTP por endpoint
- `service/`: lógica de negocio (generación, validación, refresco)
- `config/`: configuración de seguridad, JWT y Redis
- `filter/`: filtros de acceso y validación
- `exception/`: excepciones personalizadas y handler global
- `repository/`: interfaz para almacenamiento de tokens (Redis)

---

## 🚀 Preparado para el futuro

- Compatible con arquitecturas sin estado (stateless)
- Base sólida para migración a OAuth2 / OpenID Connect
- Adaptable a microservicios vía API Gateway

---

## 📝 Requisitos Externos

- Certificados configurados correctamente en un F5 o similar.
- Integración con SSO actuales y reales para login inicial
- Servidor de aplicaciones bajo el dominio `.tudominio.es`

---

## 🧪 Pruebas

- Requiere de pruebas unitarias básicas
- Requiere de Validación de endpoints críticos
- Requiere de Validación de seguridad y cabeceras con herramientas como Postman y curl