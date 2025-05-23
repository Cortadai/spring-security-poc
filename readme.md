# 📚 Documentación Relacionada

- [Parte 1: Objetivo](./objetivo.md)

---

- [Parte 2.1: Fake SSO - info](./fake-sso/info.md)
- [Parte 2.2: Fake SSO - readme](./fake-sso/readme.md)

---

- [Parte 3.1: Middleware de Seguridad - info](./security-middleware/info.md)
- [Parte 3.2: Middleware de Seguridad - readme](./security-middleware/readme.md)

---

- [Parte 4.1: Bank-App-Api - info](./bank-app-api/info.md)
- [Parte 4.2: Bank-App-Api - readme](./bank-app-api/readme.md)

---

- [Parte 5.1: Bank-App-Ui - info](./bank-app-ui/info.md)
- [Parte 5.2: Bank-App-Ui - readme](./bank-app-ui/readme.md)

---

# ✅ Checklist del Cumplimiento de Requerimientos POC - Opción 2

| Requisito                                     | Implementación en Middleware                   | Cumple |
|----------------------------------------------|-----------------------------------------------|--------|
| **/loginBegin**                              | `LogInBeginController.java`                   | ✅     |
| **/loginEnd**                                | `LogInEndController.java`                     | ✅     |
| **/refresco**                                | `RefreshController.java`                      | ✅     |
| **/logoff**                                  | `LogOffController.java`                       | ✅     |
| **/obtenerclaims**                           | `ClaimsController.java`                       | ✅     |
| **/estadosession**                           | `SessionController.java`                      | ✅     |
| Firma JWT (RS256)                            | `TokenProvider.java`                          | ✅     |
| Encriptación de JWT en cookies               | `BaseController.java` + AES (CBC/PKCS5Padding)| ✅     |
| Cookies seguras                              | `HttpOnly`, `Secure`, `SameSite=Strict`       | ✅     |
| Claims obligatorios (iss, sub, exp, typ...)  | `TokenProvider.java`                          | ✅     |
| Claims personalizados (roles, idsession...)  | `TokenProvider.java`                          | ✅     |
| Cabecera `X-Cert-Auth`                       | Todos los controladores                       | ✅     |
| Cabecera `X-Idsession`                       | Todos los controladores                       | ✅     |
| Cabecera `Authorization` con JWT cifrado     | SPA / Backend / Middleware                    | ✅     |
| Validación JWT completa + fingerprint        | `JwtCookieAndTokenAuthenticationFilter.java`  | ✅     |
| Gestión segura de `Sessiontmp`               | `LogInBeginController`, `LogInEndController`  | ✅     |
| Control de expiración y refresco             | `RefreshController`, `RefreshService`         | ✅     |
| Separación por capas y lógica limpia         | Servicios, controladores, filtros             | ✅     |
| Protección contra acceso navegador directo   | `BlockBrowserAccessFilter.java`               | ✅     |
| Manejador de errores global                  | `GlobalExceptionHandler.java`                 | ✅     |

---

## 🔄 FLUJO DE LLAMADAS A TRAVÉS DE LOS 4 COMPONENTES

### 🟪 1. Usuario accede al SSO simulado (Fake SSO)

* **Componente**: Fake SSO (`/login`)
* **Acción**:
    * El usuario selecciona una aplicación (ej. `idaplicacion=bankapp`)
    * Se crea la cookie **`COOKIESSO`** (no segura)
    * Se llama al middleware vía:

      ```http
      POST /loginBegin?idaplicacion=bankapp
      Cookie: COOKIESSO=...
      X-Cert-Auth: (certificado PEM simulado)
      ```

---

### 🟦 2. Middleware genera `Sessiontmp` y redirige a la SPA

* **Componente**: Middleware
* **Acción**:
    * Valida `COOKIESSO` y `X-Cert-Auth`
    * Genera JWT temporal y crea cookie `Sessiontmp`
    * Responde con:

      ```http
      Set-Cookie: Sessiontmp=valor_encriptado; Secure; HttpOnly; SameSite=Strict; Max-Age=30
      ```

    * Redirección:

      ```http
      Location: https://spa.tudominio.es/bankapp/
      ```

---

### 🟩 3. SPA detecta `Sessiontmp` y llama a `/login2EndSPA`

* **Componente**: Angular (SPA)
* **Acción**:
    * Detecta cookie `Sessiontmp`
    * Llama al backend:

      ```http
      POST /login2EndSPA
      Cookie: Sessiontmp=...
      X-Cert-Auth: (certificado PEM)
      ```

---

### 🟥 4. Backend llama al middleware para generar sesión real

* **Componente**: Backend SPA
* **Acción**:
    * Redirige `/login2EndSPA` al middleware `/loginEnd`
    * Middleware genera:
        * `tokenSession`, `tokenRefresco`, `tokenAcceso`
    * Devuelve:

      ```http
      Set-Cookie: Session-{idsession}=... ; HttpOnly; Secure; ...
      Set-Cookie: Proteccion-{idsession}=hash; HttpOnly; Secure; ...
      Authorization: Bearer {jwt_cifrado}
      X-Idsession: {idsession}
      ```

---

### 🟨 5. SPA guarda token y realiza peticiones de negocio

* **Componente**: Angular
* **Acción**:
    * Guarda `Authorization` en localStorage
    * Peticiones HTTP:

      ```http
      Authorization: Bearer {jwt_cifrado}
      X-Idsession: {idsession}
      X-token-pro: 1
      Cookie: Proteccion-{idsession}=...
      ```

---

### 🟫 6. SPA detecta expiración y lanza refresco

* **Componente**: Angular → Backend → Middleware
* **Acción**:
    * Llama a `/refrescoSPA`, que redirige a `/refresco`
    * Middleware valida tokens y responde con:

      ```http
      Set-Cookie: Proteccion-{idsession}=nuevo_hash
      Authorization: Bearer {nuevo_jwt_cifrado}
      ```

---

### ⛔ 7. Logout

* **Componente**: Angular → Backend → Middleware
* **Acción**:
    * Angular llama a `/logoffSPA`
    * Middleware borra cookies y limpia cabeceras:

      ```http
      Set-Cookie: Session-{idsession}=; Max-Age=0
      Set-Cookie: Proteccion-{idsession}=; Max-Age=0
      Authorization: (vacía)
      X-Idsession: (vacía)
      ```

---

## 📌 RESUMEN DE COMPONENTES EN CADA FASE

| Fase                 | Componente principal | Endpoint clave        |
|----------------------|----------------------|-----------------------|
| Login inicial        | Fake SSO             | `/loginBegin`         |
| Token temporal       | Middleware           | `/loginBegin`         |
| Generación sesión    | Middleware           | `/loginEnd`           |
| Inicio sesión en SPA | Angular + Backend    | `/login2EndSPA`       |
| Acceso a negocio     | Angular + Backend    | `/negocio`, `/notices` |
| Refresco de token    | Angular + Middleware | `/refrescoSPA`        |
| Logout               | Angular + Middleware | `/logoffSPA`          |
