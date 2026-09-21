import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (peticion, siguiente) => {
  const token = inject(AuthService).obtenerToken();
  if (token && peticion.url.startsWith('/api/')) {
    peticion = peticion.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  }
  return siguiente(peticion);
};