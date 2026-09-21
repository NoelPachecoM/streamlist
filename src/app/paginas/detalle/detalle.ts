import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { ESTADOS, EstadoLista, ItemLista } from '../../modelos/lista';
import { DetalleTitulo, TipoTitulo } from '../../modelos/titulo';
import { AuthService } from '../../servicios/auth.service';
import { ListaService } from '../../servicios/lista.service';
import { TitulosService } from '../../servicios/titulos.service';

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
  protected readonly auth = inject(AuthService);

  protected readonly estados = ESTADOS;

  protected readonly titulo = signal<DetalleTitulo | null>(null);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);

  protected readonly item = signal<ItemLista | null>(null);
  protected readonly estadoElegido = signal<EstadoLista>('por_ver');
  protected readonly guardando = signal(false);
  protected readonly mensaje = signal<string | null>(null);

  protected readonly grupos = computed(() => {
    const d = this.titulo()?.disponibilidad;
    if (!d) return [];
    return [
      { nombre: 'Suscripción', plataformas: d.suscripcion },
      { nombre: 'Renta', plataformas: d.renta },
      { nombre: 'Compra', plataformas: d.compra },
    ].filter((grupo) => grupo.plataformas.length > 0);
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
      this.manejarErrorLista(error);
    } finally {
      this.guardando.set(false);
    }
  }

  protected async quitar(): Promise<void> {
    const actual = this.item();
    if (!actual) return;
    this.guardando.set(true);
    this.mensaje.set(null);
    try {
      await firstValueFrom(this.lista.quitar(actual.id));
      this.item.set(null);
      this.estadoElegido.set('por_ver');
      this.mensaje.set('Quitado de tu lista.');
    } catch (error) {
      this.manejarErrorLista(error);
    } finally {
      this.guardando.set(false);
    }
  }

  private async cargar(id: number, tipo: TipoTitulo): Promise<void> {
    this.cargando.set(true);
    this.error.set(null);
    this.titulo.set(null);
    this.item.set(null);
    this.mensaje.set(null);
    this.estadoElegido.set('por_ver');
    try {
      this.titulo.set(await firstValueFrom(this.titulos.detalle(id, tipo)));
    } catch {
      this.error.set('No se pudo cargar el título. Intenta de nuevo.');
    } finally {
      this.cargando.set(false);
    }
    if (this.titulo()) await this.cargarItem(id, tipo);
  }

  /** Si hay sesión, revisa si este título ya está en la lista del usuario. */
  private async cargarItem(id: number, tipo: TipoTitulo): Promise<void> {
    if (!this.auth.haIniciadoSesion()) return;
    try {
      const items = await firstValueFrom(this.lista.obtener());
      const encontrado = items.find((i) => i.tmdbId === id && i.tipo === tipo) ?? null;
      this.item.set(encontrado);
      if (encontrado) this.estadoElegido.set(encontrado.estado);
    } catch (error) {
      this.manejarErrorLista(error);
    }
  }

  private manejarErrorLista(error: unknown): void {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 401) {
        this.auth.cerrarSesion();
        this.mensaje.set('Tu sesión expiró. Inicia sesión otra vez.');
        return;
      }
      if (error.status === 409) {
        this.mensaje.set('Este título ya está en tu lista.');
        return;
      }
      const texto = error.error?.error?.mensaje;
      this.mensaje.set(typeof texto === 'string' ? texto : 'No se pudo completar la acción.');
      return;
    }
    this.mensaje.set('No se pudo completar la acción.');
  }
}