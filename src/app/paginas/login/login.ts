
import {
  AfterViewInit,
  Component,
  ElementRef,
  inject,
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
  protected mensaje = '';

  ngAfterViewInit(): void {
    this.auth.mostrarBotonGoogle(
      this.botonGoogle().nativeElement,
    );
  }

  protected cambiarModo(modo: ModoAcceso): void {
    this.modo = modo;
    this.mensaje = '';
    this.nombre = '';
    this.apellidos = '';
    this.contrasena = '';
    this.confirmacion = '';
  }

  protected enviarFormulario(): void {
    this.mensaje = '';

    if (
      this.modo === 'registro' &&
      (!this.nombre.trim() || !this.apellidos.trim())
    ) {
      this.mensaje = 'Ingresa tu nombre y tus apellidos.';
      return;
    }

    if (!this.correo.trim() || !this.contrasena) {
      this.mensaje = 'Completa todos los campos.';
      return;
    }

    if (this.contrasena.length < 8) {
      this.mensaje = 'La contraseña debe tener al menos 8 caracteres.';
      return;
    }

    if (
      this.modo === 'registro' &&
      this.contrasena !== this.confirmacion
    ) {
      this.mensaje = 'Las contraseñas no coinciden.';
      return;
    }

    this.mensaje =
      'El formulario está validado, pero falta conectar el servicio de autenticación.';
  }
}
