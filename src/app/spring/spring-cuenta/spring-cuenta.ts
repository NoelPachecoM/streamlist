import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { DatosArco, SpringApiService, mensajeDeError } from '../spring-api.service';

@Component({
  selector: 'app-spring-cuenta',
  imports: [FormsModule],
  templateUrl: './spring-cuenta.html',
  styleUrl: './spring-cuenta.css',
})
export class SpringCuenta implements OnInit {
  protected readonly api = inject(SpringApiService);
  private readonly router = inject(Router);

  protected readonly datos = signal<DatosArco | null>(null);
  protected readonly mensaje = signal('');
  protected readonly error = signal('');
  protected readonly cargando = signal(false);
  /** Id de la solicitud de cancelacion (existe cuando ya se pidio el codigo al correo). */
  protected readonly solicitudCancelacion = signal<number | null>(null);

  /** Cambio de correo con doble verificación: inicio -> actual -> nuevo. */
  protected readonly pasoCorreo = signal<'inicio' | 'actual' | 'nuevo'>('inicio');
  protected readonly solicitudCorreo = signal<number | null>(null);
  protected readonly correoPendiente = signal('');

  // Cada operacion ARCO pide la contraseña actual para verificar que eres tu.
  protected passwordActual = '';
  protected nombreNuevo = '';
  protected telefonoNuevo = '';
  protected correoNuevo = '';
  protected passwordNueva = '';
  protected codigoCancelacion = '';
  protected codigoCorreo = '';

  ngOnInit(): void {
    void this.cargarDatos();
  }

  protected inicial(): string {
    const nombre = this.datos()?.nombre ?? this.api.usuario()?.nombre ?? '';
    return (nombre.trim()[0] ?? '?').toUpperCase();
  }

  protected salir(): void {
    this.api.cerrarSesion();
    void this.router.navigate(['/login']);
  }

  protected cambiarNombre(): Promise<void> {
    return this.ejecutar(true, async (correo) => {
      const nuevo = this.exigir(this.nombreNuevo, 'el nombre nuevo');
      const r = await this.api.arco('cambiar-nombre', {
        correo,
        password: this.passwordActual,
        nombreNuevo: nuevo,
      });
      this.api.actualizarSesion({ nombre: nuevo });
      this.nombreNuevo = '';
      return r.mensaje;
    });
  }

  protected cambiarTelefono(): Promise<void> {
    return this.ejecutar(true, async (correo) => {
      const nuevo = this.exigir(this.telefonoNuevo, 'el teléfono nuevo');
      const r = await this.api.arco('cambiar-telefono', {
        correo,
        password: this.passwordActual,
        telefonoNuevo: nuevo,
      });
      this.api.actualizarSesion({ telefono: nuevo });
      this.telefonoNuevo = '';
      return r.mensaje;
    });
  }

  /** Cambio de correo, paso 1: Spring manda un código al correo ACTUAL. */
  protected pedirCambioCorreo(): Promise<void> {
    return this.ejecutar(true, async (correo) => {
      const nuevo = this.exigir(this.correoNuevo, 'el correo nuevo');
      const r = await this.api.arco('cambiar-correo/solicitar', {
        correo,
        password: this.passwordActual,
        correoNuevo: nuevo,
      });
      this.solicitudCorreo.set(r.solicitudId ?? null);
      this.correoPendiente.set(nuevo);
      this.codigoCorreo = '';
      this.pasoCorreo.set('actual');
      return r.mensaje;
    });
  }

  /** Paso 2: con el código del correo actual, Spring manda otro código al correo NUEVO. */
  protected confirmarCorreoActual(): Promise<void> {
    return this.ejecutar(false, async () => {
      const solicitudId = this.solicitudCorreo();
      if (solicitudId === null) throw new Error('Empieza el cambio de correo de nuevo.');
      const r = await this.api.arco('cambiar-correo/confirmar-actual', {
        solicitudId,
        codigo: this.exigir(this.codigoCorreo, 'el código'),
      });
      this.codigoCorreo = '';
      this.pasoCorreo.set('nuevo');
      return r.mensaje;
    });
  }

