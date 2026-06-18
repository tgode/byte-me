import {
  Component, Output, EventEmitter, signal, computed, ElementRef, ViewChild
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';

@Component({
  selector: 'app-message-input',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatIconModule, MatTooltipModule],
  template: `
    <div class="input-area">

      <div class="composer" [class.composer--focused]="isFocused()" [class.composer--disabled]="disabled()">

        <textarea
          #textArea
          class="composer-textarea"
          [(ngModel)]="messageText"
          (ngModelChange)="onTextChange()"
          (keydown.enter)="onEnter($event)"
          (focus)="isFocused.set(true)"
          (blur)="isFocused.set(false)"
          [disabled]="disabled()"
          placeholder="Ask an HR question…"
          rows="1"
          maxlength="2000"
          aria-label="Type your message"
        ></textarea>

        <div class="composer-actions">
          @if (charCount() > 1600) {
            <span class="char-count" [class.char-warn]="charCount() > 1900">
              {{ 2000 - charCount() }}
            </span>
          }
          <button
            class="send-btn"
            [class.send-btn--active]="canSend()"
            [disabled]="!canSend()"
            (click)="sendMessage()"
            matTooltip="Send (Enter)"
            aria-label="Send message">
            <mat-icon>send</mat-icon>
          </button>
        </div>
      </div>

      <p class="input-hint">
        <mat-icon inline>lock</mat-icon>
        Answers are based exclusively on your company HR documents
      </p>
    </div>
  `,
  styles: [`
    :host { display: contents; }

    .input-area {
      padding: 10px 20px 12px;
      background: var(--c-surface);
      border-top: 1px solid var(--c-border);
      flex-shrink: 0;
      transition: background var(--t-slow), border-color var(--t-slow);
    }

    /* ── Composer box ── */
    .composer {
      display: flex;
      align-items: flex-end;
      gap: 0;
      background: var(--c-bg);
      border: 1.5px solid var(--c-border);
      border-radius: var(--radius-xl);
      transition:
        border-color var(--t-base),
        box-shadow var(--t-base),
        background var(--t-slow);
      overflow: hidden;
    }
    .composer--focused {
      border-color: var(--c-primary);
      background: var(--c-surface);
      box-shadow: var(--shadow-glow);
    }
    .composer--disabled {
      opacity: 0.55;
      cursor: not-allowed;
    }

    .composer-textarea {
      flex: 1;
      border: none;
      background: transparent;
      padding: 11px 8px 11px 18px;
      font-family: inherit;
      font-size: 14px;
      line-height: 1.5;
      resize: none;
      outline: none;
      color: var(--c-text);
      max-height: 130px;
      overflow-y: auto;
    }
    .composer-textarea::placeholder {
      color: var(--c-text-muted);
    }
    .composer-textarea:disabled {
      cursor: not-allowed;
    }

    .composer-actions {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 8px 10px 8px 4px;
      flex-shrink: 0;
    }

    .char-count {
      font-size: 11px;
      color: var(--c-text-muted);
      min-width: 28px;
      text-align: right;
    }
    .char-warn { color: var(--c-error) !important; font-weight: 600; }

    /* ── Send button ── */
    .send-btn {
      width: 36px;
      height: 36px;
      border-radius: 50%;
      border: none;
      background: var(--c-border);
      color: var(--c-text-muted);
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: default;
      flex-shrink: 0;
      transition:
        background var(--t-base),
        color var(--t-base),
        transform var(--t-fast),
        box-shadow var(--t-base);
      mat-icon { font-size: 18px; width: 18px; height: 18px; }
    }
    .send-btn--active {
      background: var(--c-primary);
      color: #fff;
      cursor: pointer;
      box-shadow: var(--shadow-sm);
    }
    .send-btn--active:hover {
      background: var(--c-primary-dark);
      transform: scale(1.06);
      box-shadow: var(--shadow-md);
    }
    .send-btn--active:active { transform: scale(0.94); }
    .send-btn:disabled { opacity: 1; }

    /* ── Footer hint ── */
    .input-hint {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 4px;
      margin: 7px 0 0;
      font-size: 11px;
      color: var(--c-text-muted);
      mat-icon { font-size: 12px !important; opacity: 0.7; }
    }
  `]
})
export class MessageInputComponent {
  @Output() messageSent = new EventEmitter<string>();
  @ViewChild('textArea') textArea!: ElementRef<HTMLTextAreaElement>;

  messageText = '';
  readonly disabled  = signal(false);
  readonly isFocused = signal(false);
  readonly charCount = computed(() => this.messageText.length);
  readonly canSend   = computed(() =>
    this.messageText.trim().length > 0 && !this.disabled()
  );

  setDisabled(value: boolean): void {
    this.disabled.set(value);
  }

  onTextChange(): void {
    const el = this.textArea?.nativeElement;
    if (el) {
      el.style.height = 'auto';
      el.style.height = Math.min(el.scrollHeight, 130) + 'px';
    }
  }

  onEnter(event: Event): void {
    if (!(event as KeyboardEvent).shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  sendMessage(): void {
    const text = this.messageText.trim();
    if (!text || this.disabled()) return;
    this.messageSent.emit(text);
    this.messageText = '';
    if (this.textArea?.nativeElement) {
      this.textArea.nativeElement.style.height = 'auto';
    }
  }
}
