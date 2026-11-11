import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { PaginaSpring, SpringApiService, mensajeDeError } from '../spring-api.service';
import { Campo, RECURSOS, Recurso } from './catalogos.config';

const TAMANO_PAGINA = 10;

@Component({
  selector: 'app-spring-catalogos',
  imports: [FormsModule, RouterLink],
  templateUrl: './spring-catalogos.html',
  styleUrl: '../spring.css',
})
export class SpringCatalogos implements OnInit {
  private readonly api = inject(SpringApiService);

  protected readonly recursos = RECURSOS;
  protected readonly recurso = signal<Recurso>(RECURSOS[0]);
  protected readonly pagina = signal<PaginaSpring | null>(null);
  protected readonly numeroPagina = signal(0);
  protected readonly editandoId = signal<number | null>(null);
  protected readonly cargando = signal(false);
  protected readonly mensaje = signal('');
  protected readonly error = signal('');

  protected readonly filas = computed(() => this.pagina()?.content ?? []);
  protected readonly hayAnterior = computed(() => this.numeroPagina() > 0);
  protected readonly haySiguiente = computed(
    () => this.numeroPagina() + 1 < (this.pagina()?.totalPages ?? 0),
  );

  protected formulario: Record<string, string> = {};
  protected busqueda = '';

  ngOnInit(): void {
    void this.cargar(0);
  }

  protected elegir(ruta: string): void {
    const nuevo = this.recursos.find((x) => x.ruta === ruta);
    if (!nuevo) return;
    this.recurso.set(nuevo);
    this.busqueda = '';
    this.limpiarFormulario();
    void this.cargar(0);
  }

  protected idDe(fila: Record<string, any>): number {
    return fila[this.recurso().idClave ?? 'id'] as number;
  }

  protected texto(fila: Record<string, any>, campo: Campo): string {
    const valor = fila[campo.clave];
    if (valor === null || valor === undefined || valor === '') return '—';
    if (campo.tipo === 'ref') {
      const id = valor[campo.refId ?? 'id'];
      return valor.nombre ? `${id} · ${valor.nombre}` : String(id);
    }
    const s = String(valor);
    return s.length > 60 ? s.slice(0, 57) + '…' : s;
  }

  protected tipoInput(campo: Campo): string {
    if (campo.tipo === 'numero' || campo.tipo === 'ref') return 'number';
    if (campo.tipo === 'fecha') return 'date';
    return 'text';
  }

  protected buscar(): void {
    void this.cargar(0);
  }

  protected async cargar(numero: number): Promise<void> {
    this.cargando.set(true);
    this.error.set('');
    try {
      const r = this.recurso();
      this.pagina.set(
        await this.api.listar(r.ruta, numero, TAMANO_PAGINA, r.busqueda ? this.busqueda : undefined),
      );
      this.numeroPagina.set(numero);
    } catch (e) {
      this.pagina.set(null);
      this.error.set(mensajeDeError(e));
    } finally {
      this.cargando.set(false);
    }
  }

  protected editar(fila: Record<string, any>): void {
    const valores: Record<string, string> = {};
    for (const c of this.recurso().campos) {
      const v = fila[c.clave];
      if (v === null || v === undefined) valores[c.clave] = '';
      else if (c.tipo === 'ref') valores[c.clave] = String(v[c.refId ?? 'id'] ?? '');
      else valores[c.clave] = String(v);
    }
    this.formulario = valores;
    this.editandoId.set(this.idDe(fila));
    this.mensaje.set('');
    this.error.set('');
  }

  protected limpiarFormulario(): void {
    this.formulario = {};
    this.editandoId.set(null);
  }

  protected async guardar(): Promise<void> {
    if (this.cargando()) return;
    this.mensaje.set('');
    this.error.set('');

    let cuerpo: Record<string, unknown>;
    try {
      cuerpo = this.armarCuerpo();
    } catch (e) {
      this.error.set((e as Error).message);
      return;
    }

    const ruta = this.recurso().ruta;
    const id = this.editandoId();
    this.cargando.set(true);
    try {
      if (id === null) {
        await this.api.crear(ruta, cuerpo);
        this.mensaje.set('Registro creado.');
      } else {
        await this.api.actualizar(ruta, id, cuerpo);
        this.mensaje.set('Registro actualizado.');
      }
      this.limpiarFormulario();
      await this.cargar(this.numeroPagina());
    } catch (e) {
      this.error.set(mensajeDeError(e));
    } finally {
      this.cargando.set(false);
    }
  }

  protected async eliminar(fila: Record<string, any>): Promise<void> {
    if (this.cargando()) return;
    const id = this.idDe(fila);
    if (!globalThis.confirm(`¿Eliminar el registro ${id}?`)) return;
    this.mensaje.set('');
    this.error.set('');
    this.cargando.set(true);
    try {
      await this.api.eliminar(this.recurso().ruta, id);
      this.mensaje.set('Registro eliminado.');
      // Si se borro el ultimo de la pagina, se regresa a la anterior.
      const ultimo = this.filas().length === 1 && this.numeroPagina() > 0;
      await this.cargar(ultimo ? this.numeroPagina() - 1 : this.numeroPagina());
    } catch (e) {
      this.error.set(mensajeDeError(e));
    } finally {
      this.cargando.set(false);
    }
  }

  /** Convierte lo escrito en el formulario al JSON que espera Spring. */
  private armarCuerpo(): Record<string, unknown> {
    const cuerpo: Record<string, unknown> = {};
    for (const c of this.recurso().campos) {
      const crudo = (this.formulario[c.clave] ?? '').trim();
      if (crudo === '') {
        // Las relaciones vacias no se mandan; los demas campos vacios van como null.
        if (c.tipo !== 'ref') cuerpo[c.clave] = null;
        continue;
      }
      if (c.tipo === 'numero' || c.tipo === 'ref') {
        const numero = Number(crudo);
        if (Number.isNaN(numero)) throw new Error(`"${c.etiqueta}" debe ser un número.`);
        cuerpo[c.clave] = c.tipo === 'ref' ? { [c.refId ?? 'id']: numero } : numero;
      } else {
        cuerpo[c.clave] = crudo;
      }
    }
    return cuerpo;
  }
}
