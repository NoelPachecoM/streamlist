import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { EstadoLista, ItemLista } from '../modelos/lista';
import { TipoTitulo } from '../modelos/titulo';

export interface CambiosItem {
  estado?: EstadoLista;
  calificacion?: number | null;
  resena?: string | null;
}

@Injectable({ providedIn: 'root' })
export class ListaService {
  private readonly http = inject(HttpClient);

  obtener(estado?: EstadoLista): Observable<ItemLista[]> {
    let params = new HttpParams();
    if (estado) params = params.set('estado', estado);
    return this.http.get<ItemLista[]>('/api/lista', { params });
  }

  agregar(tmdbId: number, tipo: TipoTitulo, estado: EstadoLista): Observable<ItemLista> {
    return this.http.post<ItemLista>('/api/lista', { tmdbId, tipo, estado });
  }

  actualizar(id: string, cambios: CambiosItem): Observable<ItemLista> {
    const params = new HttpParams().set('id', id);
    return this.http.patch<ItemLista>('/api/lista', cambios, { params });
  }

  quitar(id: string): Observable<void> {
    const params = new HttpParams().set('id', id);
    return this.http.delete<void>('/api/lista', { params });
  }
}