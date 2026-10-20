import { Routes } from '@angular/router';
import { hrGuard } from './core/api';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./pages/login').then((m) => m.Login) },
  {
    path: 'overview',
    canActivate: [hrGuard],
    data: { view: 'overview' },
    loadComponent: () => import('./pages/workspace').then((m) => m.Workspace),
  },
  {
    path: 'employees',
    canActivate: [hrGuard],
    data: { view: 'employees' },
    loadComponent: () => import('./pages/workspace').then((m) => m.Workspace),
  },
  {
    path: 'employees/new',
    canActivate: [hrGuard],
    loadComponent: () => import('./pages/employee-form').then((m) => m.EmployeeForm),
  },
  {
    path: 'employees/:id',
    canActivate: [hrGuard],
    loadComponent: () => import('./pages/employee-form').then((m) => m.EmployeeForm),
  },
  { path: '', pathMatch: 'full', redirectTo: 'overview' },
  { path: '**', redirectTo: 'overview' },
];
