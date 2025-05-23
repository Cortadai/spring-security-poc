
# BankApp API - Cumplimiento POC Opción 1

Este backend forma parte de una arquitectura basada en SPA + REST y está diseñado para integrarse con un middleware de autenticación.

---

## 🎯 Objetivo

- Validar los JWT que llegan desde el middleware.
- Extraer y utilizar los claims de sesión y acceso.
- Gestionar el refresco y cierre de sesión.
- Proteger endpoints REST mediante filtros y cabeceras.

---

## 📌 Endpoints Implementados

| Endpoint             | Descripción                                                |
|----------------------|------------------------------------------------------------|
| `/loginEnd`          | Procesa la cookie `Sessiontmp`, genera cookies permanentes.|
| `/refresco`          | Genera nuevo token de acceso si ha expirado.               |
| `/logoff`            | Invalida y elimina cookies de sesión y acceso.             |
| `/obtenerclaims`     | Devuelve los claims de los tokens actuales.                |
| `/estadosession`     | Verifica si la sesión está activa.                         |

> 🔒 El endpoint `/loginBegin` no se implementa aquí; corresponde al middleware.

---

## 🔐 Seguridad

- **Cookies**: `HttpOnly`, `Secure`, `SameSite=Strict`, `Path=/`, `Domain=.tudomain.es`
- **JWT**:
  - Extraídos de las cookies `Session-{idsession}` y `Acceso-{idsession}`
  - Verificados con clave pública (`RS256`)
  - Claims personalizados utilizados para controlar roles y refrescos
- **Cabeceras**:
  - `X-Idsession`: requerida para todas las peticiones REST protegidas
- **Filtros**:
  - `JwtCookieAuthenticationFilter`: valida y autentica tokens de acceso
  - `GlobalExceptionHandler`: maneja errores centralizadamente
- **Protección contra ataques**:
  - CSRF: controlado mediante cabeceras y validación de sesión
  - XSS: mitigado del lado cliente (Angular)

---

## ✅ Cumplimiento POC

Este backend ha sido verificado para que cumpla estos puntos:

- ✔️ Uso correcto de cookies seguras
- ✔️ Validación robusta de tokens JWT
- ✔️ Expiración y refresco según `NumeroRefresco` / `MaxRefrescos`
- ✔️ Claims mínimos y personalizados presentes
- ✔️ Estructura modular y lista para microservicios

---

## ⚙️ Arquitectura Técnica

- **Lenguaje**: Java
- **Framework**: Spring Boot
- **JWT**: `jjwt`, utilidades internas (`SessionUtil`)
- **Seguridad**: Spring Security + filtros personalizados
- **Persistencia**: JPA + Repositorios
- **Cabeceras necesarias**: `X-Idsession` en todas las llamadas protegidas

---

## 🧪 Pruebas y Validación

- Comprobación de login, refresco, cierre de sesión
- Requiere Validación manual con herramientas como Postman o curl
- Manejo de errores controlado en todos los endpoints

---

## 📦 Organización del Proyecto

- `controller/middleware/`: Endpoints de sesión y seguridad
- `controller/bank/`: Endpoints de negocio (cuentas, tarjetas, préstamos...)
- `filter/`: Autenticación y validación de JWT
- `exception/`: Gestión de errores y accesos
- `config/`: Seguridad HTTP y configuración de clientes REST

---

## 🔄 Integración

Este backend espera que:

- Las cookies `Session-{idsession}` y `Acceso-{idsession}` estén correctamente generadas y encriptadas por el middleware.
- Se proporcione el identificador de sesión en la cabecera `X-Idsession`.
- Se mantenga la coordinación con el middleware en el proceso de login y refresco.