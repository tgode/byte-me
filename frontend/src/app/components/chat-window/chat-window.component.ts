import {
  Component, OnInit, OnDestroy, OnChanges, SimpleChanges,
  Input, Output, EventEmitter,
  signal, computed,
  ViewChild, ElementRef, AfterViewChecked, inject
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MessageListComponent } from '../message-list/message-list.component';
import { MessageInputComponent } from '../message-input/message-input.component';
import { ChatService } from '../../services/chat.service';
import { ChatMessage } from '../../models/chat.model';
import { uuidv4 } from '../../shared/uuid';

@Component({
  selector: 'app-chat-window',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatIconModule, MatButtonModule, MatTooltipModule, MatSnackBarModule,
    MessageListComponent, MessageInputComponent
  ],
  template: `
    <div class="chat-window">

      <!-- ── Header ── -->
      <header class="chat-header">
        <div class="header-left">
          <div class="header-avatar" [class.thinking]="isLoading()">
            <mat-icon>support_agent</mat-icon>
          </div>
          <div class="header-info">
            <span class="header-name">ByteHR AI</span>
            <span class="header-status">
              <span class="status-dot" [class.thinking]="isLoading()"></span>
              {{ isLoading() ? 'Thinking…' : 'HR Assistant · Online' }}
            </span>
          </div>
        </div>

        <div class="header-actions">
          <button mat-icon-button class="sm action-btn"
                  matTooltip="Sync HR documents"
                  (click)="syncDocuments()">
            <mat-icon>sync</mat-icon>
          </button>
          <button mat-icon-button class="sm action-btn"
                  matTooltip="Clear conversation"
                  (click)="clearConversation()">
            <mat-icon>delete_outline</mat-icon>
          </button>
        </div>
      </header>

      <!-- ── Messages ── -->
      <div class="messages-scroll" #scrollContainer>
        <app-message-list [messages]="messages()" />
      </div>

      <!-- ── Input ── -->
      <app-message-input
        #inputComponent
        (messageSent)="onMessageSent($event)"
      />

    </div>
  `,
  styles: [`
    :host { display: contents; }

    .chat-window {
      display: flex;
      flex-direction: column;
      height: 100%;
      background: var(--c-bg);
      overflow: hidden;
      transition: background var(--t-slow);
    }

    /* ── Header ── */
    .chat-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0 16px;
      height: var(--header-height);
      background: var(--c-surface);
      border-bottom: 1px solid var(--c-border);
      flex-shrink: 0;
      gap: 12px;
      transition: background var(--t-slow), border-color var(--t-slow);
    }
    .header-left {
      display: flex;
      align-items: center;
      gap: 10px;
    }
    .header-avatar {
      width: 34px;
      height: 34px;
      border-radius: 50%;
      background: var(--c-primary);
      color: #fff;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      transition: box-shadow var(--t-base);
      mat-icon { font-size: 20px; width: 20px; height: 20px; }
    }
    .header-avatar.thinking {
      box-shadow: 0 0 0 3px rgba(var(--c-primary-rgb), 0.25);
      animation: avatar-pulse 1.5s ease-in-out infinite;
    }
    @keyframes avatar-pulse {
      0%, 100% { box-shadow: 0 0 0 3px rgba(var(--c-primary-rgb), 0.25); }
      50%       { box-shadow: 0 0 0 6px rgba(var(--c-primary-rgb), 0.08); }
    }
    .header-info {
      display: flex;
      flex-direction: column;
      gap: 1px;
    }
    .header-name {
      font-size: 14px;
      font-weight: 600;
      color: var(--c-text);
      line-height: 1.2;
    }
    .header-status {
      display: flex;
      align-items: center;
      gap: 5px;
      font-size: 11.5px;
      color: var(--c-text-secondary);
      line-height: 1.2;
    }
    .status-dot {
      width: 7px;
      height: 7px;
      border-radius: 50%;
      background: var(--c-online);
      flex-shrink: 0;
      transition: background var(--t-base);
    }
    .status-dot.thinking {
      background: var(--c-thinking);
      animation: dot-blink 0.9s ease-in-out infinite;
    }
    @keyframes dot-blink {
      0%, 100% { opacity: 1; }
      50%       { opacity: 0.2; }
    }
    .header-actions {
      display: flex;
      align-items: center;
      gap: 2px;
    }
    .action-btn {
      color: var(--c-text-muted) !important;
      transition: color var(--t-fast) !important;
    }
    .action-btn:hover { color: var(--c-text) !important; }

    /* ── Messages scroll ── */
    .messages-scroll {
      flex: 1;
      overflow-y: auto;
      display: flex;
      flex-direction: column;
      scroll-behavior: smooth;
    }
  `]
})
export class ChatWindowComponent implements OnInit, OnChanges, OnDestroy, AfterViewChecked {

