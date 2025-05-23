import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth/auth.service';
import { catchError, map } from 'rxjs/operators';
import { of } from 'rxjs';
import Swal from 'sweetalert2';

export const rolGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return authService.verificarSesionActiva().pipe(
    map(activa => {
      if (!activa) {
        Swal.fire({
          icon: 'warning',
          title: 'Sesión expirada',
          text: 'Por seguridad, se ha cerrado tu sesión.',
          confirmButtonText: 'Aceptar',
          allowOutsideClick: false,
          allowEscapeKey: false
        }).then(() => {
          const id = sessionStorage.getItem("idsession");

          if (id) {
            // Intentamos cerrar sesión en el backend para limpiar cookies y revocar tokens
            authService.cerrarSesionYRedirigir();
          } else {
            // Si no hay idsession, simplemente limpiamos y redirigimos
            sessionStorage.clear();
            router.navigate(['/home']);
          }
        });

        return false;
      }

      const userStr = sessionStorage.getItem('userdetails');
      if (!userStr) {
        router.navigate(['/forbidden']);
        return false;
      }

      const user = JSON.parse(userStr);
      const role = user?.role?.toLowerCase();

      if (role !== 'admin') {
        console.warn('[Guard] Acceso denegado por rol');
        router.navigate(['/forbidden']);
        return false;
      }

      console.log('[Guard] Acceso permitido');
      return true;
    }),
    catchError(() => {
      authService.cerrarSesionYRedirigir();
      return of(false);
    })
  );
};
