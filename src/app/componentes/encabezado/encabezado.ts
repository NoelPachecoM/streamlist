import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../servicios/auth.service';

@Component({
  selector: 'app-encabezado',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './encabezado.html',
  styleUrl: './encabezado.css',
})
export class Encabezado {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  /** Menú del celular (en computadora siempre se ve). */
  protected readonly menuAbierto = signal(false);

  protected inicial(nombre: string): string {
    return (nombre.trim()[0] ?? '?').toUpperCase();
  }

  protected cerrarMenu(): void {
    this.menuAbierto.set(false);
  }

  protected cerrarSesion(): void {
    this.cerrarMenu();
    this.auth.cerrarSesion();
    void this.router.navigate(['/buscar']);
  }
}
