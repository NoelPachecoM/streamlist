import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import {
  AdministracionService,
  RegistroBitacora,
  ResenaAdmin,
  RolId,
  UsuarioAdmin,
} from '../../servicios/administracion.service';
import { AuthService } from '../../servicios/auth.service';

type Seccion = 'usuarios' | 'resenas' | 'bitacora';

@Component({
  selector: 'app-administracion',
  templateUrl: './administracion.html',
  styleUrl: './administracion.css',
})
export class Administracion implements OnInit {
  private readonly administracion = inject(AdministracionService);
  protected readonly auth = inject(AuthService);

  protected readonly seccion = signal<Seccion>('usuarios');
  protected readonly consulta = signal('');
  protected readonly consultaResenas = signal('');
  protected readonly aviso = signal('');
  protected readonly error = signal('');
  protected readonly guardando = signal<string | null>(null);

  protected readonly usuarios = signal<UsuarioAdmin[]>([]);
  protected readonly cargando = signal(true);

  protected readonly resenas = signal<ResenaAdmin[]>([]);
  protected readonly cargandoResenas = signal(false);

  protected readonly bitacora = signal<RegistroBitacora[]>([]);
  protected readonly cargandoBitacora = signal(false);

  protected readonly usuariosFiltrados = computed(() => {
    const texto = this.consulta().trim().toLocaleLowerCase();
    return this.usuarios().filter((usuario) => {
      return `${usuario.nombre} ${usuario.correo ?? ''}`.toLocaleLowerCase().includes(texto);
    });
  });

  protected readonly resenasFiltradas = computed(() => {
    const texto = this.consultaResenas().trim().toLocaleLowerCase();
    return this.resenas().filter((resena) => {
      return `${resena.usuarioNombre} ${resena.usuarioCorreo ?? ''} ${resena.titulo} ${resena.resena}`
        .toLocaleLowerCase()
        .includes(texto);
    });
  });

  protected readonly resumen = computed(() => {
    switch (this.seccion()) {
      case 'resenas':
        return {
          subtitulo: 'Reseñas escritas por los usuarios en sus listas.',
          conteo: `${this.resenas().length} RESEÑAS`,
        };
      case 'bitacora':
        return {
          subtitulo: 'Acciones realizadas por los administradores.',
          conteo: `${this.bitacora().length} REGISTROS`,
        };
      default:
        return {
          subtitulo: 'Cuentas y permisos registrados en el servidor.',
          conteo: `${this.usuarios().length} CUENTAS`,
        };
    }
  });

  ngOnInit(): void {
    void this.cargarUsuarios();
  }

  protected abrirSeccion(seccion: Seccion): void {
    this.seccion.set(seccion);
    this.aviso.set('');
    this.error.set('');
    if (seccion === 'resenas') void this.cargarResenas();
    if (seccion === 'bitacora') void this.cargarBitacora();
  }

  protected etiquetaRol(rolId: RolId): string {
    return rolId === 1 ? 'Administrador' : 'Usuario';
  }

  protected etiquetaTipo(tipo: string): string {
    return tipo === 'pelicula' ? 'Película' : tipo === 'serie' ? 'Serie' : tipo;
  }

  protected etiquetaAccion(accion: string): string {
    switch (accion) {
      case 'rol_cambiado':
        return 'Cambio de rol';
      case 'resena_eliminada':
        return 'Reseña eliminada';
      default:
        return accion;
    }
  }

  protected describir(registro: RegistroBitacora): string {
    const detalle = registro.detalle ?? {};
    if (registro.accion === 'rol_cambiado') {
      const nombre = String(detalle['nombre'] ?? registro.entidadId ?? 'una cuenta');
      const anterior = this.etiquetaRol(detalle['rolAnterior'] as RolId);
      const nuevo = this.etiquetaRol(detalle['rolNuevo'] as RolId);
      return `Cambió el rol de ${nombre}: ${anterior} → ${nuevo}.`;
    }
    if (registro.accion === 'resena_eliminada') {
      const usuario = String(detalle['usuario'] ?? 'un usuario');
      const titulo = String(detalle['titulo'] ?? 'un título');
      const texto = String(detalle['textoEliminado'] ?? '');
      return `Eliminó la reseña de ${usuario} sobre "${titulo}". Texto: "${texto}".`;
    }
    return `${registro.accion} en ${registro.entidad}${registro.entidadId ? ` (${registro.entidadId})` : ''}.`;
  }

  protected formatearFecha(fecha: string): string {
    return new Date(fecha).toLocaleString('es-MX', { dateStyle: 'medium', timeStyle: 'short' });
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

  protected async eliminarResena(resena: ResenaAdmin): Promise<void> {
    const confirmado = confirm(
      `¿Eliminar el texto de la reseña de ${resena.usuarioNombre} sobre "${resena.titulo}"? La calificación se conserva.`,
    );
    if (!confirmado) return;

    this.guardando.set(resena.id);
    this.aviso.set('');
    this.error.set('');
    try {
      await firstValueFrom(this.administracion.eliminarResena(resena.id));
      this.resenas.update((resenas) => resenas.filter((actual) => actual.id !== resena.id));
      this.aviso.set('Reseña eliminada y registrada en la bitácora.');
    } catch {
      this.error.set('No se pudo eliminar la reseña. Verifica tu sesión e inténtalo de nuevo.');
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

  private async cargarResenas(): Promise<void> {
    this.cargandoResenas.set(true);
    try {
      this.resenas.set(await firstValueFrom(this.administracion.obtenerResenas()));
    } catch {
      this.error.set('No se pudieron cargar las reseñas. Verifica permisos y conexión.');
    } finally {
      this.cargandoResenas.set(false);
    }
  }

  private async cargarBitacora(): Promise<void> {
    this.cargandoBitacora.set(true);
    try {
      this.bitacora.set(await firstValueFrom(this.administracion.obtenerBitacora()));
    } catch {
      this.error.set('No se pudo cargar la bitácora. Verifica que la tabla exista y que tengas permisos.');
    } finally {
      this.cargandoBitacora.set(false);
    }
  }
}
