import { Component, OnInit, signal, effect, inject, DestroyRef } from '@angular/core';
import { RouterOutlet } from '@angular/router';

export type Theme = 'light' | 'dark';

// Global theme signal — any component can import and use this directly
export const appTheme = signal<Theme>('light');

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  template: `<router-outlet />`,
  styles: [`
    :host {
      display: block;
      height: 100vh;
      overflow: hidden;
    }
  `]
})
export class AppComponent {
  constructor() {
    // Load persisted or OS-preferred theme BEFORE first render
    const stored = localStorage.getItem('bytehr-theme') as Theme | null;
    if (stored === 'dark' || stored === 'light') {
      appTheme.set(stored);
    } else {
      appTheme.set(
        window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
      );
    }

    // effect() MUST be inside constructor (injection context) to work
    effect(() => {
      const t = appTheme();
      document.documentElement.setAttribute('data-theme', t);
      localStorage.setItem('bytehr-theme', t);
    });
  }
}
