import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { SPRING_URL } from './spring-config';

export interface UsuarioSpring {
  id: number;
  nombre: string;
  correo: string;
  telefono: string | null;
  rol: string | null;
  suscripcion: string;
  /** Token de sesión que da Spring al iniciar sesión. Se manda en cada petición (spring-token.interceptor). */
  token?: string;
}

export interface DatosArco {
  id: number;
  nombre: string;
  correo: string;
  telefono: string | null;
  suscripcion: string;
}

export interface RespuestaArco {
  mensaje: string;
  solicitudId?: number;
  codigoDesarrollo?: string;
}

export interface PaginaSpring {
  content: Record<string, any>[];
  totalPages: number;
  totalElements: number;
  number: number;
}

export interface DatosRegistroSpring {
  nombre: string;
  correo: string;
  password: string;
  telefono?: string;
}

const CLAVE_SESION = 'streamlist_spring_usuario';

/** Convierte cualquier error de HTTP en un texto para mostrar en pantalla. */
export function mensajeDeError(e: unknown): string {
  if (e instanceof HttpErrorResponse) {
    if (e.status === 0) {
      return 'No se pudo conectar con Spring. Revisa que la aplicación esté iniciada.';
    }
    const cuerpo = e.error;
    if (cuerpo && typeof cuerpo === 'object' && typeof cuerpo.error === 'string') return cuerpo.error;
    if (typeof cuerpo === 'string' && cuerpo.trim()) return cuerpo;
    return `Error ${e.status}`;
  }
  if (e instanceof Error && e.message) return e.message;
  return 'Ocurrió un error inesperado.';
}

@Injectable({ providedIn: 'root' })
export class SpringApiService {
  private readonly http = inject(HttpClient);
  private readonly _usuario = signal<UsuarioSpring | null>(this.leerSesion());

  readonly usuario = this._usuario.asReadonly();
  readonly haIniciadoSesion = computed(() => this._usuario() !== null);

  // ---------------- Sesion (los datos del usuario, nunca la password) ----------------

  async iniciarSesion(correo: string, password: string): Promise<UsuarioSpring> {
    const usuario = await firstValueFrom(
      this.http.post<UsuarioSpring>(this.url('/api/auth/login'), { correo, password }),
    );
    this.guardarSesion(usuario);
    return usuario;
  }

  /** Paso 1 del registro: Spring manda un código al correo. Todavía no se crea la cuenta. */
  solicitarRegistro(datos: DatosRegistroSpring): Promise<RespuestaArco> {
    return firstValueFrom(
      this.http.post<RespuestaArco>(this.url('/api/auth/registro/solicitar'), datos),
    );
  }

  /** Reenvía el código (Spring pide esperar 60 segundos entre envíos). */
  reenviarRegistro(solicitudId: number): Promise<RespuestaArco> {
    return firstValueFrom(
      this.http.post<RespuestaArco>(this.url('/api/auth/registro/reenviar'), { solicitudId }),
    );
  }

  /** Paso 2: con el código correcto Spring crea la cuenta y se inicia sesión. */
  async confirmarRegistro(solicitudId: number, codigo: string): Promise<UsuarioSpring> {
    const usuario = await firstValueFrom(
      this.http.post<UsuarioSpring>(this.url('/api/auth/registro/confirmar'), { solicitudId, codigo }),
    );
    this.guardarSesion(usuario);
    return usuario;
  }

  /** Token de la sesión actual (o null). */
  token(): string | null {
    return this._usuario()?.token ?? null;
  }

  /**
   * Vuelve a leer de Spring los datos del usuario del token (sobre todo el rol), por si se
   * cambiaron en la base. Si el token ya no sirve (venció o se cerró), se cierra la sesión.
   * Si Spring no responde, se queda con lo guardado.
   */
  async refrescarSesion(): Promise<UsuarioSpring | null> {
    const actual = this._usuario();
    if (!actual) return null;
    if (!actual.token) {
      // Sesión guardada antes de que existieran los tokens: hay que volver a entrar.
      this.olvidarSesion();
      return null;
    }
    try {
      const usuario = await firstValueFrom(this.http.get<UsuarioSpring>(this.url('/api/auth/sesion')));
      const completo = { ...usuario, token: actual.token };
      this.guardarSesion(completo);
      return completo;
    } catch (e) {
      if (e instanceof HttpErrorResponse && e.status === 401) {
        this.olvidarSesion();
        return null;
      }
      return actual;
    }
  }

  /** Cierra la sesión en Spring (borra el token de la base) y en el navegador. */
  cerrarSesion(): void {
    if (this.token()) {
      // No hace falta esperar la respuesta
      this.http.post(this.url('/api/auth/logout'), {}).subscribe({ error: () => undefined });
    }
    this.olvidarSesion();
  }

  /** Borra la sesión solo del navegador (por ejemplo, cuando Spring dice que el token ya no sirve). */
  olvidarSesion(): void {
    try {
      localStorage.removeItem(CLAVE_SESION);
    } catch {
      /* sin almacenamiento disponible */
    }
    this._usuario.set(null);
  }

  actualizarSesion(cambios: Partial<Omit<UsuarioSpring, 'token'>>): void {
    const actual = this._usuario();
    if (actual) this.guardarSesion({ ...actual, ...cambios });
  }

  // ---------------- ARCO ----------------

  /** Mis datos actuales, leídos de Spring con el token de sesión (sin mandar el correo en la dirección). */
  datos(): Promise<DatosArco> {
    return firstValueFrom(this.http.get<DatosArco>(this.url('/api/auth/sesion')));
  }

  /** ruta: cambiar-nombre, cambiar-telefono, cambiar-correo, cambiar-password,
   *  olvide-password/solicitar, olvide-password/confirmar, cancelar/solicitar, cancelar/confirmar */
  arco(ruta: string, cuerpo: Record<string, unknown>): Promise<RespuestaArco> {
    return firstValueFrom(this.http.post<RespuestaArco>(this.url(`/api/arco/${ruta}`), cuerpo));
  }

  // ---------------- CRUD generico de los catalogos ----------------

  listar(ruta: string, pagina: number, tamano: number, q?: string): Promise<PaginaSpring> {
    let params = new HttpParams().set('page', pagina).set('size', tamano);
    if (q && q.trim()) params = params.set('q', q.trim());
    return firstValueFrom(this.http.get<PaginaSpring>(this.url(`/api/${ruta}`), { params }));
  }

  crear(ruta: string, cuerpo: Record<string, unknown>): Promise<unknown> {
    return firstValueFrom(this.http.post(this.url(`/api/${ruta}`), cuerpo));
  }

  actualizar(ruta: string, id: number, cuerpo: Record<string, unknown>): Promise<unknown> {
    return firstValueFrom(this.http.put(this.url(`/api/${ruta}/${id}`), cuerpo));
  }

  eliminar(ruta: string, id: number): Promise<unknown> {
    return firstValueFrom(this.http.delete(this.url(`/api/${ruta}/${id}`)));
  }

  // ---------------- Privado ----------------

  private url(ruta: string): string {
    return `${SPRING_URL}${ruta}`;
  }

  private guardarSesion(usuario: UsuarioSpring): void {
    try {
      localStorage.setItem(CLAVE_SESION, JSON.stringify(usuario));
    } catch {
      /* sin almacenamiento disponible */
    }
    this._usuario.set(usuario);
  }

  private leerSesion(): UsuarioSpring | null {
    try {
      const texto = localStorage.getItem(CLAVE_SESION);
      return texto ? (JSON.parse(texto) as UsuarioSpring) : null;
    } catch {
      return null;
    }
  }
}
