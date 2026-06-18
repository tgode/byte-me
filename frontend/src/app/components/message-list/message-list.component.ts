import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { CitationPanelComponent } from '../citation-panel/citation-panel.component';
import { LoadingIndicatorComponent } from '../loading-indicator/loading-indicator.component';
import { ChatMessage } from '../../models/chat.model';

@Component({
  selector: 'app-message-list',
  standalone: true,
  imports: [CommonModule, MatIconModule, CitationPanelComponent, LoadingIndicatorComponent],
  template: `
    <div class="messages-container">

      <!-- ── Empty state ── -->
      @if (!messages || messages.length === 0) {
        <div class="empty-state">
          <div class="empty-logo">
            <mat-icon>support_agent</mat-icon>
          </div>
          <h2 class="empty-title">How can I help you?</h2>
          <p class="empty-sub">Ask me anything about HR policies, leave, benefits, or other workplace topics.</p>
          <div class="suggestion-row">
            @for (s of suggestions; track s.label) {
              <button class="suggestion-chip" (click)="onSuggestion(s.text)">
                <span class="chip-emoji">{{ s.emoji }}</span>
                <span>{{ s.label }}</span>
              </button>
            }
          </div>
        </div>
      }

      <!-- ── Messages ── -->
      @for (msg of messages; track msg.id) {
        <div class="msg-row" [class.user]="msg.role === 'user'" [class.bot]="msg.role === 'assistant'">

          <!-- Bot avatar (left) -->
          @if (msg.role === 'assistant') {
            <div class="avatar bot-avatar" aria-hidden="true">
              <mat-icon>support_agent</mat-icon>
            </div>
          }

          <div class="bubble-col" [class.user-col]="msg.role === 'user'">

            <!-- Role label -->
            <span class="role-label">{{ msg.role === 'user' ? 'You' : 'ByteHR AI' }}</span>

            <!-- Bubble -->
            <div class="bubble"
                 [class.user-bubble]="msg.role === 'user'"
                 [class.bot-bubble]="msg.role === 'assistant'">

              @if (msg.loading) {
                <app-loading-indicator />
              } @else {
                <div class="msg-text" [innerHTML]="format(msg.content)"></div>

                @if (msg.role === 'assistant' && msg.citations?.length) {
                  <app-citation-panel [citations]="msg.citations!" />
                }

                @if (msg.role === 'assistant' && msg.confidenceScore && msg.confidenceScore > 0) {
                  <div class="confidence"
                       [class.high]="msg.confidenceScore >= 0.8"
                       [class.low]="msg.confidenceScore < 0.6">
                    <mat-icon inline>verified</mat-icon>
                    {{ (msg.confidenceScore * 100).toFixed(0) }}% confidence
                  </div>
                }
              }
            </div>

            <span class="timestamp">{{ msg.timestamp | date:'HH:mm' }}</span>
          </div>

          <!-- User avatar (right) -->
          @if (msg.role === 'user') {
            <div class="avatar user-avatar" aria-hidden="true">
              <mat-icon>person</mat-icon>
            </div>
          }

        </div>
      }

    </div>
  `,
  styles: [`
    .messages-container {
      display: flex;
      flex-direction: column;
      gap: 4px;
      padding: 20px 16px 8px;
      flex: 1;
    }

    /* ── Empty state ── */
    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      flex: 1;
      padding: 48px 24px;
      text-align: center;
      min-height: 400px;
    }
    .empty-logo {
      width: 64px;
      height: 64px;
      border-radius: 50%;
      background: var(--c-primary);
      color: #fff;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: 20px;
      box-shadow: 0 4px 16px rgba(0,120,212,0.3);
      mat-icon { font-size: 32px; width: 32px; height: 32px; }
    }
    .empty-title {
      margin: 0 0 8px;
      font-size: 22px;
      font-weight: 600;
      color: var(--c-text);
    }
    .empty-sub {
      margin: 0 0 28px;
      font-size: 14px;
      color: var(--c-text-secondary);
      max-width: 380px;
      line-height: 1.5;
    }
    .suggestion-row {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      justify-content: center;
    }
    .suggestion-chip {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 8px 16px;
      background: var(--c-surface);
      border: 1px solid var(--c-border);
      border-radius: var(--radius-full);
      color: var(--c-primary);
      font-size: 13px;
      font-family: inherit;
      cursor: pointer;
      transition: background 0.15s, border-color 0.15s, box-shadow 0.15s;
    }
    .suggestion-chip:hover {
      background: var(--c-primary-light);
      border-color: var(--c-primary);
      box-shadow: var(--shadow-sm);
    }
    .chip-emoji { font-size: 16px; }

    /* ── Message row ── */
    .msg-row {
      display: flex;
      align-items: flex-end;
      gap: 10px;
      padding: 4px 0;
    }
    .user { flex-direction: row-reverse; }

    /* ── Avatars ── */
    .avatar {
      width: 34px;
      height: 34px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      mat-icon { font-size: 18px; width: 18px; height: 18px; }
    }
    .bot-avatar  { background: var(--c-primary); color: #fff; }
    .user-avatar { background: #E1DFDD; color: #323130; }

    /* ── Bubble column ── */
    .bubble-col {
      display: flex;
      flex-direction: column;
      gap: 3px;
      max-width: min(68%, 560px);
    }
    .user-col { align-items: flex-end; }

    .role-label {
      font-size: 11px;
      font-weight: 600;
      color: var(--c-text-muted);
      padding: 0 4px;
    }

    /* ── Bubble ── */
    .bubble {
      padding: 10px 14px;
      font-size: 14px;
      line-height: 1.55;
      word-break: break-word;
    }
    .user-bubble {
      background: var(--c-bubble-user);
      color: #fff;
      border-radius: var(--radius-lg) var(--radius-lg) var(--radius-sm) var(--radius-lg);
    }
    .bot-bubble {
      background: var(--c-bubble-bot);
      color: var(--c-text);
      border-radius: var(--radius-lg) var(--radius-lg) var(--radius-lg) var(--radius-sm);
      box-shadow: var(--shadow-sm);
      border: 1px solid var(--c-border-light);
    }

    .msg-text { white-space: pre-wrap; }

    /* ── Confidence badge ── */
    .confidence {
      display: inline-flex;
      align-items: center;
      gap: 3px;
      font-size: 11px;
      font-weight: 500;
      padding: 3px 8px;
      border-radius: var(--radius-full);
      margin-top: 6px;
      background: #EFF6FC;
      color: var(--c-primary);
    }
    .confidence.high { background: #DFF6DD; color: var(--c-success); }
    .confidence.low  { background: #FFF4CE; color: var(--c-warning); }

    /* ── Timestamp ── */
    .timestamp {
      font-size: 10px;
      color: var(--c-text-muted);
      padding: 0 4px;
    }
  `]
})
export class MessageListComponent {
  @Input() messages: ChatMessage[] = [];

  readonly suggestions = [
    { emoji: '🏖️', label: 'Vacation days', text: 'How many vacation days do I have?' },
    { emoji: '🏥', label: 'Sick leave',    text: 'What is the sick leave policy?' },
    { emoji: '🎁', label: 'Benefits',      text: 'What employee benefits are available?' },
    { emoji: '📋', label: 'Work hours',    text: 'What are the working hours policy?' }
  ];

  format(content: string): string {
    return content
      .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
      .replace(/\*(.*?)\*/g, '<em>$1</em>')
      .replace(/`(.*?)`/g, '<code style="background:var(--c-border-light);padding:1px 5px;border-radius:3px;font-size:13px">$1</code>');
  }

  onSuggestion(text: string): void {
    document.dispatchEvent(new CustomEvent('hr-suggestion', { detail: text }));
  }
}
