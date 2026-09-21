import { AfterViewInit, Component, ElementRef, inject, viewChild } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../servicios/auth.service';

@Component({
  selector: 'app-login',
  imports: [RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login implements AfterViewInit {
  protected readonly auth = inject(AuthService);
  private readonly botonGoogle = viewChild.required<ElementRef<HTMLElement>>('botonGoogle');

  ngAfterViewInit(): void {
    this.auth.mostrarBotonGoogle(this.botonGoogle().nativeElement);
  }
}