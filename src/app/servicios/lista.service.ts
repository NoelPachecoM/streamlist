import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { EstadoLista, ItemLista, Resena } from '../modelos/lista';
import { TipoTitulo } from '../modelos/titulo';
import { SpringApiService } from '../spring/spring-api.service';
import { SPRING_URL } from '../spring/spring-config';

export interface CambiosItem {
  estado?: EstadoLista;
  calificacion?: number | null;
  resena?: string | null;
}

/** Mi lista y reseñas: todo pasa por Spring (/api/mi-lista). */
@Injectable({ providedIn: 'root' })
export class ListaService {
  private readonly http = inject(HttpClient);
  private readonly spring = inject(SpringApiService);
  private readonly base = `${SPRING_URL}/api/mi-lista`;

  // Spring sabe quién eres por el token de sesión (no se manda el usuarioId).

  obtener(): Observable<ItemLista[]> {
    if (!this.haySesion()) return this.sinSesion();
    return this.http.get<ItemLista[]>(this.base);
  }

  agregar(tmdbId: number, tipo: TipoTitulo, estado: EstadoLista): Observable<ItemLista> {
    if (!this.haySesion()) return this.sinSesion();
    return this.http.post<ItemLista>(this.base, { tmdbId, tipo, estado });
  }

  actualizar(id: number, cambios: CambiosItem): Observable<ItemLista> {
    if (!this.haySesion()) return this.sinSesion();
    return this.http.put<ItemLista>(`${this.base}/${id}`, cambios);
  }

  quitar(id: number): Observable<void> {
    if (!this.haySesion()) return this.sinSesion();
    return this.http.delete<void>(`${this.base}/${id}`);
  }

  /** Reseñas de todos los usuarios para un título (no hace falta sesión). */
  resenas(tmdbId: number, tipo: TipoTitulo): Observable<Resena[]> {
    const params = new HttpParams().set('tmdbId', tmdbId).set('tipo', tipo);
    return this.http.get<Resena[]>(`${this.base}/resenas`, { params });
  }

  private haySesion(): boolean {
    return this.spring.token() !== null;
  }

  private sinSesion(): Observable<never> {
    return throwError(() => new Error('Inicia sesión para usar tu lista.'));
  }
}
