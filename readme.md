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

# ✅ Checklist del Cumplimiento de Requerimientos POC - Opción 1

| Requisito                                     | Implementación en Middleware                   | Cumple |
|----------------------------------------------|-----------------------------------------------|--------|
| **/loginBegin**                              | `LogInBeginController.java`                   | ✅     |
| **/loginEnd**                                | `LogInEndController.java`                     | ✅     |
| **/refresco**                                | `RefreshTokenController.java`                 | ✅     |
| **/logoff**                                  | `LogOffController.java`                       | ✅     |
| **/obtenerclaims**                           | `ClaimsController.java`                       | ✅     |
| **/estadosession**                           | `SessionController.java`                      | ✅     |
| Firma JWT (RS256)                            | `TokenProvider.java`                          | ✅     |
| Encriptación de JWT dentro de cookies        | `BaseController.java` + `AES/CBC/PKCS5Padding`| ✅     |
| Cookies con atributos seguros                | `HttpOnly`, `Secure`, `SameSite=Strict`, etc. | ✅     |
| Claims obligatorios (iss, sub, exp, typ...)  | `TokenProvider.java`                          | ✅     |
| Claims personalizados (roles, idsession...)  | `TokenProvider.java`                          | ✅     |
| Cabecera `X-Cert-Auth` con certificado       | Todos los controladores                       | ✅     |
| Cabecera `X-Idsession`                       | Todos los controladores                       | ✅     |
| Validación JWT completa                      | `TokenProvider`, `SecurityInfoService`        | ✅     |
| Gestión segura de `Sessiontmp`               | `LogInBeginController`, `LogInEndController`  | ✅     |
| Control de expiración y refresco             | `RefreshTokenController`, `RefreshService`    | ✅     |
| Separación por capas y lógica limpia         | Servicios, controladores, filtros             | ✅     |
| Protección contra acceso navegador directo   | `BlockBrowserAccessFilter.java`               | ✅     |
| Manejador de errores global                  | `GlobalExceptionHandler.java`                 | ✅     |

---

## 🔄 FLUJO DE LLAMADAS A TRAVÉS DE LOS 4 COMPONENTES

### 🟪 1. Usuario accede al SSO simulado (Fake SSO)

* **Componente**: Fake SSO (`/login`)
* **Acción**:

    * El usuario selecciona una aplicación (por ejemplo: `idaplicacion=bankapp`)
    * Se crea la cookie **`COOKIESSO`** (no segura)
    * Se llama al middleware vía:

      ```
      POST /loginBegin?idaplicacion=bankapp
      Cookie: COOKIESSO=...
      X-Cert-Auth: (certificado PEM simulado)
      ```

---

### 🟦 2. Middleware genera `Sessiontmp` y redirige a la SPA

* **Componente**: Middleware

* **Acción**:

    * Valida `COOKIESSO` y `X-Cert-Auth`
    * Genera un JWT temporal (`tokenSesionTemporal`) con `idaplicacion`, `idusuario`, `idsession`
    * Lo encripta y crea la cookie `Sessiontmp`
    * Responde con:

      ```
      Set-Cookie: Sessiontmp=valor_encriptado; Secure; HttpOnly; SameSite=Strict; ...
      ```

* **Fake SSO** recibe esta cookie y redirige a:

  ```
  Location: https://spa.tudominio.es/bankapp/
  ```

---

### 🟩 3. SPA detecta `Sessiontmp` y llama a `/loginEnd`

* **Componente**: Angular (SPA)
* **Acción**:

    * Al cargarse, detecta que hay una cookie `Sessiontmp`
    * Llama al backend SPA:

      ```
      POST /loginEnd
      Cookie: Sessiontmp=...
      X-Cert-Auth: (certificado PEM real)
      ```

---

### 🟥 4. Backend llama al middleware para generar sesión real

* **Componente**: Backend SPA
* **Acción**:

    * Redirige la petición `/loginEnd` al endpoint REST `/loginEnd` del middleware.

    * Middleware desencripta `Sessiontmp` y extrae `idusuario`, `idaplicacion`, `idsession`

    * Genera:

        * `tokenSession` (largo plazo)
        * `tokenRefresco`
        * `tokenAcceso` (corto plazo)

    * Devuelve cookies:

      ```
      Set-Cookie: Session-{idsession}=... ; Secure; HttpOnly; ...
      Set-Cookie: Acceso-{idsession}=... ; Secure; HttpOnly; ...
      ```

    * También cabecera:

      ```
      X-Idsession: {idsession}
      ```

---

### 🟨 5. SPA envía peticiones de negocio

* **Componente**: Angular (SPA)

* **Acción**:

    * Al hacer peticiones (por ejemplo a `/cuentas`, `/notices`, etc.), el navegador **envía automáticamente las cookies**.
    * Angular añade:

      ```
      X-Idsession: {idsession}
      X-token-pro: 1
      withCredentials: true
      ```

* El backend extrae los JWT de las cookies y los valida.

---

### 🟫 6. SPA detecta expiración del token de acceso y lo refresca

* **Componente**: Angular (`expiration.interceptor.ts`)

* **Acción**:

    * Detecta que el token de acceso está por expirar
    * Llama a:

      ```
      POST /refresco
      Cookie: Session-{idsession}, Acceso-{idsession}
      X-Idsession: {idsession}
      X-Cert-Auth: (certificado)
      ```

* Middleware valida sesión y refresco, genera nuevo `tokenAcceso`, lo devuelve en:

  ```
  Set-Cookie: Acceso-{idsession}=nuevo_token
  ```

---

### ⛔ 7. Logout

* **Componente**: Angular → Backend → Middleware
* **Acción**:

    * Angular llama a `/logoff` → backend → middleware
    * Middleware invalida los tokens y devuelve cookies con `Max-Age=0`:

      ```
      Set-Cookie: Session-{idsession}=... ; Max-Age=0
      Set-Cookie: Acceso-{idsession}=... ; Max-Age=0
      ```

---

## 📌 RESUMEN DE COMPONENTES EN CADA FASE

| Fase                 | Componente principal | Endpoint clave |
| -------------------- | -------------------- | -------------- |
| Login inicial        | Fake SSO             | `/loginBegin`  |
| Token temporal       | Middleware           | `/loginBegin`  |
| Generación sesión    | Middleware           | `/loginEnd`    |
| Inicio sesión en SPA | Angular + Backend    | `/loginEnd`    |
| Acceso a negocio     | Angular + Backend    | `/{negocio}`   |
| Refresco de token    | Angular + Middleware | `/refresco`    |
| Logout               | Angular + Middleware | `/logoff`      |
