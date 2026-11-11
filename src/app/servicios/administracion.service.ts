import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { SpringApiService } from '../spring/spring-api.service';
import { SPRING_URL } from '../spring/spring-config';

export type RolSpring = 'ROLE_ADMIN' | 'ROLE_CLIENTE';

export interface UsuarioAdmin {
  id: number;
  nombre: string;
  correo: string | null;
  rol: RolSpring | null;
  activo: boolean;
}

export interface ResenaAdmin {
  id: number;
  usuarioId: number;
  usuario: string;
  correo: string | null;
  titulo: string;
  tipo: 'pelicula' | 'serie' | string;
  tmdbId: number;
  calificacion: number | null;
  resena: string;
  fecha: string | null;
}

export interface RegistroBitacora {
  id: number;
  fecha: string | null;
  administrador: string;
  accion: string;
  detalle: string;
}

/** Panel de Administración sobre Spring (/api/admin). Spring revisa en la base que seas ROLE_ADMIN. */
@Injectable({ providedIn: 'root' })
export class AdministracionService {
  private readonly http = inject(HttpClient);
  private readonly spring = inject(SpringApiService);
  private readonly base = `${SPRING_URL}/api/admin`;

  // Spring sabe quién es el administrador por el token de sesión (no se manda el adminId).

  obtenerUsuarios(): Observable<UsuarioAdmin[]> {
    if (!this.haySesion()) return this.sinSesion();
    return this.http.get<UsuarioAdmin[]>(`${this.base}/usuarios`);
  }

  cambiarRol(id: number, rol: RolSpring): Observable<UsuarioAdmin> {
    if (!this.haySesion()) return this.sinSesion();
    return this.http.put<UsuarioAdmin>(`${this.base}/usuarios/${id}/rol`, { rol });
  }

  obtenerResenas(): Observable<ResenaAdmin[]> {
    if (!this.haySesion()) return this.sinSesion();
    return this.http.get<ResenaAdmin[]>(`${this.base}/resenas`);
  }

  eliminarTextoResena(id: number): Observable<void> {
    if (!this.haySesion()) return this.sinSesion();
    return this.http.post<void>(`${this.base}/resenas/${id}/eliminar-texto`, {});
  }

  obtenerBitacora(): Observable<RegistroBitacora[]> {
    if (!this.haySesion()) return this.sinSesion();
    return this.http.get<RegistroBitacora[]>(`${this.base}/bitacora`);
  }

  private haySesion(): boolean {
    return this.spring.token() !== null;
  }

  private sinSesion(): Observable<never> {
    return throwError(() => new Error('Inicia sesión como administrador.'));
  }
}
