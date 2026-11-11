import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import {
  AdministracionService,
  RegistroBitacora,
  ResenaAdmin,
  RolSpring,
  UsuarioAdmin,
} from '../../servicios/administracion.service';
import { AuthService } from '../../servicios/auth.service';
import { mensajeDeError } from '../../spring/spring-api.service';

type Seccion = 'usuarios' | 'resenas' | 'bitacora';

@Component({
  selector: 'app-administracion',
  templateUrl: './administracion.html',
  styleUrl: './administracion.css',
})
export class Administracion implements OnInit {
  private readonly administracion = inject(AdministracionService);
  private readonly router = inject(Router);
  protected readonly auth = inject(AuthService);

  protected readonly seccion = signal<Seccion>('usuarios');
  protected readonly consulta = signal('');
  protected readonly aviso = signal('');
  protected readonly error = signal('');
  protected readonly cargando = signal(true);
  protected readonly guardando = signal<number | null>(null);

  protected readonly usuarios = signal<UsuarioAdmin[]>([]);
  protected readonly resenas = signal<ResenaAdmin[]>([]);
  protected readonly bitacora = signal<RegistroBitacora[]>([]);

  protected readonly subtitulo = computed(() => {
    switch (this.seccion()) {
      case 'resenas':
        return 'Reseñas escritas por los usuarios en sus listas.';
      case 'bitacora':
        return 'Acciones realizadas por los administradores.';
      default:
        return 'Cuentas y permisos registrados en el servidor.';
    }
  });

  protected readonly contador = computed(() => {
    switch (this.seccion()) {
      case 'resenas':
        return `${this.resenas().length} RESEÑAS`;
      case 'bitacora':
        return `${this.bitacora().length} REGISTROS`;
      default:
        return `${this.usuarios().length} CUENTAS`;
    }
  });

  protected readonly usuariosFiltrados = computed(() => {
    const texto = this.consulta().trim().toLocaleLowerCase();
    return this.usuarios().filter((u) =>
      `${u.nombre} ${u.correo ?? ''}`.toLocaleLowerCase().includes(texto),
    );
  });

  protected readonly resenasFiltradas = computed(() => {
    const texto = this.consulta().trim().toLocaleLowerCase();
    return this.resenas().filter((r) =>
      `${r.usuario} ${r.correo ?? ''} ${r.titulo} ${r.resena}`.toLocaleLowerCase().includes(texto),
    );
  });

  ngOnInit(): void {
    void this.cargar();
  }

  protected async cambiarSeccion(seccion: Seccion): Promise<void> {
    if (this.seccion() === seccion) return;
    this.seccion.set(seccion);
    this.consulta.set('');
    this.aviso.set('');
    await this.cargar();
  }

  protected etiquetaRol(rol: RolSpring | null): string {
    return rol === 'ROLE_ADMIN' ? 'Administrador' : 'Usuario';
  }

  protected esMiCuenta(usuario: UsuarioAdmin): boolean {
    return String(usuario.id) === this.auth.perfil()?.id;
  }

  protected etiquetaTipo(tipo: string): string {
    return tipo === 'serie' ? 'Serie' : 'Película';
  }

  protected fecha(valor: string | null): string {
    if (!valor) return '';
    const d = new Date(valor);
    return Number.isNaN(d.getTime())
      ? ''
      : d.toLocaleString('es-MX', {
          day: 'numeric',
          month: 'short',
          year: 'numeric',
          hour: 'numeric',
          minute: '2-digit',
        });
  }

  protected async cambiarRol(usuario: UsuarioAdmin): Promise<void> {
    const nuevo: RolSpring = usuario.rol === 'ROLE_ADMIN' ? 'ROLE_CLIENTE' : 'ROLE_ADMIN';
    this.guardando.set(usuario.id);
    this.aviso.set('');
    this.error.set('');
    try {
      const actualizado = await firstValueFrom(this.administracion.cambiarRol(usuario.id, nuevo));
      this.usuarios.update((lista) => lista.map((u) => (u.id === actualizado.id ? actualizado : u)));
      this.aviso.set(`Rol actualizado para ${actualizado.nombre}: ahora es ${this.etiquetaRol(actualizado.rol)}.`);
    } catch (e) {
      this.manejarError(e);
    } finally {
      this.guardando.set(null);
    }
  }

  protected async eliminarTexto(resena: ResenaAdmin): Promise<void> {
    if (!window.confirm(`¿Eliminar el texto de la reseña de ${resena.usuario} sobre «${resena.titulo}»?`)) return;
    this.guardando.set(resena.id);
    this.aviso.set('');
    this.error.set('');
    try {
      await firstValueFrom(this.administracion.eliminarTextoResena(resena.id));
      this.resenas.update((lista) => lista.filter((r) => r.id !== resena.id));
      this.aviso.set('Texto de la reseña eliminado. Quedó anotado en la bitácora.');
    } catch (e) {
      this.manejarError(e);
    } finally {
      this.guardando.set(null);
    }
  }

  private async cargar(): Promise<void> {
    this.cargando.set(true);
    this.error.set('');
    try {
      switch (this.seccion()) {
        case 'usuarios':
          this.usuarios.set(await firstValueFrom(this.administracion.obtenerUsuarios()));
          break;
        case 'resenas':
          this.resenas.set(await firstValueFrom(this.administracion.obtenerResenas()));
          break;
        case 'bitacora':
          this.bitacora.set(await firstValueFrom(this.administracion.obtenerBitacora()));
          break;
      }
    } catch (e) {
      this.manejarError(e);
    } finally {
      this.cargando.set(false);
    }
  }

  /** Si Spring dice que ya no eres administrador (cambio en la base), se actualiza la sesión. */
  private manejarError(e: unknown): void {
    this.error.set(mensajeDeError(e));
    if (e instanceof HttpErrorResponse && (e.status === 401 || e.status === 403)) {
      void this.auth.cargarPerfil().then((perfil) => {
        if (perfil?.rolId !== 1) void this.router.navigate(['/buscar']);
      });
    }
  }
}
