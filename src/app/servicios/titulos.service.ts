import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { DetalleTitulo, PaginaResultados, Tendencias, TipoTitulo } from '../modelos/titulo';
import { SPRING_URL } from '../spring/spring-config';

/** Búsqueda, detalle y tendencias: todo pasa por Spring, que consulta TMDB y guarda una cache en la base. */
@Injectable({ providedIn: 'root' })
export class TitulosService {
  private readonly http = inject(HttpClient);

  buscar(texto: string, pagina = 1): Observable<PaginaResultados> {
    const params = new HttpParams().set('q', texto).set('pagina', pagina);
    return this.http.get<PaginaResultados>(`${SPRING_URL}/api/tmdb/buscar`, { params });
  }

  detalle(id: number, tipo: TipoTitulo, pais = 'MX'): Observable<DetalleTitulo> {
    const params = new HttpParams().set('id', id).set('tipo', tipo).set('pais', pais);
    return this.http.get<DetalleTitulo>(`${SPRING_URL}/api/tmdb/detalle`, { params });
  }

  /** Lo más visto en el mundo esta semana o hoy (Spring lo guarda en cache 6 horas). */
  tendencias(periodo: 'semana' | 'dia' = 'semana'): Observable<Tendencias> {
    const params = new HttpParams().set('periodo', periodo);
    return this.http.get<Tendencias>(`${SPRING_URL}/api/tmdb/tendencias`, { params });
  }
}
