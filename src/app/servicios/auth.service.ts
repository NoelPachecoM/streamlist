import { Injectable, computed, signal } from '@angular/core';
import { createClient } from '@supabase/supabase-js';
import type { SupabaseClient } from '@supabase/supabase-js';
import { GOOGLE_CLIENT_ID, SUPABASE_PUBLISHABLE_KEY, SUPABASE_URL } from '../config';

declare const google: any;

export interface UsuarioSesion {
  nombre: string;
  correo: string;
  foto?: string;
}

export interface ResultadoAcceso {
  ok: boolean;
  mensaje: string;
}

export interface DatosRegistro {
  nombre: string;
  apellidos: string;
  correo: string;
  contrasena: string;
}

type Origen = 'google' | 'supabase';

const CLAVE_TOKEN = 'streamlist_token';

const SUPABASE_CONFIGURADO =
  !!SUPABASE_URL && !!SUPABASE_PUBLISHABLE_KEY && !SUPABASE_URL.includes('TU-PROYECTO');

const MENSAJE_GENERICO = 'No se pudo completar la operación. Intenta de nuevo.';

function traducirError(mensaje: string): string {
  const t = mensaje.toLowerCase();
  if (t.includes('invalid login credentials')) return 'Correo o contraseña incorrectos.';
  if (t.includes('email not confirmed')) return 'Confirma tu correo antes de iniciar sesión.';
  if (t.includes('already registered')) return 'Ya existe una cuenta con ese correo.';
  if (t.includes('rate limit') || t.includes('too many')) {
    return 'Demasiados intentos. Espera un momento e inténtalo de nuevo.';
  }
  if (t.includes('password should contain') || t.includes('weak_password') || t.includes('weak password')) {
    return 'La contraseña es muy débil. Usa mayúscula, minúscula y número.';
  }
  if (t.includes('password') && t.includes('characters')) {
    return 'La contraseña es muy corta. Usa al menos 8 caracteres.';
  }
  if (t.includes('email') && t.includes('invalid')) return 'El correo no es válido.';
  if (t.includes('signups not allowed') || t.includes('signup is disabled')) {
    return 'El registro de cuentas está deshabilitado.';
  }
  return MENSAJE_GENERICO;
}

function decodificar(token: string): any | null {
  try {
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    const json = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join(''),
    );
    return JSON.parse(json);
  } catch {
    return null;
  }
}

