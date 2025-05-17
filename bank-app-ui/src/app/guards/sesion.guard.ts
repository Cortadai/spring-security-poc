import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth/auth.service';
import { catchError, map } from 'rxjs/operators';
import { of } from 'rxjs';
import Swal from 'sweetalert2';

export const sesionGuard: CanActivateFn = (route, state) => {
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
            authService.logout(id).subscribe({
              next: () => {
                sessionStorage.clear();
                router.navigate(['/home']);
              },
              error: () => {
                sessionStorage.clear();
                router.navigate(['/home']);
              }
            });
          } else {
            // Si no hay idsession, simplemente limpiamos y redirigimos
            sessionStorage.clear();
            router.navigate(['/home']);
          }
        });

        return false;
      }

      console.log('[Guard] Sesión activa, acceso permitido');
      return true;
    }),
    catchError(() => {
      sessionStorage.clear();
      router.navigate(['/home']);
      return of(false);
    })
  );
};
