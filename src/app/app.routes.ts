import { Routes } from '@angular/router';
import { Buscar } from './paginas/buscar/buscar';
import { Detalle } from './paginas/detalle/detalle';
import { Login } from './paginas/login/login';
import { MiLista } from './paginas/mi-lista/mi-lista';

export const routes: Routes = [
  { path: 'buscar', component: Buscar },
  { path: 'titulo/:tipo/:id', component: Detalle },
  { path: 'mi-lista', component: MiLista },
  { path: 'login', component: Login },
  { path: '**', redirectTo: 'buscar' },
];