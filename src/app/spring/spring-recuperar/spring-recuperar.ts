import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { SpringApiService, mensajeDeError } from '../spring-api.service';

@Component({
  selector: 'app-spring-recuperar',
  imports: [FormsModule, RouterLink],
  templateUrl: './spring-recuperar.html',
  styleUrl: '../spring.css',
})
export class SpringRecuperar {
  private readonly api = inject(SpringApiService);

  protected readonly paso = signal<1 | 2 | 3>(1);
  protected readonly mensaje = signal('');
  protected readonly error = signal('');
  protected readonly cargando = signal(false);
  private solicitudId: number | null = null;

  protected correo = '';
  protected nombre = '';
  protected token = '';
  protected passwordNueva = '';
  protected confirmacion = '';

  /** Paso 1: Spring manda un token de 6 digitos al correo. */
  protected async solicitar(): Promise<void> {
    if (!this.correo.trim() || !this.nombre.trim()) {
      this.error.set('Escribe tu correo y tu nombre tal como los registraste.');
      return;
    }
    await this.ejecutar(async () => {
      const r = await this.api.arco('olvide-password/solicitar', {
        correo: this.correo.trim(),
        nombre: this.nombre.trim(),
      });
      this.solicitudId = r.solicitudId ?? null;
      this.paso.set(2);
      // La API puede devolver codigoDesarrollo (modo pruebas con Postman), pero la pantalla
      // nunca lo muestra: el código solo se conoce si llega al correo.
      return r.mensaje;
    });
  }

  /** Paso 2: con el token correcto se cambia la contraseña. */
  protected async confirmar(): Promise<void> {
    if (!this.token.trim() || !this.passwordNueva) {
      this.error.set('Escribe el token y tu contraseña nueva.');
      return;
    }
    if (this.passwordNueva !== this.confirmacion) {
      this.error.set('Las contraseñas no coinciden.');
      return;
    }
    if (this.solicitudId === null) {
      this.error.set('Primero solicita el token.');
      return;
    }
    await this.ejecutar(async () => {
      const r = await this.api.arco('olvide-password/confirmar', {
        solicitudId: this.solicitudId,
        token: this.token.trim(),
        passwordNueva: this.passwordNueva,
      });
      this.paso.set(3);
      return r.mensaje;
    });
  }

  private async ejecutar(accion: () => Promise<string>): Promise<void> {
    if (this.cargando()) return;
    this.mensaje.set('');
    this.error.set('');
    this.cargando.set(true);
    try {
      this.mensaje.set(await accion());
    } catch (e) {
      this.error.set(mensajeDeError(e));
    } finally {
      this.cargando.set(false);
    }
  }
}
