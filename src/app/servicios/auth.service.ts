import { Injectable, computed, inject } from '@angular/core';
import { SpringApiService, mensajeDeError } from '../spring/spring-api.service';

/**
 * Único servicio de sesión de la aplicación. Toda la sesión vive en Spring
 * (SpringApiService); este servicio solo conserva la interfaz que ya usaban
 * Mi lista, Detalle, Buscar, Administración y el encabezado.
 *
 * El código del login original (Supabase y Google) quedó guardado en
 * auth-supabase.service.ts, sin usar.
 */

export interface UsuarioSesion {
  nombre: string;
  correo: string;
  foto?: string;
}

export interface PerfilSesion {
  id: string;
  nombre: string;
  correo: string | null;
  rolId: 1 | 2;
}

export interface ResultadoAcceso {
  ok: boolean;
  mensaje: string;
}

export interface ResultadoSolicitud extends ResultadoAcceso {
  solicitudId?: number;
}

export interface DatosRegistro {
  nombre: string;
  correo: string;
  contrasena: string;
  telefono?: string;
}

const ROL_ADMIN = 'ROLE_ADMIN';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly spring = inject(SpringApiService);

  /**
   * Al abrir la página se vuelve a leer el usuario desde Spring, para que un cambio de rol
   * hecho en la base (pgAdmin) o en Administración se note sin cerrar sesión.
   */
  readonly listo: Promise<void> = this.spring.refrescarSesion().then(() => undefined);

  readonly usuario = computed<UsuarioSesion | null>(() => {
    const u = this.spring.usuario();
    return u ? { nombre: u.nombre, correo: u.correo } : null;
  });

  readonly haIniciadoSesion = computed(() => this.usuario() !== null);

  readonly perfil = computed<PerfilSesion | null>(() => {
    const u = this.spring.usuario();
    if (!u) return null;
    return {
      id: String(u.id),
      nombre: u.nombre,
      correo: u.correo,
      rolId: u.rol === ROL_ADMIN ? 1 : 2,
    };
  });

  readonly esAdministrador = computed(() => this.spring.usuario()?.rol === ROL_ADMIN);

  /** Token de sesión de Spring (lo agrega spring-token.interceptor a cada petición). */
  obtenerToken(): string | null {
    return this.spring.token();
  }

  /** Lee el rol actual desde Spring (lo usa el guardia de Administración). */
  async cargarPerfil(): Promise<PerfilSesion | null> {
    await this.spring.refrescarSesion();
    return this.perfil();
  }

  async iniciarSesion(correo: string, contrasena: string): Promise<ResultadoAcceso> {
    try {
      await this.spring.iniciarSesion(correo.trim(), contrasena);
      return { ok: true, mensaje: 'Sesión iniciada.' };
    } catch (e) {
      return { ok: false, mensaje: mensajeDeError(e) };
    }
  }

  /** Paso 1 del registro: manda el código al correo. */
  async solicitarRegistro(datos: DatosRegistro): Promise<ResultadoSolicitud> {
    try {
      const r = await this.spring.solicitarRegistro({
        nombre: datos.nombre.trim(),
        correo: datos.correo.trim(),
        password: datos.contrasena,
        telefono: datos.telefono?.trim() || undefined,
      });
      return { ok: true, mensaje: r.mensaje, solicitudId: r.solicitudId };
    } catch (e) {
      return { ok: false, mensaje: mensajeDeError(e) };
    }
  }

  async reenviarRegistro(solicitudId: number): Promise<ResultadoSolicitud> {
    try {
      const r = await this.spring.reenviarRegistro(solicitudId);
      return { ok: true, mensaje: r.mensaje, solicitudId: r.solicitudId };
    } catch (e) {
      return { ok: false, mensaje: mensajeDeError(e) };
    }
  }

  /** Paso 2: con el código correcto se crea la cuenta y se inicia sesión. */
  async confirmarRegistro(solicitudId: number, codigo: string): Promise<ResultadoAcceso> {
    try {
      await this.spring.confirmarRegistro(solicitudId, codigo.trim());
      return { ok: true, mensaje: '¡Cuenta creada! Ya iniciaste sesión.' };
    } catch (e) {
      return { ok: false, mensaje: mensajeDeError(e) };
    }
  }

  cerrarSesion(): void {
    this.spring.cerrarSesion();
  }
}
