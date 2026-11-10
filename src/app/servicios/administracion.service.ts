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

@Injectable({ providedIn: 'root' })
export class AdministracionService {
  private readonly http = inject(HttpClient);

  obtenerUsuarios(): Observable<UsuarioAdmin[]> {
    return this.http.get<UsuarioAdmin[]>('/api/admin/usuarios');
  }

  cambiarRol(id: string, rolId: RolId): Observable<UsuarioAdmin> {
    return this.http.patch<UsuarioAdmin>(`/api/admin/usuarios?id=${encodeURIComponent(id)}`, { rolId });
  }
}