function estaVigente(token: string): boolean {
  const datos = decodificar(token);
  return !!datos?.exp && datos.exp * 1000 > Date.now();
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly _token = signal<string | null>(null);
  private temporizador?: ReturnType<typeof setTimeout>;
  private googleInicializado = false;
  private origen: Origen | null = null;
  private resolverListo!: () => void;

  /**
   * Se cumple cuando ya se restauró la sesión guardada (si la hay). Las páginas que
   * piden datos del usuario lo esperan para no decidir "no hay sesión" demasiado pronto.
   */
  readonly listo: Promise<void> = new Promise<void>((resolver) => (this.resolverListo = resolver));

  /** Cliente de Supabase Auth (cuentas de correo y contraseña). Null si no está configurado. */
  private readonly supabase: SupabaseClient | null = SUPABASE_CONFIGURADO
    ? createClient(SUPABASE_URL, SUPABASE_PUBLISHABLE_KEY, {
        auth: { persistSession: true, autoRefreshToken: true, detectSessionInUrl: false },
      })
    : null;

  readonly usuario = computed<UsuarioSesion | null>(() => {
    const token = this._token();
    if (!token) return null;
    const datos = decodificar(token);
    if (!datos) return null;
    const meta = datos.user_metadata ?? {};
    const nombreCuenta = [meta.nombre, meta.apellidos].filter(Boolean).join(' ').trim();
    return {
      nombre: datos.name ?? (nombreCuenta || datos.email),
      correo: datos.email,
      foto: datos.picture,
    };
  });

  readonly haIniciadoSesion = computed(() => this.usuario() !== null);

  constructor() {
    const guardado = localStorage.getItem(CLAVE_TOKEN);
    if (guardado && estaVigente(guardado)) {
      this.activarSesion(guardado, 'google');
    } else {
      localStorage.removeItem(CLAVE_TOKEN);
    }
    this.escucharSupabase();
    if (!this.supabase) this.resolverListo();
    // Seguro de vida: nunca dejar a la interfaz esperando indefinidamente.
    setTimeout(() => this.resolverListo(), 4000);
  }

  /** Devuelve el token si sigue vigente; si venció, cierra la sesión (Google) o espera la renovación (Supabase). */
  obtenerToken(): string | null {
    const token = this._token();
    if (token && !estaVigente(token)) {
      // Los tokens de Supabase se renuevan solos con su refresh token.
      if (this.origen === 'google') this.cerrarSesion();
      return null;
    }
    return token;
  }

  /** Crea una cuenta con correo y contraseña. */
  async registrarse(datos: DatosRegistro): Promise<ResultadoAcceso> {
    if (!this.supabase) return { ok: false, mensaje: 'Falta configurar Supabase en config.ts.' };
    try {
      const { data, error } = await this.supabase.auth.signUp({
        email: datos.correo.trim(),
        password: datos.contrasena,
        options: {
          data: { nombre: datos.nombre.trim(), apellidos: datos.apellidos.trim() },
          emailRedirectTo: `${window.location.origin}/login`,
        },
      });
      if (error) return { ok: false, mensaje: traducirError(error.message) };

      // Con confirmación de correo activada, un correo ya registrado no da error:
      // Supabase devuelve un usuario "falso" sin identidades.
      if (data.user && data.user.identities?.length === 0) {
        return { ok: false, mensaje: 'Ya existe una cuenta con ese correo.' };
      }
      if (!data.session) {
        return {
          ok: true,
          mensaje: 'Cuenta creada. Revisa tu correo para confirmarla y después inicia sesión.',
        };
      }
      return { ok: true, mensaje: '¡Cuenta creada! Ya iniciaste sesión.' };
    } catch {
      return { ok: false, mensaje: MENSAJE_GENERICO };
    }
  }

  /** Inicia sesión con correo y contraseña. */
  async iniciarSesion(correo: string, contrasena: string): Promise<ResultadoAcceso> {
    if (!this.supabase) return { ok: false, mensaje: 'Falta configurar Supabase en config.ts.' };
    try {
      const { error } = await this.supabase.auth.signInWithPassword({
        email: correo.trim(),
        password: contrasena,
      });
      if (error) return { ok: false, mensaje: traducirError(error.message) };
      return { ok: true, mensaje: 'Sesión iniciada.' };
    } catch {
      return { ok: false, mensaje: MENSAJE_GENERICO };
    }
  }

  /** Dibuja el botón oficial "Iniciar sesión con Google" dentro del elemento dado. */
  mostrarBotonGoogle(contenedor: HTMLElement): void {
    const renderizar = () => {
      if (typeof google === 'undefined' || !google.accounts?.id) {
        return;
      }

      contenedor.innerHTML = '';

      if (!this.googleInicializado) {
        google.accounts.id.initialize({
          client_id: GOOGLE_CLIENT_ID,
          callback: (respuesta: { credential: string }) => this.guardarSesion(respuesta.credential),
          auto_select: false,
          cancel_on_tap_outside: true,
        });
        this.googleInicializado = true;
      }

      google.accounts.id.renderButton(contenedor, {
        theme: 'outline',
        size: 'large',
        shape: 'pill',
        text: 'signin_with',
        locale: 'es',
        width: Math.max(contenedor.clientWidth || 280, 220),
      });
    };

    if (typeof google !== 'undefined' && google.accounts?.id) {
      renderizar();
      return;
    }

    this.esperarGoogle().then(renderizar).catch(() => {
      console.warn('Google SDK no disponible en este momento.');
    });
  }

  cerrarSesion(): void {
    const eraSupabase = this.origen === 'supabase';
    clearTimeout(this.temporizador);
    localStorage.removeItem(CLAVE_TOKEN);
    this._token.set(null);
    this.origen = null;
    if (eraSupabase) void this.supabase?.auth.signOut();
    if (typeof google !== 'undefined' && google.accounts?.id) {
      google.accounts.id.disableAutoSelect();
    }
  }

  private guardarSesion(token: string): void {
    if (!estaVigente(token)) return;
    // Si había una sesión de correo/contraseña, se descarta (solo una sesión a la vez).
    if (this.origen === 'supabase') void this.supabase?.auth.signOut({ scope: 'local' });
    localStorage.setItem(CLAVE_TOKEN, token);
    this.activarSesion(token, 'google');
  }

  private activarSesion(token: string, origen: Origen): void {
    this._token.set(token);
    this.origen = origen;
    clearTimeout(this.temporizador);
    // Google: no hay renovación, se cierra al vencer. Supabase: se renueva solo.
    if (origen === 'google') {
      const msRestantes = decodificar(token).exp * 1000 - Date.now();
      this.temporizador = setTimeout(() => this.cerrarSesion(), msRestantes);
    }
  }

  /** Mantiene sincronizado el token con la sesión de Supabase (inicio, renovación y cierre). */
  private escucharSupabase(): void {
    this.supabase?.auth.onAuthStateChange((evento, sesion) => {
      // El primer aviso (INITIAL_SESSION) indica que la sesión guardada ya se restauró.
      this.resolverListo();
      if (sesion) {
        // Si ya hay una sesión de Google restaurada, no la pisa al arrancar.
        if (evento === 'INITIAL_SESSION' && this.origen === 'google') return;
        this.activarSesion(sesion.access_token, 'supabase');
      } else if (this.origen === 'supabase') {
        this._token.set(null);
        this.origen = null;
      }
    });
  }

  private esperarGoogle(): Promise<void> {
    return new Promise((resolver, rechazar) => {
      const maxMs = 10000;
      const inicio = Date.now();
      const intervalo = setInterval(() => {
        if (typeof google !== 'undefined' && google.accounts?.id) {
          clearInterval(intervalo);
          resolver();
          return;
        }

        if (Date.now() - inicio >= maxMs) {
          clearInterval(intervalo);
          rechazar(new Error('Google SDK no cargó a tiempo'));
        }
      }, 100);
    });
  }
}