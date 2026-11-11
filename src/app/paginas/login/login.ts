import { Component, OnDestroy, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../servicios/auth.service';

type ModoAcceso = 'login' | 'registro';

const SEGUNDOS_ESPERA = 60;

@Component({
  selector: 'app-login',
  imports: [RouterLink, FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login implements OnDestroy {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected modo: ModoAcceso = 'login';
  protected nombre = '';
  protected telefono = '';
  protected correo = '';
  protected contrasena = '';
  protected confirmacion = '';
  protected codigo = '';

  /** Registro, paso 2: ya se mandó el código al correo y falta escribirlo. */
  protected readonly esperandoCodigo = signal(false);
  protected readonly segundosEspera = signal(0);
  protected readonly mensaje = signal('');
  protected readonly aviso = signal('');
  protected readonly cargando = signal(false);

  /** Aviso al llegar desde Mi cuenta después de cambiar la contraseña (?aviso=password). */
  protected readonly avisoLlegada = signal(
    inject(ActivatedRoute).snapshot.queryParamMap.get('aviso') === 'password'
      ? 'Tu contraseña se cambió. Inicia sesión con tu contraseña nueva.'
      : '',
  );

  private solicitudId: number | null = null;
  private temporizador?: ReturnType<typeof setInterval>;

  ngOnDestroy(): void {
    clearInterval(this.temporizador);
  }

  protected cambiarModo(modo: ModoAcceso): void {
    this.modo = modo;
    this.reiniciarRegistro();
    this.nombre = '';
    this.telefono = '';
    this.contrasena = '';
    this.confirmacion = '';
    this.mensaje.set('');
  }

  /** Vuelve al formulario de datos (por ejemplo, para corregir el correo). */
  protected corregirDatos(): void {
    this.reiniciarRegistro();
    this.mensaje.set('');
  }

  protected async enviarFormulario(): Promise<void> {
    if (this.cargando()) return;
    this.mensaje.set('');
    this.aviso.set('');
    const registro = this.modo === 'registro';

    if (registro && !this.nombre.trim()) {
      this.mensaje.set('Ingresa tu nombre.');
      return;
    }
    if (!this.correo.trim() || !this.contrasena) {
      this.mensaje.set('Completa todos los campos obligatorios.');
      return;
    }
    if (registro) {
      if (this.contrasena.length < 8) {
        this.mensaje.set('La contraseña debe tener al menos 8 caracteres.');
        return;
      }
      if (this.contrasena !== this.confirmacion) {
        this.mensaje.set('Las contraseñas no coinciden.');
        return;
      }
    }

    this.cargando.set(true);
    try {
      if (registro) {
        // Paso 1: Spring manda el código al correo; la cuenta todavía no existe.
        const r = await this.auth.solicitarRegistro({
          nombre: this.nombre,
          correo: this.correo,
          contrasena: this.contrasena,
          telefono: this.telefono,
        });
        if (!r.ok || r.solicitudId === undefined) {
          this.mensaje.set(r.mensaje);
          return;
        }
        this.solicitudId = r.solicitudId;
        this.codigo = '';
        this.esperandoCodigo.set(true);
        this.aviso.set(r.mensaje);
        this.iniciarEspera();
      } else {
        const r = await this.auth.iniciarSesion(this.correo, this.contrasena);
        if (r.ok) {
          this.contrasena = '';
          await this.router.navigate(['/buscar']);
        } else {
          this.mensaje.set(r.mensaje);
        }
      }
    } finally {
      this.cargando.set(false);
    }
  }

  /** Paso 2 del registro: confirma el código y crea la cuenta. */
  protected async confirmarCodigo(): Promise<void> {
    if (this.cargando() || this.solicitudId === null) return;
    this.mensaje.set('');
    this.aviso.set('');
    if (!/^\d{6}$/.test(this.codigo.trim())) {
      this.mensaje.set('Escribe el código de 6 dígitos que llegó a tu correo.');
      return;
    }
    this.cargando.set(true);
    try {
      const r = await this.auth.confirmarRegistro(this.solicitudId, this.codigo);
      if (r.ok) {
        this.reiniciarRegistro();
        this.contrasena = '';
        this.confirmacion = '';
        await this.router.navigate(['/buscar']);
      } else {
        this.mensaje.set(r.mensaje);
      }
    } finally {
      this.cargando.set(false);
    }
  }

  protected async reenviarCodigo(): Promise<void> {
    if (this.cargando() || this.solicitudId === null || this.segundosEspera() > 0) return;
    this.mensaje.set('');
    this.aviso.set('');
    this.cargando.set(true);
    try {
      const r = await this.auth.reenviarRegistro(this.solicitudId);
      if (r.ok) {
        this.aviso.set(r.mensaje);
        this.codigo = '';
        this.iniciarEspera();
      } else {
        this.mensaje.set(r.mensaje);
      }
    } finally {
      this.cargando.set(false);
    }
  }

  private iniciarEspera(): void {
    clearInterval(this.temporizador);
    this.segundosEspera.set(SEGUNDOS_ESPERA);
    this.temporizador = setInterval(() => {
      const restantes = this.segundosEspera() - 1;
      this.segundosEspera.set(Math.max(restantes, 0));
      if (restantes <= 0) clearInterval(this.temporizador);
    }, 1000);
  }

  private reiniciarRegistro(): void {
    clearInterval(this.temporizador);
    this.esperandoCodigo.set(false);
    this.segundosEspera.set(0);
    this.aviso.set('');
    this.solicitudId = null;
    this.codigo = '';
  }
}
