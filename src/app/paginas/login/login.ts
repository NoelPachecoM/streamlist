
import {
  AfterViewInit,
  Component,
  ElementRef,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../servicios/auth.service';

type ModoAcceso = 'login' | 'registro';

@Component({
  selector: 'app-login',
  imports: [RouterLink, FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login implements AfterViewInit {
  protected readonly auth = inject(AuthService);

  private readonly botonGoogle =
    viewChild.required<ElementRef<HTMLElement>>('botonGoogle');

  protected modo: ModoAcceso = 'login';
  protected nombre = '';
  protected apellidos = '';
  protected correo = '';
  protected contrasena = '';
  protected confirmacion = '';
  protected readonly mensaje = signal('');
  protected readonly cargando = signal(false);

  ngAfterViewInit(): void {
    this.auth.mostrarBotonGoogle(
      this.botonGoogle().nativeElement,
    );
  }

  protected cambiarModo(modo: ModoAcceso): void {
    this.modo = modo;
    this.mensaje.set('');
    this.nombre = '';
    this.apellidos = '';
    this.contrasena = '';
    this.confirmacion = '';
  }

  protected async enviarFormulario(): Promise<void> {
    if (this.cargando()) return;
    this.mensaje.set('');

    if (
      this.modo === 'registro' &&
      (!this.nombre.trim() || !this.apellidos.trim())
    ) {
      this.mensaje.set('Ingresa tu nombre y tus apellidos.');
      return;
    }

    if (!this.correo.trim() || !this.contrasena) {
      this.mensaje.set('Completa todos los campos.');
      return;
    }

    if (this.contrasena.length < 8) {
      this.mensaje.set('La contraseña debe tener al menos 8 caracteres.');
      return;
    }

    if (
      this.modo === 'registro' &&
      this.contrasena !== this.confirmacion
    ) {
      this.mensaje.set('Las contraseñas no coinciden.');
      return;
    }

    this.cargando.set(true);
    try {
      if (this.modo === 'registro') {
        const resultado = await this.auth.registrarse({
          nombre: this.nombre,
          apellidos: this.apellidos,
          correo: this.correo,
          contrasena: this.contrasena,
        });
        if (resultado.ok && !this.auth.haIniciadoSesion()) {
          // Falta confirmar el correo: se deja listo el formulario de inicio de sesión.
          this.modo = 'login';
          this.nombre = '';
          this.apellidos = '';
          this.confirmacion = '';
        }
        this.contrasena = resultado.ok ? '' : this.contrasena;
        this.mensaje.set(resultado.mensaje);
      } else {
        const resultado = await this.auth.iniciarSesion(this.correo, this.contrasena);
        if (resultado.ok) this.contrasena = '';
        else this.mensaje.set(resultado.mensaje);
      }
    } finally {
      this.cargando.set(false);
    }
  }
}
