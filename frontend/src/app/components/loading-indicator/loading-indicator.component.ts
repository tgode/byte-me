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
      gap: 10px;
      padding: 8px 14px;
      background: #fff;
      border-radius: 18px;
      border-top-left-radius: 4px;
      box-shadow: 0 1px 4px rgba(0,0,0,0.1);
      max-width: 180px;
    }
    .dot-typing {
      display: flex;
      gap: 5px;
    }
    .dot-typing span {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: #0078D4;
      animation: bounce 1.2s infinite ease-in-out;
    }
    .dot-typing span:nth-child(1) { animation-delay: 0s; }
    .dot-typing span:nth-child(2) { animation-delay: 0.2s; }
    .dot-typing span:nth-child(3) { animation-delay: 0.4s; }
    @keyframes bounce {
      0%, 80%, 100% { transform: scale(0.6); opacity: 0.5; }
      40%            { transform: scale(1.0); opacity: 1; }
    }
    .loading-text {
      font-size: 12px;
      color: #605E5C;
      font-style: italic;
    }
  `]
})
export class LoadingIndicatorComponent {}
