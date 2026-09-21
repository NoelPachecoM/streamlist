import { Injectable, computed, signal } from '@angular/core';
import { GOOGLE_CLIENT_ID } from '../config';

declare const google: any;

export interface UsuarioSesion {
  nombre: string;
  correo: string;
  foto?: string;
}

const CLAVE_TOKEN = 'streamlist_token';

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

  readonly usuario = computed<UsuarioSesion | null>(() => {
    const token = this._token();
    if (!token) return null;
    const datos = decodificar(token);
    if (!datos) return null;
    return { nombre: datos.name ?? datos.email, correo: datos.email, foto: datos.picture };
  });

  readonly haIniciadoSesion = computed(() => this.usuario() !== null);

  constructor() {
    const guardado = localStorage.getItem(CLAVE_TOKEN);
    if (guardado && estaVigente(guardado)) {
      this.activarSesion(guardado);
    } else {
      localStorage.removeItem(CLAVE_TOKEN);
    }
  }

  /** Devuelve el token si sigue vigente; si venció, cierra la sesión. */
  obtenerToken(): string | null {
    const token = this._token();
    if (token && !estaVigente(token)) {
      this.cerrarSesion();
      return null;
    }
    return token;
  }

  /** Dibuja el botón oficial "Iniciar sesión con Google" dentro del elemento dado. */
  mostrarBotonGoogle(contenedor: HTMLElement): void {
    this.esperarGoogle().then(() => {
      google.accounts.id.initialize({
        client_id: GOOGLE_CLIENT_ID,
        callback: (respuesta: { credential: string }) => this.guardarSesion(respuesta.credential),
      });
      google.accounts.id.renderButton(contenedor, {
        theme: 'outline',
        size: 'large',
        shape: 'pill',
        text: 'signin_with',
        locale: 'es',
      });
    });
  }

  cerrarSesion(): void {
    clearTimeout(this.temporizador);
    localStorage.removeItem(CLAVE_TOKEN);
    this._token.set(null);
    if (typeof google !== 'undefined' && google.accounts?.id) {
      google.accounts.id.disableAutoSelect();
    }
  }

  private guardarSesion(token: string): void {
    if (!estaVigente(token)) return;
    localStorage.setItem(CLAVE_TOKEN, token);
    this.activarSesion(token);
  }

  private activarSesion(token: string): void {
    this._token.set(token);
    clearTimeout(this.temporizador);
    const msRestantes = decodificar(token).exp * 1000 - Date.now();
    this.temporizador = setTimeout(() => this.cerrarSesion(), msRestantes);
  }

  private esperarGoogle(): Promise<void> {
    return new Promise((resolver) => {
      const intervalo = setInterval(() => {
        if (typeof google !== 'undefined' && google.accounts?.id) {
          clearInterval(intervalo);
          resolver();
        }
      }, 100);
    });
  }
}