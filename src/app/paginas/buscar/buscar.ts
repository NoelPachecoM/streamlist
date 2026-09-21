import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { ResultadoBusqueda } from '../../modelos/titulo';
import { AuthService } from '../../servicios/auth.service';
import { TitulosService } from '../../servicios/titulos.service';

@Component({
  selector: 'app-buscar',
  imports: [RouterLink],
  templateUrl: './buscar.html',
  styleUrl: './buscar.css',
})
export class Buscar {
  private readonly titulos = inject(TitulosService);
  protected readonly auth = inject(AuthService);

  protected readonly termino = signal('');
  protected readonly resultados = signal<ResultadoBusqueda[]>([]);
  protected readonly pagina = signal(0);
  protected readonly totalPaginas = signal(0);
  protected readonly cargando = signal(false);
  protected readonly buscado = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly hayMas = computed(() => this.pagina() < this.totalPaginas());

  protected async buscar(texto: string): Promise<void> {
    const consulta = texto.trim();
    if (!consulta) return;
    this.termino.set(consulta);
    this.resultados.set([]);
    this.pagina.set(0);
    this.totalPaginas.set(0);
    this.buscado.set(false);
    await this.cargarPagina(1);
  }

  protected async cargarMas(): Promise<void> {
    await this.cargarPagina(this.pagina() + 1);
  }

  private async cargarPagina(numero: number): Promise<void> {
    this.cargando.set(true);
    this.error.set(null);
    try {
      const respuesta = await firstValueFrom(this.titulos.buscar(this.termino(), numero));
      this.resultados.update((actuales) => [...actuales, ...respuesta.resultados]);
      this.pagina.set(respuesta.pagina);
      this.totalPaginas.set(respuesta.totalPaginas);
    } catch {
      this.error.set('No se pudo buscar. Intenta de nuevo.');
    } finally {
      this.cargando.set(false);
      this.buscado.set(true);
    }
  }
}