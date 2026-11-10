import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AdministracionService, RolId, UsuarioAdmin } from '../../servicios/administracion.service';
import { AuthService } from '../../servicios/auth.service';

@Component({
  selector: 'app-administracion',
  templateUrl: './administracion.html',
  styleUrl: './administracion.css',
})
export class Administracion implements OnInit {
  private readonly administracion = inject(AdministracionService);
  protected readonly auth = inject(AuthService);
  protected readonly consulta = signal('');
  protected readonly aviso = signal('');
  protected readonly usuarios = signal<UsuarioAdmin[]>([]);
  protected readonly cargando = signal(true);
  protected readonly guardando = signal<string | null>(null);
  protected readonly error = signal('');

  protected readonly usuariosFiltrados = computed(() => {
    const texto = this.consulta().trim().toLocaleLowerCase();
    return this.usuarios().filter((usuario) => {
      return `${usuario.nombre} ${usuario.correo ?? ''}`.toLocaleLowerCase().includes(texto);
    });
  });

  ngOnInit(): void {
    void this.cargarUsuarios();
  }

  protected etiquetaRol(rolId: RolId): string {
    return rolId === 1 ? 'Administrador' : 'Usuario';
  }

  protected async cambiarRol(usuario: UsuarioAdmin): Promise<void> {
    const rolId: RolId = usuario.rolId === 1 ? 2 : 1;
    this.guardando.set(usuario.id);
    this.aviso.set('');
    this.error.set('');
    try {
      const actualizado = await firstValueFrom(this.administracion.cambiarRol(usuario.id, rolId));
      this.usuarios.update((usuarios) =>
        usuarios.map((actual) => (actual.id === actualizado.id ? actualizado : actual)),
      );
      this.aviso.set(`Rol actualizado para ${actualizado.nombre}.`);
    } catch {
      this.error.set('No se pudo actualizar el rol. Verifica tu sesión e inténtalo de nuevo.');
    } finally {
      this.guardando.set(null);
    }
  }

  protected esMiCuenta(usuario: UsuarioAdmin): boolean {
    return usuario.id === this.auth.perfil()?.id;
  }

  private async cargarUsuarios(): Promise<void> {
    this.cargando.set(true);
    this.error.set('');
    try {
      this.usuarios.set(await firstValueFrom(this.administracion.obtenerUsuarios()));
    } catch {
      this.error.set('No se pudieron cargar las cuentas reales. Verifica permisos y conexión.');
    } finally {
      this.cargando.set(false);
    }
  }
}