  @Input() conversationId = uuidv4();

  @Output() conversationStarted = new EventEmitter<{ id: string; title: string; preview: string }>();
  @Output() conversationCleared = new EventEmitter<void>();

  @ViewChild('scrollContainer') scrollContainer!: ElementRef<HTMLElement>;
  @ViewChild('inputComponent') inputComponent!: MessageInputComponent;

  private readonly chatService = inject(ChatService);
  private readonly snackBar    = inject(MatSnackBar);

  private readonly _messages  = signal<ChatMessage[]>([]);
  readonly messages  = this._messages.asReadonly();

  private readonly _isLoading = signal(false);
  readonly isLoading = this._isLoading.asReadonly();

  private shouldScrollToBottom = false;
  private suggestionListener!: EventListener;
  private readonly messageStore = new Map<string, ChatMessage[]>();

  ngOnInit(): void {
    this.suggestionListener = (e: Event) => {
      const text = (e as CustomEvent).detail as string;
      if (text) this.onMessageSent(text);
    };
    document.addEventListener('hr-suggestion', this.suggestionListener);
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['conversationId']) {
      const prev = changes['conversationId'].previousValue;
      if (prev) this.messageStore.set(prev, this._messages());
      const next = changes['conversationId'].currentValue as string;
      this._messages.set(this.messageStore.get(next) ?? []);
      this._isLoading.set(false);
      this.shouldScrollToBottom = true;
    }
  }

  ngOnDestroy(): void {
    document.removeEventListener('hr-suggestion', this.suggestionListener);
  }

  ngAfterViewChecked(): void {
    if (this.shouldScrollToBottom) {
      this.scrollToBottom();
      this.shouldScrollToBottom = false;
    }
  }

  onMessageSent(text: string): void {
    if (!text.trim() || this._isLoading()) return;

    const userMsg: ChatMessage = { id: uuidv4(), role: 'user',      content: text, timestamp: new Date() };
    const loadMsg: ChatMessage = { id: uuidv4(), role: 'assistant', content: '',   timestamp: new Date(), loading: true };

    this._messages.update(m => [...m, userMsg, loadMsg]);
    this._isLoading.set(true);
    this.inputComponent?.setDisabled(true);
    this.shouldScrollToBottom = true;

    const title = text.length > 40 ? text.slice(0, 40) + '…' : text;
    this.conversationStarted.emit({ id: this.conversationId, title, preview: text });

    this.chatService.sendMessage({ message: text, conversationId: this.conversationId }).subscribe({
      next: (res) => {
        this._messages.update(msgs => msgs.map(m =>
          m.id === loadMsg.id
            ? { ...m, content: res.answer, citations: res.citations, confidenceScore: res.confidenceScore, loading: false }
            : m
        ));
        this._isLoading.set(false);
        this.inputComponent?.setDisabled(false);
        this.shouldScrollToBottom = true;
      },
      error: (err: Error) => {
        this._messages.update(msgs => msgs.map(m =>
          m.id === loadMsg.id
            ? { ...m, content: err.message || 'An error occurred. Please try again.', loading: false }
            : m
        ));
        this._isLoading.set(false);
        this.inputComponent?.setDisabled(false);
        this.shouldScrollToBottom = true;
        this.snackBar.open('Connection error. Please check API availability.', 'Dismiss', { duration: 4000 });
      }
    });
  }

  clearConversation(): void {
    this._messages.set([]);
    this.conversationCleared.emit();
  }

  syncDocuments(): void {
    this.chatService.triggerSync().subscribe({
      next: () => this.snackBar.open('Document sync started.', 'OK', { duration: 3000 }),
      error: () => this.snackBar.open('Sync failed. Check SharePoint configuration.', 'Dismiss', { duration: 4000 })
    });
  }

  private scrollToBottom(): void {
    try {
      const el = this.scrollContainer?.nativeElement;
      if (el) el.scrollTop = el.scrollHeight;
    } catch { /* ignore */ }
  }
}
