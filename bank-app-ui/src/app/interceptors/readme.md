# 🛡️ Interceptor de Seguridad Único: `AuthSecurityInterceptor`

Este interceptor reemplaza los antiguos `CsrfInterceptor` y `ExpirationInterceptor`, centralizando toda la lógica de seguridad en **una única clase**.

---

## 📦 Qué hace este interceptor

### 1. **Controla la sesión activa**

* Llama a `tokenManager.verificarYRefrescar()`.
* Si la sesión ha caducado, muestra un `Swal` de aviso y redirige al login.

### 2. **Protege contra CSRF**

* Antes de cada `POST`, `PUT`, `DELETE`, obtiene el token CSRF desde el backend (`/csrf`).
* Añade la cabecera `X-Token-Pro` con el token recibido.

### 3. **Gestiona cookies y cabeceras**

* Añade siempre `withCredentials: true`.
* Añade la cabecera `X-Idsession` con el valor almacenado en `sessionStorage`.

---

## 🔁 ¿Cuándo actúa?

### ✔️ Actúa en:

* Todas las peticiones **no seguras** (`POST`, `PUT`, `DELETE`) que **no están excluidas**.

### ❌ Ignora:

* Métodos seguros: `GET`, `HEAD`, `OPTIONS`.
* Endpoints internos que no deben validar CSRF o sesión:

  * `/csrf`
  * `/expires`
  * `/refresh`
  * `/logInEnd`
  * `/logOff`
  * `/getClaims`
  * `/getSessionState`

---

## 🧱 Cómo está organizado

### Interceptor: `auth-security.interceptor.ts`

Ubicación sugerida: `src/app/interceptors/auth-security.interceptor.ts`

### Registro:

```ts
providers: [
  {
    provide: HTTP_INTERCEPTORS,
    useClass: AuthSecurityInterceptor,
    multi: true
  }
]
```

---

## 🔐 Seguridad en orden correcto

Para cualquier `POST`, se garantiza:

1. **Verificación de sesión activa**
   (`tokenManager.verificarYRefrescar()`)

2. **Obtención del token CSRF actualizado**
   (`GET /csrf`)

3. **Clonación de la request con todas las cabeceras y `withCredentials: true`**

4. **Envío seguro al backend**

---

## 💣 Si ocurre un 401 o 403...

El interceptor:

* Muestra una alerta de sesión caducada con SweetAlert2.
* Llama a `authService.cerrarSesionYRedirigir()` para limpiar sesión y navegar al login.

---

## 🛠️ Requisitos en backend

* El endpoint `/csrf` debe devolver:

  ```json
  {
    "token": "...",
    "headerName": "X-Token-Pro",
    "parameterName": "_csrf"
  }
  ```
* Spring debe estar configurado para:

  * Usar `CookieCsrfTokenRepository`.
  * Cambiar el nombre de la cabecera a `X-Token-Pro`.
  * Aceptar cookies con `SameSite=Lax` y `Secure=false` en desarrollo.

---

## ✅ Beneficios

* Código limpio y centralizado.
* Evita duplicación y problemas de orden entre múltiples interceptores.
* Manejo robusto de sesión y protección CSRF.
