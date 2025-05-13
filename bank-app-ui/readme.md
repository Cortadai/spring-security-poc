
# 🏦+💻 bank-app-ui

Interfaz de usuario desarrollada en Angular para una aplicación bancaria ficticia. Esta aplicación forma parte de una Proof of Concept (PoC) que implementa autenticación segura basada en JWT, manejo de sesión, protección CSRF y renovación automática de tokens.

---

## 🔍 Descripción

`bank-app-ui` permite a los usuarios autenticarse, navegar por diferentes secciones (cuentas, préstamos, tarjetas, etc.) y gestionar su sesión de forma segura utilizando:

- 🔐 JWT con Refresh Token
- 🛡️ CSRF Protection via `XSRF-TOKEN` cookie + `X-XSRF-TOKEN` header
- 🔄 Auto-renovación de tokens
- 🚪 Logout seguro sincronizado con el backend

---

## 🧱 Estructura del Proyecto

```
src/app
├── components
│   ├── account
│   ├── balance
│   ├── cards
│   ├── contact
│   ├── dashboard
│   ├── header
│   ├── home
│   ├── loans
│   ├── login
│   ├── logout
│   └── notices
├── constants         # Rutas y constantes generales
├── interceptors      # Añade tokens y cabeceras automáticamente
├── model             # Interfaces y modelos de usuario
├── routeguards       # Guards de navegación (auth, role)
├── services          # AuthService, LoginService, LogoutService, etc.
├── app.component.*   # Entrada principal de la aplicación
```

---

## 🧪 Funcionalidad Principal

- ✅ Login con validación en backend
- ✅ Almacenamiento de access y refresh tokens en `sessionStorage`
- ✅ XSRF token guardado automáticamente desde cookie
- ✅ Redirección tras login a `/dashboard`
- ✅ Refresco automático del access token antes de expirar
- ✅ Logout con llamada al backend y limpieza local

---

## 🔐 Seguridad

- **Autenticación JWT**
  - Token de acceso (`Authorization`)
  - Token de refresco (`Authorization-Refresh`)
- **Renovación automática**
  - Se programa en segundo plano y se ejecuta 10 segundos antes de expirar
- **Protección CSRF**
  - Token `XSRF-TOKEN` en cookie
  - Cabecera `X-XSRF-TOKEN` enviada automáticamente
- **Logout**
  - Llama al backend (`POST /apiLogout`)
  - Backend borra y blacklistea los tokens en Redis
  - Frontend limpia `sessionStorage` y cancela timers

---

## 🔁 Comunicación con el Backend

- `GET /login` — Valida el usuario y devuelve tokens (headers)
- `POST /refresh` — Recibe `{ refreshToken }` y devuelve nuevos tokens
- `POST /apiLogout` — Elimina tokens en el servidor y termina la sesión

---

## 🧠 Gestión de Sesión

- Todos los tokens se almacenan en `sessionStorage`
- Se usa un `AuthService` que:
  - Decodifica el JWT
  - Programa la renovación automática
  - Maneja el refresco
  - Ejecuta logout y limpieza
- El `XhrInterceptor` agrega los headers `Authorization`, `X-XSRF-TOKEN` y `X-Requested-With` a cada petición.

---

## ⚙️ Instalación y uso

### 1. Requisitos

- Node.js 18+
- Angular CLI

### 2. Instalación

```bash
npm install
```

### 3. Arrancar en local

```bash
ng serve
```

Se inicia en: [http://localhost:4200](http://localhost:4200)

---

## 🌍 Variables de entorno

Se configuran desde:

```
src/environments/environment.ts
```

Incluye la variable:

```ts
rooturl: 'http://localhost:8080' // backend Spring Boot
```

---

## 📦 Dependencias clave

- `@angular/common/http`
- `rxjs`
- `jwt-decode` (si decides usarlo en el futuro)
- Cookies gestionadas automáticamente por el navegador

---

## 🛠️ Pendientes o mejoras futuras

- Guardas de ruta (`canActivate`) según rol del usuario
- Página de expiración de sesión personalizada
- Feedback visual en errores de login/logout
- Uso de interceptores para auto-refrescar token si la respuesta es `401`
- Logout automático si se detecta refresh inválido
