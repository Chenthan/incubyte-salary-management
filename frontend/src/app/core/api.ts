import { HttpClient, HttpErrorResponse, HttpInterceptorFn, HttpParams } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, firstValueFrom, throwError } from 'rxjs';
import {
  ApiProblem,
  Employee,
  EmployeeInput,
  EmployeePage,
  Identity,
  Query,
  ReferenceData,
  Summary,
} from './models';

@Injectable({ providedIn: 'root' })
export class SessionState {
  readonly identity = signal<Identity | null>(null);
  readonly csrf = signal<{ token: string; headerName: string } | null>(null);
}

export const sessionInterceptor: HttpInterceptorFn = (request, next) => {
  const session = inject(SessionState);
  const router = inject(Router);
  const csrf = session.csrf();
  if (request.url.startsWith('/api/') && csrf && /^(POST|PUT|PATCH|DELETE)$/.test(request.method)) {
    request = request.clone({ setHeaders: { [csrf.headerName]: csrf.token } });
  }
  return next(request).pipe(
    catchError((error: HttpErrorResponse) => {
      if (
        error.status === 401 &&
        !request.url.endsWith('/auth/login') &&
        !request.url.endsWith('/auth/me')
      ) {
        session.identity.set(null);
        session.csrf.set(null);
        void router.navigate(['/login'], { queryParams: { expired: '1' } });
      }
      return throwError(() => error);
    }),
  );
};

export function problem(error: unknown): ApiProblem {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0)
      return { detail: 'Cannot reach the workspace. Check the connection and try again.' };
    const body: unknown = error.error;
    if (body && typeof body === 'object' && 'detail' in body) return body as ApiProblem;
    return {
      status: error.status,
      detail:
        error.status === 401
          ? 'Sign-in failed. Check your username and password.'
          : 'The request failed. Please try again.',
    };
  }
  return { detail: 'Something went wrong. Please try again.' };
}

@Injectable({ providedIn: 'root' })
export class Api {
  private readonly http = inject(HttpClient);
  readonly session = inject(SessionState);
  async csrf(): Promise<void> {
    this.session.csrf.set(
      await firstValueFrom(this.http.get<{ token: string; headerName: string }>('/api/auth/csrf')),
    );
  }
  async currentUser(): Promise<boolean> {
    try {
      this.session.identity.set(await firstValueFrom(this.http.get<Identity>('/api/auth/me')));
      await this.csrf();
      return true;
    } catch {
      this.session.identity.set(null);
      return false;
    }
  }
  async login(username: string, password: string): Promise<void> {
    await this.csrf();
    await firstValueFrom(
      this.http.post(
        '/api/auth/login',
        new HttpParams().set('username', username).set('password', password),
      ),
    );
    if (!(await this.currentUser())) throw new Error('Session initialization failed');
  }
  async logout(): Promise<void> {
    await firstValueFrom(this.http.post('/api/auth/logout', {}));
    this.session.identity.set(null);
    this.session.csrf.set(null);
  }
  referenceData() {
    return this.http.get<ReferenceData>('/api/reference-data');
  }
  employees(query: Query) {
    return this.http.get<EmployeePage>('/api/employees', { params: query });
  }
  summary(query: Query) {
    return this.http.get<Summary>('/api/reports/salary-summary', { params: query });
  }
  employee(id: string) {
    return this.http.get<Employee>('/api/employees/' + encodeURIComponent(id));
  }
  create(input: EmployeeInput) {
    return this.http.post<Employee>('/api/employees', input);
  }
  update(id: string, input: EmployeeInput) {
    return this.http.put<Employee>('/api/employees/' + encodeURIComponent(id), input);
  }
  status(employee: Employee, active: boolean) {
    return this.http.patch<Employee>(
      '/api/employees/' + encodeURIComponent(employee.id) + '/status',
      { active, version: employee.version },
    );
  }
}

export const hrGuard: CanActivateFn = async (_route, state) => {
  const api = inject(Api);
  const router = inject(Router);
  return (
    (await api.currentUser()) ||
    router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } })
  );
};
