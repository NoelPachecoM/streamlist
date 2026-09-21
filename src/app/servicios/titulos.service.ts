import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { DetalleTitulo, PaginaResultados, TipoTitulo } from '../modelos/titulo';

@Injectable({ providedIn: 'root' })
export class TitulosService {
  private readonly http = inject(HttpClient);

  buscar(texto: string, pagina = 1): Observable<PaginaResultados> {
    const params = new HttpParams().set('q', texto).set('pagina', pagina);
    return this.http.get<PaginaResultados>('/api/buscar', { params });
  }

  detalle(id: number, tipo: TipoTitulo, pais = 'MX'): Observable<DetalleTitulo> {
    const params = new HttpParams().set('id', id).set('tipo', tipo).set('pais', pais);
    return this.http.get<DetalleTitulo>('/api/detalle', { params });
  }
}