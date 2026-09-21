import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { ESTADOS, EstadoLista, ItemLista } from '../../modelos/lista';
import { DetalleTitulo } from '../../modelos/titulo';
import { AuthService } from '../../servicios/auth.service';
import { CambiosItem, ListaService } from '../../servicios/lista.service';
import { TitulosService } from '../../servicios/titulos.service';

@Component({
  selector: 'app-mi-lista',
  imports: [RouterLink],
  templateUrl: './mi-lista.html',
  styleUrl: './mi-lista.css',
})
export class MiLista {
  private readonly lista = inject(ListaService);
  private readonly titulos = inject(TitulosService);
  protected readonly auth = inject(AuthService);

  protected readonly estados = ESTADOS;
  protected readonly calificaciones = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];

  protected readonly items = signal<ItemLista[]>([]);
  protected readonly detalles = signal<Record<string, DetalleTitulo | undefined>>({});
  protected readonly pestana = signal<EstadoLista>('por_ver');
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly mensajes = signal<Record<string, string>>({});
  protected readonly ocupado = signal<string | null>(null);

  protected readonly itemsVisibles = computed(() =>
    this.items().filter((i) => i.estado === this.pestana()),
  );

  constructor() {
    void this.cargar();
  }

  protected contar(estado: EstadoLista): number {
    return this.items().filter((i) => i.estado === estado).length;
  }

  protected clave(item: { tipo: string; tmdbId: number }): string {
    return `${item.tipo}-${item.tmdbId}`;
  }

  protected async cambiarEstado(item: ItemLista, estado: string): Promise<void> {
    await this.aplicar(item, { estado: estado as EstadoLista }, 'Estado actualizado.');
  }

  protected async calificar(item: ItemLista, valor: string): Promise<void> {
    const calificacion = valor === '' ? null : Number(valor);
    await this.aplicar(item, { calificacion }, 'Calificación guardada.');
  }

  protected async guardarResena(item: ItemLista, texto: string): Promise<void> {
    const limpio = texto.trim();
    await this.aplicar(item, { resena: limpio === '' ? null : limpio }, 'Reseña guardada.');
  }

  protected async quitar(item: ItemLista): Promise<void> {
    if (!window.confirm('¿Quitar este título de tu lista?')) return;
    this.ocupado.set(item.id);
    this.poner(item.id, '');
    try {
      await firstValueFrom(this.lista.quitar(item.id));
      this.items.update((actuales) => actuales.filter((i) => i.id !== item.id));
    } catch (error) {
      this.poner(item.id, this.textoError(error));
    } finally {
      this.ocupado.set(null);
    }
  }

  private async aplicar(item: ItemLista, cambios: CambiosItem, exito: string): Promise<void> {
    this.ocupado.set(item.id);
    this.poner(item.id, '');
    try {
      const actualizado = await firstValueFrom(this.lista.actualizar(item.id, cambios));
      this.items.update((actuales) => actuales.map((i) => (i.id === actualizado.id ? actualizado : i)));
      this.poner(item.id, exito);
    } catch (error) {
      this.poner(item.id, this.textoError(error));
    } finally {
      this.ocupado.set(null);
    }
  }

  private async cargar(): Promise<void> {
    if (!this.auth.haIniciadoSesion()) {
      this.cargando.set(false);
      return;
    }
    try {
      const items = await firstValueFrom(this.lista.obtener());
      this.items.set(items);
      await this.cargarDetalles(items);
    } catch (error) {
      this.error.set(this.textoError(error));
    } finally {
      this.cargando.set(false);
    }
  }

  /** La lista solo guarda ids; los nombres y pósters se piden a TMDB. */
  private async cargarDetalles(items: ItemLista[]): Promise<void> {
    const resultados = await Promise.allSettled(
      items.map((i) => firstValueFrom(this.titulos.detalle(i.tmdbId, i.tipo))),
    );
    const mapa: Record<string, DetalleTitulo | undefined> = {};
    resultados.forEach((resultado, indice) => {
      if (resultado.status === 'fulfilled') mapa[this.clave(items[indice])] = resultado.value;
    });
    this.detalles.set(mapa);
  }

  private poner(id: string, texto: string): void {
    this.mensajes.update((actuales) => ({ ...actuales, [id]: texto }));
  }

  private textoError(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 401) {
        this.auth.cerrarSesion();
        return 'Tu sesión expiró. Inicia sesión otra vez.';
      }
      const texto = error.error?.error?.mensaje;
      if (typeof texto === 'string') return texto;
    }
    return 'No se pudo completar la acción. Intenta de nuevo.';
  }
}