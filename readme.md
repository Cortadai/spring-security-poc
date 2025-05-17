# ⚠️ Checklist - Pending tasks

- [ ] Agregar documentación
- [ ] Agregar diagramas de secuencia
- [ ] Encriptar valores en JWTs
- [x] Limpieza de clases en bank-app-api
- [x] Renombrar urls en en bank-app-ui
- [x] Repasar uso de constantes
- [x] Repasar configuración de seguridad
- [x] Automatizar arranque BBDD mySQL en bank-app-api
- [ ] Token CSRF y cabecera X-Token-Pro en bank-app-api

# ✅ Checklist - Seguridad POC

| Verificación | Estado |
|--------------|--------|
| SPA Angular usa cookies seguras (`SameSite=Strict`, `HttpOnly`, `Secure`) | ✅ |
| SPA envía cabeceras personalizadas (`X-Idsession`, etc.) con `withCredentials` | ✅ |
| SPA no accede al middleware directamente (solo backend lo hace) | ✅ |
| Backend de SPA valida JWT en cookies y delega autenticación en middleware | ✅ |
| Backend de SPA expone solo endpoints con roles (`hasRole(...)`, etc.) | ✅ |
| Middleware tiene arquitectura sin estado (`SessionCreationPolicy.STATELESS`) | ✅ |
| Middleware genera cookies con atributos de seguridad (`Secure`, `HttpOnly`, etc.) | ✅ |
| Middleware protege acceso con validaciones internas (`X-Cert-Auth`, etc.) | ✅ |
| Middleware no es accesible desde navegador (`BlockBrowserAccessFilter`) | ✅ |
| Todos los endpoints (`/loginBegin`, `/login1End`, etc.) están implementados | ✅ |
| Validación y refresco de tokens JWT funcionando correctamente | ✅ |
| CORS configurado con orígenes válidos y cabeceras esperadas | ✅ |
| Cabeceras HTTP de seguridad (`Cache-Control`, `X-Frame-Options`, etc.) presentes | ✅ |
| HSTS (`Strict-Transport-Security`) pendiente de añadir en producción | ⚠️ |
| Cambio de `.requiresInsecure()` a `.requiresSecure()` pendiente para producción | ⚠️ |
| Uso de `X-Token-Pro` como protección CSRF opcional (por ahora no necesario) | ⚠️ |
