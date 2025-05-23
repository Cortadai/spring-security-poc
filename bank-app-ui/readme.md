# SPA Angular - Seguridad Opción 2

Este frontend Angular implementa una arquitectura SPA segura, integrada con un middleware de autenticación basado en tokens JWT protegidos mediante cookies fingerprint, en cumplimiento con los requisitos de la POC (opción 2).

---

## 🧩 Funcionalidad General

- Redirección desde SSO y login automático por backend `/login2EndSPA`.
- Recepción de tokens y almacenamiento local seguro (`sessionStorage`).
- Interceptores que gestionan tokens y cabeceras personalizadas.
- Lógica de refresco automático del token de acceso.
- Logout seguro y revocación de sesión.
- Protección de rutas mediante guards.

---

## 🔐 Seguridad

- **Token JWT**:
  - Guardado en almacenamiento local (nunca expuesto a scripts)
  - Cifrado y entregado por cabecera `Authorization`
  - Protegido por cookie `Proteccion-{idsession}`
- **Cookies**: `HttpOnly`, `Secure`, `SameSite=Strict` (gestión automática por navegador)
- **Cabeceras personalizadas**:
  - `Authorization`: token JWT cifrado
  - `X-Idsession`: identificador de sesión
  - `X-token-pro`: evita ataques CSRF y facilita control CORS
- **Validación indirecta**: todo se valida en backend y middleware

---

## ✅ Cumplimiento POC

La SPA implementa todo lo requerido para:

- Flujo de autenticación seguro
- Integración con backend `/login2EndSPA`, `/refrescoSPA`, `/logoffSPA`
- Protección de rutas con validación previa
- Tokens tratados de forma segura sin exponerlos

---

## 🧱 Arquitectura Técnica

- **Framework**: Angular
- **Autenticación**: basada en tokens JWT + fingerprint
- **Interceptores**: modifican todas las peticiones HTTP
- **Guards**: validan acceso a rutas según estado de sesión

---

## 📂 Organización del Código

- `services/auth`: lógica de login y logout
- `services/token`: gestión y refresco de token
- `interceptors/`: inyección de cabeceras seguras
- `guards/`: control de acceso a rutas
- `components/`: lógica visual
- `constants/`: configuración base (ej. urls, claves)

---

## 🧪 Pruebas

- Requiere validación manual con herramientas como Postman

---

## 📝 Requisitos Previos

- Middleware de seguridad funcional
- Backend con endpoints `/login2EndSPA`, `/refrescoSPA`, etc.
- Configuración de CORS, cookies y certificados adecuada
