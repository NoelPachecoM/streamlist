import { Component, effect, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../servicios/auth.service';

@Component({
  selector: 'app-encabezado',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './encabezado.html',
  styleUrl: './encabezado.css',
})
export class Encabezado {
  protected readonly auth = inject(AuthService);

  constructor() {
    // Carga el perfil (con el rol) al iniciar sesión para saber si mostrar el enlace de administración.
    effect(() => {
      if (this.auth.haIniciadoSesion()) void this.auth.cargarPerfil();
    });
  }
}
