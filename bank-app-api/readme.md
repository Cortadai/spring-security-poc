# 🏦+🛠️ bank-app-api

API REST desarrollada en Spring Boot para una aplicación bancaria ficticia. Este backend forma parte de una Proof of Concept (PoC) que implementa autenticación segura basada en JWT, manejo de tokens en Redis, protección contra CSRF, renovación automática de tokens y logout con invalidación activa. Además, expone endpoints protegidos por roles y estructura lógica pensada para escenarios reales.

---

## 🔍 Descripción

Este proyecto es una **prueba de concepto (PoC)** de una API REST construida con **Spring Boot 3.4.5**, orientada a servicios bancarios básicos. La aplicación simula un backend típico con funcionalidades como:

- Registro y autenticación de usuarios
- Consulta de cuentas, tarjetas, préstamos y movimientos
- Gestión de tokens JWT para autenticación stateless
- Protección con CSRF y CORS
- Manejo centralizado de excepciones
- Logging detallado

La arquitectura sigue un enfoque limpio y modular, con separación clara de responsabilidades.

---

## 🧱 Estructura del proyecto

```
src
├── main
│   ├── java
│   │   └── com.entelgy.bank
│   │       ├── config                  → Configuración de seguridad, CORS, CSRF
│   │       ├── controller              → Controladores REST públicos y protegidos
│   │       ├── dto                     → Clases DTO para respuestas
│   │       ├── exception               → Excepciones personalizadas
│   │       ├── exception.entrypoint    → EntryPoint de autenticación personalizada
│   │       ├── exception.handler       → GlobalExceptionHandler
│   │       ├── filter                  → Filtros de validación y emisión de JWT
│   │       ├── model                   → Entidades JPA
│   │       ├── repository              → Interfaces JPA/Redis
│   │       ├── service                 → Servicios de negocio
│   └── resources
│       ├── application.properties      → Configuración de base de datos y Redis
│       └── notas.md                    → Apuntes del desarrollo
```

---

## 🔐 Seguridad y autenticación

- El sistema **no utiliza sesiones de servidor**: toda la autenticación se basa en **JWTs**.
- La autenticación se realiza mediante `/apiLogin` (credenciales básicas).
- Tras autenticación válida, se genera un JWT y se incluye en la cabecera `Authorization` de futuras peticiones.
- El filtro `JWTTokenValidatorFilter` se encarga de validar la autenticidad, validez temporal y estado en blacklist de cada token.
- Los tokens pueden invalidarse (blacklist) mediante el endpoint `/apiLogout`.

### 🛠️ Configuración destacada:

- `SecurityFilterChain` configura:
    - CSRF habilitado con exclusión de ciertos endpoints
    - Protección CORS solo desde `http://localhost:4200`
    - Política `SessionCreationPolicy.STATELESS`
    - EntryPoint personalizado para respuestas 401 en formato JSON

---

## ⚠️ Control de errores

### Errores de autenticación

- Implementado en `CustomBasicAuthenticationEntryPoint`.
- Devuelve una respuesta 401 en JSON con estructura:

```json
{
  "timestamp": "13/05/2025 10:23:33",
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource",
  "path": "/refresh"
}
```

### Errores de negocio y lógica

- Manejados en `GlobalExceptionHandler` mediante `@RestControllerAdvice`.
- Las excepciones personalizadas incluyen:
    - `TokenAuthenticationException`
    - `InvalidTokenException`
    - `TokenBlacklistedException`
    - `TokenMismatchException`
- También se maneja `BadCredentialsException` y errores genéricos (`Exception`).

---

## 📜 Logging

- Se utiliza SLF4J (`@Slf4j`) para logging estructurado en los filtros y servicios críticos.
- El validador de tokens (`JWTTokenValidatorFilter`) informa de:
    - Tokens inválidos
    - Tokens expirados
    - Acceso denegado
- El token provider (`TokenProvider`) informa de validaciones y parsing de JWT.

---

## 📡 Endpoints destacados

| Método | Ruta            | Autenticación | Descripción                             |
|--------|------------------|---------------|------------------------------------------|
| POST   | /apiLogin        | No            | Login con usuario y password             |
| POST   | /apiLogout       | Sí            | Invalida el JWT actual (blacklist)       |
| GET    | /refresh         | Sí            | Prueba de endpoint protegido             |
| GET    | /myAccount       | Sí (USER)     | Consulta de cuenta bancaria              |
| GET    | /myCards         | Sí (USER)     | Consulta de tarjetas                     |
| GET    | /myLoans         | Sí (USER)     | Consulta de préstamos                    |
| GET    | /myBalance       | Sí (USER/ADMIN)| Consulta de balance                      |
| GET    | /user            | Sí            | Información básica del usuario autenticado |
| GET    | /notices         | No            | Noticias públicas                        |
| POST   | /contact         | No            | Envío de mensaje de contacto             |
| POST   | /register        | No            | Registro de nuevo cliente                |

---

## 💡 Consideraciones adicionales

- Se utiliza `Redis` como almacenamiento para el blacklist de tokens.
- Toda la lógica de validación de JWT está externalizada en `TokenProvider`.
- La clase `CsrfCookieFilter` se asegura de enviar el token CSRF en cookie tras autenticación.

---

## 🧰 Requisitos

- Java 17
- Maven
- Base de datos MySQL
- Redis local