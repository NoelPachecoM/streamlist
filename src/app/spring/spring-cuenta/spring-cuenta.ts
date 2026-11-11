import { Component, OnDestroy, OnInit, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { DatosArco, SpringApiService, mensajeDeError } from '../spring-api.service';

/** Secciones de "Editar mi cuenta"; el aviso de cada operación se muestra en la suya. */
type Seccion = 'nombre' | 'telefono' | 'correo' | 'password' | 'cancelar';

/** Segundos que se muestra el aviso antes de cerrar la sesión al cambiar la contraseña. */
const SEGUNDOS_ANTES_DE_SALIR = 5;
const MINIMO_PASSWORD = 8;

@Component({
  selector: 'app-spring-cuenta',
  imports: [FormsModule],
  templateUrl: './spring-cuenta.html',
  styleUrl: './spring-cuenta.css',
})
export class SpringCuenta implements OnInit, OnDestroy {
  protected readonly api = inject(SpringApiService);
  private readonly router = inject(Router);

  protected readonly datos = signal<DatosArco | null>(null);
  protected readonly mensaje = signal('');
  protected readonly error = signal('');
  /** En qué sección se muestra el aviso (null = aviso flotante general). */
  protected readonly seccionAviso = signal<Seccion | null>(null);
  protected readonly cargando = signal(false);
  /** Cuenta regresiva después de cambiar la contraseña (0 = no está saliendo). */
  protected readonly segundosParaSalir = signal(0);
  /** Id de la solicitud de cancelacion (existe cuando ya se pidio el codigo al correo). */
  protected readonly solicitudCancelacion = signal<number | null>(null);

  /** Cambio de correo con doble verificación: inicio -> actual -> nuevo. */
  protected readonly pasoCorreo = signal<'inicio' | 'actual' | 'nuevo'>('inicio');
  protected readonly solicitudCorreo = signal<number | null>(null);
  protected readonly correoPendiente = signal('');

  // Cada sección tiene su PROPIO campo de contraseña, para que lo escrito en una no aparezca en las demás.
  protected passwordNombre = '';
  protected passwordTelefono = '';
  protected passwordCorreo = '';
  protected passwordCambio = '';
  protected passwordCancelar = '';

  protected nombreNuevo = '';
  protected telefonoNuevo = '';
  protected correoNuevo = '';
  protected passwordNueva = '';
  protected passwordConfirmacion = '';
  protected codigoCancelacion = '';
  protected codigoCorreo = '';

  private temporizadorSalida?: ReturnType<typeof setInterval>;

  constructor() {
    // Si la sesión se pierde (venció o se cerró en otra pestaña), no se queda la página de Mi cuenta
    // con el encabezado diciendo "Iniciar sesión": se manda al login.
    effect(() => {
      if (!this.api.haIniciadoSesion() && this.segundosParaSalir() === 0) {
        void this.router.navigate(['/login']);
      }
    });
  }

  ngOnInit(): void {
    void this.cargarDatos();
  }

  ngOnDestroy(): void {
    clearInterval(this.temporizadorSalida);
    // Si sale de la página durante la cuenta regresiva, la sesión ya no sirve: se borra igual.
    if (this.segundosParaSalir() > 0) this.api.olvidarSesion();
  }

  protected inicial(): string {
    const nombre = this.datos()?.nombre ?? this.api.usuario()?.nombre ?? '';
    return (nombre.trim()[0] ?? '?').toUpperCase();
  }

  protected salir(): void {
    this.api.cerrarSesion();
    void this.router.navigate(['/login']);
  }

  /** Al abrir una sección se limpian las contraseñas y los avisos de las demás. */
  protected alAbrirSeccion(evento: Event): void {
    const detalles = evento.target as HTMLDetailsElement;
    if (!detalles.open || this.segundosParaSalir() > 0) return;
    this.limpiarContrasenas();
    this.mensaje.set('');
    this.error.set('');
  }

  /**
   * Los campos de contraseña empiezan en solo lectura para que el navegador no los rellene
   * solo con la contraseña guardada; al darles clic se pueden escribir normalmente.
   */
  protected permitirEscribir(evento: Event): void {
    (evento.target as HTMLInputElement).readOnly = false;
  }

  /** ¿Se muestra el aviso dentro de esta sección? */
  protected avisoEn(seccion: Seccion): boolean {
    return this.seccionAviso() === seccion && (!!this.mensaje() || !!this.error());
  }

  protected cambiarNombre(): Promise<void> {
    return this.ejecutar('nombre', this.passwordNombre, async (correo) => {
      const nuevo = this.exigir(this.nombreNuevo, 'el nombre nuevo');
      if (nuevo === this.datos()?.nombre) throw new Error('El nombre nuevo es igual al actual.');
      await this.api.arco('cambiar-nombre', { correo, password: this.passwordNombre, nombreNuevo: nuevo });
      this.api.actualizarSesion({ nombre: nuevo }); // actualiza el nombre del encabezado
      this.nombreNuevo = '';
      this.passwordNombre = '';
      return `Listo, tu nombre ahora es «${nuevo}».`;
    });
  }

  protected cambiarTelefono(): Promise<void> {
    return this.ejecutar('telefono', this.passwordTelefono, async (correo) => {
      const nuevo = this.exigir(this.telefonoNuevo, 'el teléfono nuevo');
      if (!/^[0-9+\-\s]{7,20}$/.test(nuevo)) {
        throw new Error('El teléfono solo puede tener números (de 7 a 20).');
      }
      if (nuevo === this.datos()?.telefono) throw new Error('El teléfono nuevo es igual al actual.');
      await this.api.arco('cambiar-telefono', { correo, password: this.passwordTelefono, telefonoNuevo: nuevo });
      this.api.actualizarSesion({ telefono: nuevo });
      this.telefonoNuevo = '';
      this.passwordTelefono = '';
      return `Listo, tu teléfono ahora es ${nuevo}.`;
    });
  }

  /** Cambio de correo, paso 1: Spring manda un código al correo ACTUAL. */
  protected pedirCambioCorreo(): Promise<void> {
    return this.ejecutar('correo', this.passwordCorreo, async (correo) => {
      const nuevo = this.exigir(this.correoNuevo, 'el correo nuevo');
      const r = await this.api.arco('cambiar-correo/solicitar', {
        correo,
        password: this.passwordCorreo,
        correoNuevo: nuevo,
      });
      this.solicitudCorreo.set(r.solicitudId ?? null);
      this.correoPendiente.set(nuevo);
      this.codigoCorreo = '';
      this.passwordCorreo = '';
      this.pasoCorreo.set('actual');
      return `Te enviamos un código a tu correo actual (${correo}).`;
    });
  }

  /** Paso 2: con el código del correo actual, Spring manda otro código al correo NUEVO. */
  protected confirmarCorreoActual(): Promise<void> {
    return this.ejecutar('correo', null, async () => {
      const solicitudId = this.solicitudCorreo();
      if (solicitudId === null) throw new Error('Empieza el cambio de correo de nuevo.');
      await this.api.arco('cambiar-correo/confirmar-actual', {
        solicitudId,
        codigo: this.exigir(this.codigoCorreo, 'el código'),
      });
      this.codigoCorreo = '';
      this.pasoCorreo.set('nuevo');
      return `Código correcto. Ahora te enviamos otro código a ${this.correoPendiente()}.`;
    });
  }

  /** Paso 3: con el código del correo nuevo, se cambia el correo de la cuenta. */
  protected confirmarCorreoNuevo(): Promise<void> {
    return this.ejecutar('correo', null, async () => {
      const solicitudId = this.solicitudCorreo();
      const nuevo = this.correoPendiente();
      if (solicitudId === null || !nuevo) throw new Error('Empieza el cambio de correo de nuevo.');
      await this.api.arco('cambiar-correo/confirmar-nuevo', {
        solicitudId,
        codigo: this.exigir(this.codigoCorreo, 'el código'),
      });
      this.api.actualizarSesion({ correo: nuevo });
      this.reiniciarCambioCorreo();
      return `Listo, tu correo ahora es ${nuevo}.`;
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

  /** Cambia la contraseña, avisa y cierra la sesión para volver a entrar con la nueva. */
  protected cambiarPassword(): Promise<void> {
    return this.ejecutar(
      'password',
      this.passwordCambio,
      async (correo) => {
        const nueva = this.passwordNueva;
        if (!nueva) throw new Error('Escribe la contraseña nueva.');
        if (nueva.length < MINIMO_PASSWORD) {
          throw new Error(`La contraseña nueva debe tener al menos ${MINIMO_PASSWORD} caracteres.`);
        }
        if (nueva !== this.passwordConfirmacion) throw new Error('Las contraseñas nuevas no coinciden.');
        if (nueva === this.passwordCambio) throw new Error('La contraseña nueva debe ser diferente a la actual.');
        await this.api.arco('cambiar-password', { correo, password: this.passwordCambio, passwordNueva: nueva });
        this.limpiarContrasenas();
        this.empezarSalida();
        return `Tu contraseña se cambió. Por seguridad cerraremos tu sesión para que entres con la nueva.`;
      },
      false, // Spring ya cerró las sesiones: no se vuelven a pedir los datos
    );
  }

  /** Cierra la sesión ahora (botón del aviso) o al terminar la cuenta regresiva. */
  protected salirTrasCambio(): void {
    clearInterval(this.temporizadorSalida);
    this.api.olvidarSesion(); // el token ya no sirve: Spring cerró las sesiones al cambiar la contraseña
    void this.router.navigate(['/login'], { queryParams: { aviso: 'password' } });
  }

  private empezarSalida(): void {
    this.segundosParaSalir.set(SEGUNDOS_ANTES_DE_SALIR);
    this.temporizadorSalida = setInterval(() => {
      const quedan = this.segundosParaSalir() - 1;
      if (quedan <= 0) {
        this.salirTrasCambio();
      } else {
        this.segundosParaSalir.set(quedan);
      }
    }, 1000);
  }

  /** Cancelar suscripcion, paso 1: Spring manda un codigo al correo. */
  protected pedirCodigoCancelacion(): Promise<void> {
    return this.ejecutar('cancelar', this.passwordCancelar, async (correo) => {
      const r = await this.api.arco('cancelar/solicitar', { correo, password: this.passwordCancelar });
      this.solicitudCancelacion.set(r.solicitudId ?? null);
      this.passwordCancelar = '';
      return 'Te enviamos un código a tu correo para confirmar la cancelación (vale 10 minutos).';
    });
  }

  /** Cancelar suscripcion, paso 2: con el codigo correcto la cuenta queda cancelada. */
  protected confirmarCancelacion(): Promise<void> {
    return this.ejecutar('cancelar', null, async () => {
      const solicitudId = this.solicitudCancelacion();
      if (solicitudId === null) throw new Error('Primero pide el código.');
      await this.api.arco('cancelar/confirmar', {
        solicitudId,
        codigo: this.exigir(this.codigoCancelacion, 'el código'),
      });
      this.solicitudCancelacion.set(null);
      this.codigoCancelacion = '';
      this.api.actualizarSesion({ suscripcion: 'cancelada' });
      return 'Tu suscripción se canceló.';
    });
  }

  private async cargarDatos(): Promise<void> {
    if (!this.api.haIniciadoSesion()) return;
    try {
      const d = await this.api.datos();
      this.datos.set(d);
      // Mantiene el encabezado igual a lo que dice la base
      this.api.actualizarSesion({ nombre: d.nombre, correo: d.correo, telefono: d.telefono });
    } catch (e) {
      this.seccionAviso.set(null);
      this.error.set(mensajeDeError(e));
    }
  }

  /**
   * Ejecuta una operación y muestra el resultado (o el error) dentro de su sección.
   * passwordSeccion: la contraseña escrita en esa sección (null si la operación no la pide).
   */
  private async ejecutar(
    seccion: Seccion,
    passwordSeccion: string | null,
    accion: (correo: string) => Promise<string>,
    refrescar = true,
  ): Promise<void> {
    if (this.cargando() || this.segundosParaSalir() > 0) return;
    this.mensaje.set('');
    this.error.set('');
    this.seccionAviso.set(seccion);
    const correo = this.api.usuario()?.correo;
    if (!correo) {
      this.error.set('No hay sesión iniciada.');
      return;
    }
    if (passwordSeccion !== null && !passwordSeccion) {
      this.error.set('Escribe tu contraseña actual para confirmar que eres tú.');
      return;
    }
    this.cargando.set(true);
    try {
      this.mensaje.set(await accion(correo));
      if (refrescar) await this.cargarDatos();
    } catch (e) {
      const texto = e instanceof Error && !('status' in e) ? e.message : mensajeDeError(e);
      // Spring manda los textos sin acentos; este es el que más se ve en Mi cuenta
      this.error.set(
        texto === 'La contrasena actual no es correcta' ? 'La contraseña actual no es correcta.' : texto,
      );
    } finally {
      this.cargando.set(false);
    }
  }

  private limpiarContrasenas(): void {
    this.passwordNombre = '';
    this.passwordTelefono = '';
    this.passwordCorreo = '';
    this.passwordCambio = '';
    this.passwordCancelar = '';
    this.passwordNueva = '';
    this.passwordConfirmacion = '';
  }

  private exigir(valor: string, que: string): string {
    const limpio = valor.trim();
    if (!limpio) throw new Error(`Escribe ${que}.`);
    return limpio;
  }
}
