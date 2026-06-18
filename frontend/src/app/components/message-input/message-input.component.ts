import {
  Component, Output, EventEmitter, signal, computed, ElementRef, ViewChild
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';

@Component({
  selector: 'app-message-input',
  standalone: true,
  imports: [CommonModule, FormsModule, MatInputModule, MatButtonModule, MatIconModule, MatTooltipModule],
  template: `
    <div class="input-bar">
      <div class="input-wrapper">
        <textarea
          #textArea
          class="message-textarea"
          [(ngModel)]="messageText"
          (ngModelChange)="onInput()"
          (keydown.enter)="onEnter($event)"
          [disabled]="disabled()"
          placeholder="Ask an HR question…"
          rows="1"
          maxlength="2000"
          aria-label="Message input"
        ></textarea>
        <span class="char-hint" [class.warn]="charCount() > 1800">
          {{ charCount() > 1600 ? charCount() + '/2000' : '' }}
        </span>
      </div>
      <button
        class="send-btn"
        [class.active]="canSend()"
        [disabled]="!canSend()"
        (click)="sendMessage()"
        [attr.aria-label]="'Send message'"
        matTooltip="Send (Enter)">
        <mat-icon>send</mat-icon>
      </button>
    </div>
  `,
  styles: [`
    .input-bar {
      display: flex;
      align-items: flex-end;
      gap: 10px;
      padding: 12px 16px 14px;
      background: var(--c-surface);
      border-top: 1px solid var(--c-border);
      flex-shrink: 0;
    }
    .input-wrapper {
      flex: 1;
      position: relative;
      background: var(--c-bg);
      border: 1.5px solid var(--c-border);
      border-radius: 22px;
      transition: border-color 0.2s, box-shadow 0.2s;
    }
    .input-wrapper:focus-within {
      border-color: var(--c-primary);
      background: var(--c-surface);
      box-shadow: 0 0 0 2px rgba(0,120,212,0.12);
    }
    .message-textarea {
      display: block;
      width: 100%;
      border: none;
      background: transparent;
      border-radius: 22px;
      padding: 10px 44px 10px 18px;
      font-family: inherit;
      font-size: 14px;
      line-height: 1.5;
      resize: none;
      outline: none;
      color: var(--c-text);
      max-height: 120px;
      overflow-y: auto;
    }
    .message-textarea::placeholder { color: var(--c-text-muted); }
    .message-textarea:disabled { cursor: not-allowed; opacity: 0.6; }
    .char-hint {
      position: absolute;
      right: 14px;
      bottom: 8px;
      font-size: 10px;
      color: var(--c-text-muted);
      pointer-events: none;
    }
    .char-hint.warn { color: var(--c-error); }

    /* Send button */
    .send-btn {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      border: none;
      background: var(--c-border);
      color: var(--c-text-muted);
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: default;
      flex-shrink: 0;
      transition: background 0.2s, color 0.2s, transform 0.1s;
      mat-icon { font-size: 20px; width: 20px; height: 20px; }
    }
    .send-btn.active {
      background: var(--c-primary);
      color: #fff;
      cursor: pointer;
    }
    .send-btn.active:hover  { background: var(--c-primary-dark); transform: scale(1.05); }
    .send-btn.active:active { transform: scale(0.95); }
    .send-btn:disabled { opacity: 1; } /* override browser default — we handle style manually */
  `]
})
export class MessageInputComponent {
  @Output() messageSent = new EventEmitter<string>();
  @ViewChild('textArea') textArea!: ElementRef<HTMLTextAreaElement>;

  messageText = '';
  readonly disabled = signal(false);
  readonly charCount = computed(() => this.messageText.length);
  readonly canSend = computed(() =>
    this.messageText.trim().length > 0 && !this.disabled()
  );

  setDisabled(value: boolean): void {
    this.disabled.set(value);
  }

  onInput(): void {
    const el = this.textArea?.nativeElement;
    if (el) {
      el.style.height = 'auto';
      el.style.height = Math.min(el.scrollHeight, 120) + 'px';
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
