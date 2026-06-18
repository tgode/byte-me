import { Component, OnInit, signal, effect } from '@angular/core';
import { RouterOutlet } from '@angular/router';

export type Theme = 'light' | 'dark';

// Global theme signal — components can inject AppComponent or use a service
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
export class AppComponent implements OnInit {

  ngOnInit(): void {
    // Load persisted theme
    const stored = localStorage.getItem('bytehr-theme') as Theme | null;
    if (stored === 'dark' || stored === 'light') {
      appTheme.set(stored);
    } else {
      // Respect OS preference on first load
      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
      appTheme.set(prefersDark ? 'dark' : 'light');
    }

    // Apply theme to <html> element whenever signal changes
    effect(() => {
      const t = appTheme();
      document.documentElement.setAttribute('data-theme', t);
      localStorage.setItem('bytehr-theme', t);
    });
  }
}
