import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { ESTADOS, EstadoLista, ItemLista, permiteResena } from '../../modelos/lista';
import { AuthService } from '../../servicios/auth.service';
import { CambiosItem, ListaService } from '../../servicios/lista.service';
import { mensajeDeError } from '../../spring/spring-api.service';

@Component({
  selector: 'app-mi-lista',
  imports: [RouterLink],
  templateUrl: './mi-lista.html',
  styleUrl: './mi-lista.css',
})
export class MiLista {
  private readonly lista = inject(ListaService);
  protected readonly auth = inject(AuthService);

  protected readonly estados = ESTADOS;
  protected readonly calificaciones = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10];
  protected readonly permiteResena = permiteResena;

  protected readonly items = signal<ItemLista[]>([]);
  protected readonly pestana = signal<EstadoLista>('pendiente');
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly mensajes = signal<Record<number, string>>({});
  protected readonly ocupado = signal<number | null>(null);

  protected readonly itemsVisibles = computed(() =>
    this.items().filter((i) => i.estado === this.pestana()),
  );

  constructor() {
    void this.cargar();
  }

  protected contar(estado: EstadoLista): number {
    return this.items().filter((i) => i.estado === estado).length;
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
    if (!window.confirm('¿Quitar este título de tu lista? También se borra tu reseña.')) return;
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
    await this.auth.listo;
    if (!this.auth.haIniciadoSesion()) {
      this.cargando.set(false);
      return;
    }
    try {
      // Spring ya manda nombre, año y póster: no hace falta pedir cada título a TMDB.
      this.items.set(await firstValueFrom(this.lista.obtener()));
    } catch (error) {
      this.error.set(this.textoError(error));
    } finally {
      this.cargando.set(false);
    }
  }

  private poner(id: number, texto: string): void {
    this.mensajes.update((actuales) => ({ ...actuales, [id]: texto }));
  }

  private textoError(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.status === 401) {
      this.auth.cerrarSesion();
      return 'Tu sesión ya no es válida. Inicia sesión otra vez.';
    }
    return mensajeDeError(error);
  }
}
