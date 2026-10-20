import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Api, problem, SessionState } from './core/api';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  readonly session = inject(SessionState);
  private readonly api = inject(Api);
  private readonly router = inject(Router);
  readonly error = signal('');
  readonly signingOut = signal(false);
  async logout(): Promise<void> {
    this.signingOut.set(true);
    this.error.set('');
    try {
      await this.api.logout();
      await this.router.navigate(['/login']);
    } catch (error) {
      this.error.set(problem(error).detail ?? 'Sign-out failed.');
    } finally {
      this.signingOut.set(false);
    }
  }
}
