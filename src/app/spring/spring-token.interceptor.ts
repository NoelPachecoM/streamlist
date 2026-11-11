import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { SpringApiService } from './spring-api.service';
import { SPRING_URL } from './spring-config';

/**
 * Agrega el token de sesión a cada petición que va a Spring:
 *   Authorization: Bearer <token>
 * Si Spring responde 401 a una petición que llevaba token (venció o se cerró la sesión),
 * se borra la sesión del navegador para que la persona vuelva a iniciar sesión.
 */
export const springTokenInterceptor: HttpInterceptorFn = (peticion, siguiente) => {
  if (!peticion.url.startsWith(SPRING_URL)) return siguiente(peticion);

  const spring = inject(SpringApiService);
  const token = spring.token();
  if (!token) return siguiente(peticion);

  const conToken = peticion.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  return siguiente(conToken).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401 && spring.token() === token) {
        spring.olvidarSesion();
      }
      return throwError(() => error);
    }),
  );
};
