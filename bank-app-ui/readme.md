# SPA Angular - Cumplimiento POC - Opción 1

Esta aplicación Angular forma parte de una arquitectura SPA + REST conforme a la **opción 1 para la POC de seguridad**. Cumple con todos los requisitos exigidos para aplicaciones que se integran mediante cookies y tokens JWT gestionados por un middleware.

---

## 🎯 Objetivo

- Cargar la SPA tras el login desde el SSO.
- Ejecutar `/loginEnd` al detectar la cookie `Sessiontmp`.
- Dejar que el navegador gestione las cookies `Session-{idsession}` y `Acceso-{idsession}`.
- Refrescar automáticamente el token de acceso si está por expirar.
- Nunca acceder al JWT directamente desde Angular.
- Proteger todos los endpoints con cabeceras seguras y guards.

---

## 🔐 Seguridad y Requisitos POC

| Requisito                                   | Estado |
|---------------------------------------------|--------|
| Uso exclusivo de cookies seguras            | ✅     |
| No lectura de JWT desde Angular             | ✅     |
| Redirección automática con `/loginEnd`      | ✅     |
| `withCredentials: true` en todas las peticiones | ✅ |
| Cabecera `X-Idsession`                      | ✅     |
| Cabecera `X-token-pro`                      | ✅     |
| Refresco automático con `/refresco`         | ✅     |
| Guards de sesión y roles (`sesion.guard.ts`, `rol.guard.ts`) | ✅ |
| Protección CSRF mediante cabecera + SameSite| ✅     |
| Protección XSS (Angular binding seguro)     | ✅     |

---

## 🧩 Componentes Relevantes

- `login.component.ts`: punto de entrada tras redirección del SSO.
- `logout.component.ts`: llama al endpoint `/logoff` y limpia sesión.
- `auth-security.interceptor.ts`: añade `X-Idsession` y `withCredentials`.
- `csrf.interceptor.ts`: añade cabecera `X-token-pro`.
- `expiration.interceptor.ts`: refresca token con `/refresco` antes de expirar.
- `rol.guard.ts`, `sesion.guard.ts`: control de acceso a rutas.

---

## 🌐 Flujo General

1. El SSO redirige a la SPA con la cookie `Sessiontmp`.
2. Angular ejecuta `/loginEnd` automáticamente.
3. El backend responde con las cookies de sesión y acceso.
4. Angular **nunca toca directamente los tokens**.
5. Las peticiones HTTP incluyen automáticamente las cookies.
6. Cuando el token de acceso se acerca a su expiración, Angular llama a `/refresco`.

---

## 📁 Organización del Código

- `components/`: vistas por funcionalidad (dashboard, cuenta, préstamos…)
- `interceptors/`: lógica de seguridad automática en cada petición
- `guards/`: validación previa a navegación
- `services/`: llamadas a endpoints REST
- `model/`: interfaces de datos

---

## 🧪 Validación

- Probado en combinación con middleware y backend.
- Verificado manualmente mediante herramientas como DevTools.

---

## 🛡️ Preparado para Producción

- Arquitectura escalable y modular
- Soporte para múltiples sesiones con `sessionStorage`
- Seguridad by default: binding seguro, cookies restringidas, CORS configurado