  /** Paso 3: con el código del correo nuevo, se cambia el correo de la cuenta. */
  protected confirmarCorreoNuevo(): Promise<void> {
    return this.ejecutar(false, async () => {
      const solicitudId = this.solicitudCorreo();
      const nuevo = this.correoPendiente();
      if (solicitudId === null || !nuevo) throw new Error('Empieza el cambio de correo de nuevo.');
      const r = await this.api.arco('cambiar-correo/confirmar-nuevo', {
        solicitudId,
        codigo: this.exigir(this.codigoCorreo, 'el código'),
      });
      this.api.actualizarSesion({ correo: nuevo });
      this.reiniciarCambioCorreo();
      return r.mensaje;
    });
  }

  protected cancelarCambioCorreo(): void {
    this.reiniciarCambioCorreo();
    this.error.set('');
    this.mensaje.set('');
  }

  private reiniciarCambioCorreo(): void {
    this.pasoCorreo.set('inicio');
    this.solicitudCorreo.set(null);
    this.correoPendiente.set('');
    this.correoNuevo = '';
    this.codigoCorreo = '';
  }

  protected cambiarPassword(): Promise<void> {
    return this.ejecutar(true, async (correo) => {
      const nueva = this.exigir(this.passwordNueva, 'la contraseña nueva');
      const r = await this.api.arco('cambiar-password', {
        correo,
        password: this.passwordActual,
        passwordNueva: nueva,
      });
      this.passwordActual = '';
      this.passwordNueva = '';
      return r.mensaje;
    });
  }

  /** Cancelar suscripcion, paso 1: Spring manda un codigo al correo. */
  protected pedirCodigoCancelacion(): Promise<void> {
    return this.ejecutar(true, async (correo) => {
      const r = await this.api.arco('cancelar/solicitar', {
        correo,
        password: this.passwordActual,
      });
      this.solicitudCancelacion.set(r.solicitudId ?? null);
      return r.mensaje;
    });
  }

  /** Cancelar suscripcion, paso 2: con el codigo correcto la cuenta queda cancelada. */
  protected confirmarCancelacion(): Promise<void> {
    return this.ejecutar(false, async () => {
      const solicitudId = this.solicitudCancelacion();
      if (solicitudId === null) throw new Error('Primero pide el código.');
      const r = await this.api.arco('cancelar/confirmar', {
        solicitudId,
        codigo: this.exigir(this.codigoCancelacion, 'el código'),
      });
      this.solicitudCancelacion.set(null);
      this.codigoCancelacion = '';
      this.passwordActual = '';
      this.api.actualizarSesion({ suscripcion: 'cancelada' });
      return r.mensaje;
    });
  }

  private async cargarDatos(): Promise<void> {
    if (!this.api.haIniciadoSesion()) return;
    try {
      this.datos.set(await this.api.datos());
    } catch (e) {
      this.error.set(mensajeDeError(e));
    }
  }

  /** Ejecuta una operacion mostrando el resultado o el error, y refresca los datos. */
  private async ejecutar(
    pideContrasena: boolean,
    accion: (correo: string) => Promise<string>,
  ): Promise<void> {
    if (this.cargando()) return;
    this.mensaje.set('');
    this.error.set('');
    const correo = this.api.usuario()?.correo;
    if (!correo) {
      this.error.set('No hay sesión iniciada.');
      return;
    }
    if (pideContrasena && !this.passwordActual) {
      this.error.set('Escribe tu contraseña actual para confirmar que eres tú.');
      return;
    }
    this.cargando.set(true);
    try {
      this.mensaje.set(await accion(correo));
      await this.cargarDatos();
    } catch (e) {
      this.error.set(e instanceof Error && !('status' in e) ? e.message : mensajeDeError(e));
    } finally {
      this.cargando.set(false);
    }
  }

  private exigir(valor: string, que: string): string {
    const limpio = valor.trim();
    if (!limpio) throw new Error(`Escribe ${que}.`);
    return limpio;
  }
}
