import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Location } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import {
  ESTADOS,
  EstadoLista,
  ItemLista,
  Resena,
  permiteResena,
} from '../../modelos/lista';
import { DetalleTitulo, TipoTitulo } from '../../modelos/titulo';
import { AuthService } from '../../servicios/auth.service';
import { ListaService } from '../../servicios/lista.service';
import { TitulosService } from '../../servicios/titulos.service';
import { mensajeDeError } from '../../spring/spring-api.service';

@Component({
  selector: 'app-detalle',
  imports: [RouterLink],
  templateUrl: './detalle.html',
  styleUrl: './detalle.css',
})
export class Detalle {
  private readonly ruta = inject(ActivatedRoute);
  private readonly titulos = inject(TitulosService);
  private readonly lista = inject(ListaService);
  private readonly location = inject(Location);
  private readonly router = inject(Router);
  protected readonly auth = inject(AuthService);

  protected readonly estados = ESTADOS;
  protected readonly calificaciones = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10];
  protected readonly permiteResena = permiteResena;

  protected readonly titulo = signal<DetalleTitulo | null>(null);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);

  protected readonly item = signal<ItemLista | null>(null);
  protected readonly estadoElegido = signal<EstadoLista>('pendiente');
  protected readonly guardando = signal(false);
  protected readonly mensaje = signal<string | null>(null);

  protected readonly resenas = signal<Resena[]>([]);
  protected readonly cargandoResenas = signal(false);
  protected readonly errorResenas = signal<string | null>(null);
  protected readonly guardandoResena = signal(false);
  protected readonly mensajeResena = signal<string | null>(null);

  protected readonly grupos = computed(() => {
    const d = this.titulo()?.disponibilidad;
    if (!d) return [];
    return [
      { nombre: 'Suscripción', plataformas: d.suscripcion },
      { nombre: 'Renta', plataformas: d.renta },
      { nombre: 'Compra', plataformas: d.compra },
    ].filter((grupo) => grupo.plataformas.length > 0);
  });

  /** Promedio de las calificaciones de los usuarios de StreamList. */
  protected readonly promedio = computed(() => {
    const notas = this.resenas()
      .map((r) => r.calificacion)
      .filter((n): n is number => n !== null);
    if (notas.length === 0) return null;
    return notas.reduce((a, b) => a + b, 0) / notas.length;
  });

  constructor() {
    // Nos suscribimos a paramMap (no a snapshot) para detectar cambios de :id
    this.ruta.paramMap.pipe(takeUntilDestroyed()).subscribe((params) => {
      const tipo = params.get('tipo');
      const id = Number(params.get('id'));
      if ((tipo !== 'pelicula' && tipo !== 'serie') || !Number.isInteger(id)) {
        this.cargando.set(false);
        this.error.set('Título no válido.');
        return;
      }
      void this.cargar(id, tipo);
    });
  }

  /** Regresa a la página anterior (por ejemplo, a los resultados de búsqueda). */
  protected volver(): void {
    if (window.history.length > 1) {
      this.location.back();
    } else {
      void this.router.navigate(['/buscar']);
    }
  }

  protected async guardar(): Promise<void> {
    const t = this.titulo();
    if (!t) return;
    this.guardando.set(true);
    this.mensaje.set(null);
    try {
      const actual = this.item();
      if (actual) {
        const actualizado = await firstValueFrom(
          this.lista.actualizar(actual.id, { estado: this.estadoElegido() }),
        );
        this.item.set(actualizado);
        this.mensaje.set('Estado actualizado.');
      } else {
        const nuevo = await firstValueFrom(
          this.lista.agregar(t.tmdbId, t.tipo, this.estadoElegido()),
        );
        this.item.set(nuevo);
        this.mensaje.set('Agregado a tu lista.');
      }
    } catch (error) {
      this.mensaje.set(this.textoError(error));
    } finally {
      this.guardando.set(false);
    }
  }

  protected async quitar(): Promise<void> {
    const actual = this.item();
    if (!actual) return;
    if (!window.confirm('¿Quitar este título de tu lista? También se borra tu reseña.')) return;
    this.guardando.set(true);
    this.mensaje.set(null);
    try {
      await firstValueFrom(this.lista.quitar(actual.id));
      this.item.set(null);
      this.estadoElegido.set('pendiente');
      this.mensaje.set('Quitado de tu lista.');
      await this.cargarResenas(actual.tmdbId, actual.tipo);
    } catch (error) {
      this.mensaje.set(this.textoError(error));
    } finally {
      this.guardando.set(false);
    }
  }

  /** Guarda la calificación y la reseña propias (sección de abajo). */
  protected async guardarResena(valorCalificacion: string, texto: string): Promise<void> {
    const actual = this.item();
    if (!actual) return;
    const limpio = texto.trim();
    this.guardandoResena.set(true);
    this.mensajeResena.set(null);
    try {
      const actualizado = await firstValueFrom(
        this.lista.actualizar(actual.id, {
          calificacion: valorCalificacion === '' ? null : Number(valorCalificacion),
          resena: limpio === '' ? null : limpio,
        }),
      );
      this.item.set(actualizado);
      this.mensajeResena.set('Reseña guardada.');
      await this.cargarResenas(actualizado.tmdbId, actualizado.tipo);
    } catch (error) {
      this.mensajeResena.set(this.textoError(error));
    } finally {
      this.guardandoResena.set(false);
    }
  }

  protected fecha(valor: string | null): string {
    if (!valor) return '';
    const d = new Date(valor);
    return Number.isNaN(d.getTime())
      ? ''
      : d.toLocaleDateString('es-MX', { day: 'numeric', month: 'short', year: 'numeric' });
  }

  private async cargar(id: number, tipo: TipoTitulo): Promise<void> {
    this.cargando.set(true);
    this.error.set(null);
    this.titulo.set(null);
    this.item.set(null);
    this.mensaje.set(null);
    this.mensajeResena.set(null);
    this.resenas.set([]);
    this.estadoElegido.set('pendiente');
    try {
      this.titulo.set(await firstValueFrom(this.titulos.detalle(id, tipo)));
    } catch (e) {
      this.error.set(mensajeDeError(e));
    } finally {
      this.cargando.set(false);
    }
    if (this.titulo()) {
      await Promise.all([this.cargarItem(id, tipo), this.cargarResenas(id, tipo)]);
    }
  }

  /** Si hay sesión, revisa si este título ya está en la lista del usuario. */
  private async cargarItem(id: number, tipo: TipoTitulo): Promise<void> {
    await this.auth.listo;
    if (!this.auth.haIniciadoSesion()) return;
    try {
      const items = await firstValueFrom(this.lista.obtener());
      const encontrado = items.find((i) => i.tmdbId === id && i.tipo === tipo) ?? null;
      this.item.set(encontrado);
      if (encontrado) this.estadoElegido.set(encontrado.estado);
    } catch (error) {
      this.mensaje.set(this.textoError(error));
    }
  }

  private async cargarResenas(id: number, tipo: TipoTitulo): Promise<void> {
    this.cargandoResenas.set(true);
    this.errorResenas.set(null);
    try {
      this.resenas.set(await firstValueFrom(this.lista.resenas(id, tipo)));
    } catch (error) {
      this.errorResenas.set(mensajeDeError(error));
    } finally {
      this.cargandoResenas.set(false);
    }
  }

  private textoError(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.status === 401) {
      this.auth.cerrarSesion();
      return 'Tu sesión ya no es válida. Inicia sesión otra vez.';
    }
    return mensajeDeError(error);
  }
}
