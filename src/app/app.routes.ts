import { Routes } from '@angular/router';
import { Buscar } from './paginas/buscar/buscar';
import { Detalle } from './paginas/detalle/detalle';
import { Login } from './paginas/login/login';
import { MiLista } from './paginas/mi-lista/mi-lista';
import { Administracion } from './paginas/administracion/administracion';
import { administradorGuard } from './servicios/administrador.guard';
import { SpringRecuperar } from './spring/spring-recuperar/spring-recuperar';
import { SpringCuenta } from './spring/spring-cuenta/spring-cuenta';
import { springGuard } from './spring/spring.guard';

export const routes: Routes = [
  { path: 'buscar', component: Buscar },
  { path: 'titulo/:tipo/:id', component: Detalle },
  { path: 'mi-lista', component: MiLista },
  { path: 'login', component: Login },
  { path: 'administracion', component: Administracion, canActivate: [administradorGuard] },
  // La pantalla de acceso de Spring ya no existe aparte: hay un solo login.
  { path: 'spring/acceso', redirectTo: 'login' },
  { path: 'spring/recuperar', component: SpringRecuperar },
  { path: 'spring/cuenta', component: SpringCuenta, canActivate: [springGuard] },
  // Catálogos se quitó de la página (Noel, 10 nov): las tablas siguen en Spring y en Postman.
  // Los archivos siguen en src/app/spring/spring-catalogos por si se quiere regresar.
  { path: '**', redirectTo: 'buscar' },
];
