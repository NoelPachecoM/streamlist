import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { SpringApiService } from './spring-api.service';

/** Si no hay sesión, manda al login único. */
export const springGuard: CanActivateFn = () => {
  const api = inject(SpringApiService);
  return api.haIniciadoSesion() ? true : inject(Router).createUrlTree(['/login']);
};

/** Catálogos: solo para administradores (ROLE_ADMIN). */
export const springAdminGuard: CanActivateFn = () => {
  const api = inject(SpringApiService);
  const router = inject(Router);
  if (!api.haIniciadoSesion()) return router.createUrlTree(['/login']);
  return api.usuario()?.rol === 'ROLE_ADMIN' ? true : router.createUrlTree(['/buscar']);
};
