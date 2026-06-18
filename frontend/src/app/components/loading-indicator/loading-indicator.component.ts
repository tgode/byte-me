import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'app-loading-indicator',
  standalone: true,
  imports: [CommonModule, MatProgressSpinnerModule],
  template: `
    <div class="loading-wrapper">
      <div class="dot-typing">
        <span></span><span></span><span></span>
      </div>
      <span class="loading-text">ByteHR AI is thinking…</span>
    </div>
  `,
  styles: [`
    .loading-wrapper {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .dot-typing {
      display: flex;
      gap: 4px;
    }
    .dot-typing span {
      width: 7px;
      height: 7px;
      border-radius: 50%;
      background: var(--c-primary);
      animation: bounce 1.2s infinite ease-in-out;
      display: block;
    }
    .dot-typing span:nth-child(1) { animation-delay: 0s; }
    .dot-typing span:nth-child(2) { animation-delay: 0.2s; }
    .dot-typing span:nth-child(3) { animation-delay: 0.4s; }
    @keyframes bounce {
      0%, 60%, 100% { transform: translateY(0);    opacity: 0.4; }
      30%            { transform: translateY(-5px); opacity: 1;   }
    }
    .loading-text {
      font-size: 12px;
      color: var(--c-text-secondary);
      font-style: italic;
    }
  `]
})
export class LoadingIndicatorComponent {}
