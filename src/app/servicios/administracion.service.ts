import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export type RolId = 1 | 2;

export interface UsuarioAdmin {
  id: string;
  nombre: string;
  correo: string | null;
  rolId: RolId;
}

export interface ResenaAdmin {
  id: string;
  usuarioId: string;
  usuarioNombre: string;
  usuarioCorreo: string | null;
  tmdbId: number;
  tipo: string;
  titulo: string;
  calificacion: number | null;
  resena: string;
  creadoEn: string;
  actualizadoEn: string;
}

export interface RegistroBitacora {
  id: number;
  adminId: string;
  adminNombre: string;
  accion: string;
  entidad: string;
  entidadId: string | null;
  detalle: Record<string, unknown> | null;
  creadoEn: string;
}

@Injectable({ providedIn: 'root' })
export class AdministracionService {
  private readonly http = inject(HttpClient);

  obtenerUsuarios(): Observable<UsuarioAdmin[]> {
    return this.http.get<UsuarioAdmin[]>('/api/admin/usuarios');
  }

  cambiarRol(id: string, rolId: RolId): Observable<UsuarioAdmin> {
    return this.http.patch<UsuarioAdmin>(`/api/admin/usuarios?id=${encodeURIComponent(id)}`, { rolId });
  }

  obtenerResenas(): Observable<ResenaAdmin[]> {
    return this.http.get<ResenaAdmin[]>('/api/admin/resenas');
  }

  eliminarResena(id: string): Observable<{ id: string }> {
    return this.http.delete<{ id: string }>(`/api/admin/resenas?id=${encodeURIComponent(id)}`);
  }

  obtenerBitacora(): Observable<RegistroBitacora[]> {
    return this.http.get<RegistroBitacora[]>('/api/admin/bitacora');
  }
}
