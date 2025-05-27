# Fake SSO - Cumplimiento POC - Opción 1

Este módulo simula el comportamiento de un **SSO** real para facilitar el desarrollo y pruebas de aplicaciones SPA que siguen la **opción 1 de la POC de seguridad**. Inicia el flujo seguro mediante cookies y endpoints definidos, permitiendo verificar la interacción entre SSO → middleware → SPA.

---

## 🎯 Objetivo

- Simular una cookie `COOKIESSO` tras login exitoso.
- Llamar al endpoint `/loginBegin` del middleware.
- Redirigir a la SPA con la cookie `Sessiontmp` en cabecera.
- Probar el flujo completo sin necesidad de SSO real.

---

## 🔐 Seguridad y Requisitos

| Requisito                                 | Estado |
|------------------------------------------|--------|
| Cookie `COOKIESSO` generada               | ✅     |
| Cabecera `X-Cert-Auth` enviada            | ✅     |
| Conexión MTLS simulada                    | ⚠️     |
| Parámetro `idaplicacion` incluido         | ✅     |
| Cookie `Sessiontmp` enviada a la SPA      | ✅     |
| Redirección a SPA                         | ✅     |

> ⚠️ **Nota**: Este módulo está pensado para pruebas y simulación. En producción debe usarse una implementación real de SSO con certificados válidos y conexión MTLS efectiva.

---

## 🔄 Flujo Simulado

1. El usuario accede a `/login?app=idaplicacion`.
2. El controlador crea una cookie `COOKIESSO`.
3. Se llama al endpoint `/loginBegin` del middleware:
    - Incluye cabecera `Cookie: COOKIESSO=...`
    - Parámetro `idaplicacion`
    - Cabecera `X-Cert-Auth` (simulada)
4. Se recibe la cookie `Sessiontmp` en cabecera `Set-Cookie`.
5. Se redirige a `https://spa.tudominio.es/<idaplicacion>/`.

---

## 📂 Estructura del Proyecto

- `LoginController.java`: lógica principal del login y redirección.
- `login.html`: interfaz de prueba con formulario de login.
- `application.properties`: configuración de puerto y contexto.
- `FakeSsoApplication.java`: clase principal de Spring Boot.

---

## 🧪 Validación

- Probado junto al middleware de seguridad.
- Simula correctamente cookies y headers.
- Ideal para entornos de integración y validación.

---

## 📝 Consideraciones

- Adaptable a nuevos entornos cambiando dominio y endpoint.
- La cookie `Sessiontmp` es de corta duración (30 segundos).
- Requiere certificados reales y MTLS para entornos reales.

---