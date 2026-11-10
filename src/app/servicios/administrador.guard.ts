import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const administradorGuard: CanActivateFn = async () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.haIniciadoSesion()) return router.createUrlTree(['/login']);
  const perfil = await auth.cargarPerfil();
  return perfil?.rolId === 1 ? true : router.createUrlTree(['/buscar']);
};