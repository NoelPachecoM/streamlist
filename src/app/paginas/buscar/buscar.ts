import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { ResultadoBusqueda } from '../../modelos/titulo';
import { AuthService } from '../../servicios/auth.service';
import { TitulosService } from '../../servicios/titulos.service';
import { mensajeDeError } from '../../spring/spring-api.service';

@Component({
  selector: 'app-buscar',
  imports: [RouterLink],
  templateUrl: './buscar.html',
  styleUrl: './buscar.css',
})
export class Buscar {
  private readonly titulos = inject(TitulosService);
  private readonly ruta = inject(ActivatedRoute);
  private readonly router = inject(Router);
  protected readonly auth = inject(AuthService);

  // ---- Búsqueda ----
  protected readonly termino = signal('');
  protected readonly resultados = signal<ResultadoBusqueda[]>([]);
  protected readonly pagina = signal(0);
  protected readonly totalPaginas = signal(0);
  protected readonly totalResultados = signal(0);
  protected readonly cargando = signal(false);
  protected readonly buscado = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly hayMas = computed(() => this.pagina() < this.totalPaginas());

  // ---- Pantalla de inicio (cuando no hay búsqueda) ----
  protected readonly top10 = signal<ResultadoBusqueda[]>([]);
  protected readonly hoy = signal<ResultadoBusqueda[]>([]);
  protected readonly cargandoInicio = signal(true);
  protected readonly errorInicio = signal<string | null>(null);
  protected readonly destacado = computed(() => this.top10().find((t) => t.imagenFondo) ?? null);

  protected readonly esqueletos = Array.from({ length: 10 }, (_, i) => i);

  constructor() {
    // La búsqueda vive en la dirección (?q=batman): al volver del detalle se conservan los resultados.
    this.ruta.queryParamMap.pipe(takeUntilDestroyed()).subscribe((params) => {
      const q = (params.get('q') ?? '').trim();
      if (q) {
        if (q !== this.termino() || !this.buscado()) void this.iniciarBusqueda(q);
      } else {
        this.limpiarBusqueda();
      }
    });
    void this.cargarInicio();
  }

  protected buscar(texto: string): void {
    const consulta = texto.trim();
    void this.router.navigate(['/buscar'], { queryParams: consulta ? { q: consulta } : {} });
  }

  protected limpiar(caja: HTMLInputElement): void {
    caja.value = '';
    void this.router.navigate(['/buscar']);
  }

  protected async cargarMas(): Promise<void> {
    await this.cargarPagina(this.pagina() + 1);
  }

  /** Retraso escalonado para que las tarjetas aparezcan una tras otra. */
  protected retraso(indice: number): string {
    return `${Math.min(indice % 20, 14) * 45}ms`;
  }

  private async iniciarBusqueda(consulta: string): Promise<void> {
    this.termino.set(consulta);
    this.resultados.set([]);
    this.pagina.set(0);
    this.totalPaginas.set(0);
    this.totalResultados.set(0);
    this.buscado.set(false);
    await this.cargarPagina(1);
  }

  private limpiarBusqueda(): void {
    this.termino.set('');
    this.resultados.set([]);
    this.buscado.set(false);
    this.error.set(null);
  }

  private async cargarPagina(numero: number): Promise<void> {
    this.cargando.set(true);
    this.error.set(null);
    try {
      const respuesta = await firstValueFrom(this.titulos.buscar(this.termino(), numero));
      this.resultados.update((actuales) => [...actuales, ...respuesta.resultados]);
      this.pagina.set(respuesta.pagina);
      this.totalPaginas.set(respuesta.totalPaginas);
      this.totalResultados.set(respuesta.totalResultados);
    } catch (e) {
      this.error.set(mensajeDeError(e));
    } finally {
      this.cargando.set(false);
      this.buscado.set(true);
    }
  }

  private async cargarInicio(): Promise<void> {
    this.cargandoInicio.set(true);
    this.errorInicio.set(null);
    try {
      const [semana, dia] = await Promise.all([
        firstValueFrom(this.titulos.tendencias('semana')),
        firstValueFrom(this.titulos.tendencias('dia')),
      ]);
      this.top10.set(semana.resultados.slice(0, 10));
      this.hoy.set(dia.resultados.slice(0, 12));
    } catch (e) {
      this.errorInicio.set(mensajeDeError(e));
    } finally {
      this.cargandoInicio.set(false);
    }
  }
}
