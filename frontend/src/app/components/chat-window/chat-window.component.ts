import {
  Component, OnInit, OnDestroy, signal, computed,
  ViewChild, ElementRef, AfterViewChecked, inject
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MessageListComponent } from '../message-list/message-list.component';
import { MessageInputComponent } from '../message-input/message-input.component';
import { ChatService } from '../../services/chat.service';
import { ChatMessage } from '../../models/chat.model';
import { v4 as uuidv4 } from '../../shared/uuid';

@Component({
  selector: 'app-chat-window',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatToolbarModule, MatIconModule, MatButtonModule, MatTooltipModule, MatSnackBarModule,
    MessageListComponent, MessageInputComponent
  ],
  template: `
    <div class="chat-window">
      <!-- Header -->
      <mat-toolbar class="chat-header" color="primary">
        <mat-icon class="header-icon">support_agent</mat-icon>
        <div class="header-text">
          <span class="header-title">ByteHR AI</span>
          <span class="header-subtitle">HR Assistant · {{ statusLabel() }}</span>
        </div>
        <span class="spacer"></span>
        <button mat-icon-button
                matTooltip="Clear conversation"
                (click)="clearConversation()"
                aria-label="Clear conversation">
          <mat-icon>delete_outline</mat-icon>
        </button>
        <button mat-icon-button
                matTooltip="Sync HR documents"
                (click)="syncDocuments()"
                aria-label="Sync documents">
          <mat-icon>sync</mat-icon>
        </button>
      </mat-toolbar>

      <!-- Messages -->
      <div class="messages-scroll" #scrollContainer>
        <app-message-list [messages]="messages()" />
      </div>

      <!-- Input -->
      <app-message-input
        #inputComponent
        (messageSent)="onMessageSent($event)"
      />
    </div>
  `,
  styles: [`
    .chat-window {
      display: flex;
      flex-direction: column;
      height: 100%;
      background: #F5F5F5;
    }
    .chat-header {
      flex-shrink: 0;
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 0 8px;
      height: 56px;
      min-height: 56px;
    }
    .header-icon { font-size: 28px; width: 28px; height: 28px; }
    .header-text {
      display: flex;
      flex-direction: column;
    }
    .header-title  { font-size: 16px; font-weight: 600; line-height: 1.2; }
    .header-subtitle { font-size: 11px; opacity: 0.8; line-height: 1.2; }
    .spacer { flex: 1; }
    .messages-scroll {
      flex: 1;
      overflow-y: auto;
      display: flex;
      flex-direction: column;
    }
  `]
})
export class ChatWindowComponent implements OnInit, OnDestroy, AfterViewChecked {
  @ViewChild('scrollContainer') scrollContainer!: ElementRef<HTMLElement>;
  @ViewChild('inputComponent') inputComponent!: MessageInputComponent;

  private readonly chatService = inject(ChatService);
  private readonly snackBar = inject(MatSnackBar);

  private readonly _messages = signal<ChatMessage[]>([]);
  readonly messages = this._messages.asReadonly();

  private readonly _isLoading = signal(false);
  readonly statusLabel = computed(() =>
    this._isLoading() ? 'Thinking…' : 'Online'
  );

  conversationId = uuidv4();
  private shouldScrollToBottom = false;
  private suggestionListener!: EventListener;

  ngOnInit(): void {
    this.suggestionListener = (e: Event) => {
      const text = (e as CustomEvent).detail as string;
      if (text) this.onMessageSent(text);
    };
    document.addEventListener('hr-suggestion', this.suggestionListener);
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

    const userMsg: ChatMessage = {
      id: uuidv4(),
      role: 'user',
      content: text,
      timestamp: new Date()
    };

    const loadingMsg: ChatMessage = {
      id: uuidv4(),
      role: 'assistant',
      content: '',
      timestamp: new Date(),
      loading: true
    };

    this._messages.update(msgs => [...msgs, userMsg, loadingMsg]);
    this._isLoading.set(true);
    this.inputComponent.setDisabled(true);
    this.shouldScrollToBottom = true;

    this.chatService.sendMessage({
      message: text,
      conversationId: this.conversationId
    }).subscribe({
      next: (response) => {
        this._messages.update(msgs =>
          msgs.map(m => m.id === loadingMsg.id
            ? {
                ...m,
                content: response.answer,
                citations: response.citations,
                confidenceScore: response.confidenceScore,
                loading: false
              }
            : m
          )
        );
        this._isLoading.set(false);
        this.inputComponent.setDisabled(false);
        this.shouldScrollToBottom = true;
      },
      error: (err: Error) => {
        this._messages.update(msgs =>
          msgs.map(m => m.id === loadingMsg.id
            ? { ...m, content: err.message || 'An error occurred. Please try again.', loading: false }
            : m
          )
        );
        this._isLoading.set(false);
        this.inputComponent.setDisabled(false);
        this.shouldScrollToBottom = true;
        this.snackBar.open('Connection error. Please check API availability.', 'Dismiss', { duration: 4000 });
      }
    });
  }

  clearConversation(): void {
    this._messages.set([]);
    this.conversationId = uuidv4();
  }

  syncDocuments(): void {
    this.chatService.triggerSync().subscribe({
      next: () => this.snackBar.open('Document sync started successfully.', 'OK', { duration: 3000 }),
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
