# 🏦 Spring Security PoC - Sistema de Autenticación Empresarial Bancario

Sistema de autenticación de grado de producción que implementa integración SSO, middleware de seguridad y patrones de autenticación para SPAs.

## 🎯 Descripción General

Este repositorio contiene una **solución completa de autenticación empresarial** con múltiples enfoques de implementación para aplicaciones bancarias. Demuestra cómo integrar sistemas SSO legacy con SPAs modernas manteniendo seguridad de grado bancario.

## 🏗️ Arquitectura

El proyecto implementa una arquitectura de 4 capas:

```
┌─────────────┐     ┌──────────────┐     ┌─────────────┐     ┌─────────────┐
│  Fake SSO   │ --> │   Security   │ --> │   Backend   │ --> │   Angular   │
│  (Legacy)   │     │  Middleware  │     │     API     │     │     SPA     │
└─────────────┘     └──────────────┘     └─────────────┘     └─────────────┘
```

## 📁 Estructura del Repositorio

Este repositorio contiene **tres ramas** con diferentes enfoques:

### 🌿 Rama `main`
Implementación básica con autenticación JWT estándar.

### 🌿 Rama `option1` - Autenticación Basada en Cookies
- Tokens JWT almacenados en cookies HttpOnly seguras
- Protección completa contra ataques XSS
- Gestión automática de tokens por el navegador
- Ideal para despliegues en el mismo dominio

### 🌿 Rama `option2` - Autenticación Híbrida
- JWT en header Authorization + fingerprint en cookies
- Patrón double-submit cookie para protección CSRF
- Almacenamiento en localStorage con consideraciones XSS
- Ideal para despliegues cross-domain o microservicios

## ✨ Características Principales Implementadas

### Patrones de Seguridad
- **Integración SSO** - SSO empresarial simulado con puente de sesiones
- **Middleware de Seguridad** - Capa centralizada de autenticación/autorización
- **Gestión JWT** - Patrón Access/Refresh token con renovación automática
- **Protección CSRF** - Tanto basada en cookies como patrón double-submit
- **Autenticación por Certificado** - Validación de certificados X.509
- **Gestión de Sesiones** - Manejo distribuido con IDs únicos de sesión
- **Encriptación de Tokens** - Encriptación AES para tokens sensibles

### Implementación Técnica
- **7 Endpoints de Seguridad** - `/loginBegin`, `/loginEnd`, `/refresco`, `/logoff`, etc.
- **Firma JWT RS256** - Criptografía de clave asimétrica
- **Arquitectura por Capas** - Controladores, Servicios, Filtros, Manejadores de Excepciones
- **Configuraciones de Producción** - Headers seguros, CORS, rate limiting

## 📚 Documentación Detallada

Cada rama contiene documentación exhaustiva:

- **Opción 1**: [Implementación Basada en Cookies](../../tree/option1)
- **Opción 2**: [Implementación Híbrida](../../tree/option2)

### Documentación disponible en cada rama:
- `/objetivo.md` - Objetivos y requisitos del sistema
- `/fake-sso/` - Documentación del SSO simulado
- `/security-middleware/` - Documentación del middleware de seguridad
- `/bank-app-api/` - Documentación del backend
- `/bank-app-ui/` - Documentación del frontend

## 🎯 Casos de Uso

Este PoC es ideal para:
- Empresas migrando de SSO legacy a autenticación moderna
- Sistemas que requieren implementaciones de seguridad conformes a regulación
- Equipos evaluando estrategias de almacenamiento JWT (cookies vs localStorage)
- Arquitectos diseñando flujos seguros de autenticación para SPAs

## 🛡️ Consideraciones de Seguridad

Ambas implementaciones abordan:
- Prevención de Ataques XSS
- Mitigación de Ataques CSRF
- Protección contra Secuestro de Tokens
- Prevención de Fijación de Sesión
- Almacenamiento Seguro de Tokens
- Certificate Pinning

## 📈 Por Qué Importa Este Proyecto

Esto no es solo otro "tutorial JWT" - es un **blueprint listo para producción** de autenticación empresarial que aborda desafíos del mundo real como integración SSO, cumplimiento regulatorio y patrones modernos de seguridad para SPAs.

## 🔍 Comparación de Implementaciones

| Característica | Option1 (Cookies) | Option2 (Híbrido) |
|----------------|-------------------|-------------------|
| Almacenamiento JWT | HttpOnly Cookies | localStorage + Authorization header |
| Protección XSS | ✅ Completa | ⚠️ Requiere sanitización |
| Protección CSRF | ✅ SameSite cookies | ✅ Double-submit pattern |
| Cross-domain | ❌ Limitado | ✅ Soportado |
| Gestión automática | ✅ Por navegador | ❌ Manual en JS |

## 💻 Stack Tecnológico

- **Backend**: Spring Boot 3.4.5, Spring Security 6.x
- **Frontend**: Angular 17, RxJS, Angular Guards
- **Seguridad**: JWT (RS256), AES encryption, X.509 certificates
- **Infraestructura**: Docker, Redis (para gestión de tokens)

---

**Nota**: Este proyecto es una prueba de concepto que demuestra patrones de seguridad empresarial. Para uso en producción, asegúrese de adaptar las configuraciones a sus requisitos específicos de seguridad.
