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
    <div class="input-container">
      <div class="input-wrapper">
        <textarea
          #textArea
          class="message-textarea"
          [(ngModel)]="messageText"
          (ngModelChange)="onInput()"
          (keydown.enter)="onEnter($event)"
          [disabled]="disabled()"
          placeholder="Ask an HR question… (Enter to send, Shift+Enter for new line)"
          rows="1"
          maxlength="2000"
          aria-label="Message input"
        ></textarea>
        <div class="char-count" [class.warn]="charCount() > 1800">
          {{ charCount() }}/2000
        </div>
      </div>
      <button
        mat-icon-button
        class="send-button"
        [disabled]="!canSend()"
        (click)="sendMessage()"
        matTooltip="Send message"
        aria-label="Send message">
        <mat-icon>send</mat-icon>
      </button>
    </div>
  `,
  styles: [`
    .input-container {
      display: flex;
      align-items: flex-end;
      gap: 8px;
      padding: 12px 16px;
      border-top: 1px solid #E1DFDD;
      background: #fff;
    }
    .input-wrapper {
      flex: 1;
      position: relative;
    }
    .message-textarea {
      width: 100%;
      border: 1.5px solid #E1DFDD;
      border-radius: 22px;
      padding: 10px 50px 10px 16px;
      font-family: inherit;
      font-size: 14px;
      line-height: 1.5;
      resize: none;
      outline: none;
      transition: border-color 0.2s;
      background: #F5F5F5;
      max-height: 120px;
      overflow-y: auto;
      color: #201F1E;
    }
    .message-textarea:focus {
      border-color: #0078D4;
      background: #fff;
    }
    .message-textarea:disabled {
      background: #F3F2F1;
      cursor: not-allowed;
    }
    .char-count {
      position: absolute;
      right: 12px;
      bottom: 8px;
      font-size: 11px;
      color: #A19F9D;
    }
    .char-count.warn { color: #D83B01; }
    .send-button {
      background: #0078D4 !important;
      color: #fff !important;
      width: 44px;
      height: 44px;
      flex-shrink: 0;
    }
    .send-button:disabled {
      background: #E1DFDD !important;
      color: #A19F9D !important;
    }
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
