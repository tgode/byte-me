import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { CitationPanelComponent } from '../citation-panel/citation-panel.component';
import { ChatMessage } from '../../models/chat.model';

@Component({
  selector: 'app-message-list',
  standalone: true,
  imports: [CommonModule, MatIconModule, CitationPanelComponent],
  template: `
    <div class="msg-list">

      <!-- ─── Empty state ─── -->
      @if (!messages || messages.length === 0) {
        <div class="empty-state">

          <div class="empty-hero">
            <div class="hero-ring hero-ring--2"></div>
            <div class="hero-ring hero-ring--1"></div>
            <div class="hero-icon">
              <mat-icon>support_agent</mat-icon>
            </div>
          </div>

          <h2 class="empty-title">How can I help you today?</h2>
          <p class="empty-sub">
            Ask me anything about HR policies, leave, benefits,<br>or other workplace topics.
          </p>

          <div class="chips-grid">
            @for (s of suggestions; track s.label) {
              <button class="chip" (click)="onSuggestion(s.text)">
                <span class="chip-icon">{{ s.emoji }}</span>
                <span class="chip-label">{{ s.label }}</span>
                <mat-icon class="chip-arrow">arrow_forward</mat-icon>
              </button>
            }
          </div>

        </div>
      }

      <!-- ─── Messages ─── -->
      @for (msg of messages; track msg.id) {
        <div class="msg-wrap" [class.user-wrap]="msg.role === 'user'">

          <!-- Bot avatar -->
          @if (msg.role === 'assistant') {
            <div class="avatar bot-av">
              <mat-icon>support_agent</mat-icon>
            </div>
          }

          <div class="bubble-col" [class.user-col]="msg.role === 'user'">

            <span class="role-label">{{ msg.role === 'user' ? 'You' : 'ByteHR AI' }}</span>

            <div class="bubble"
                 [class.user-bubble]="msg.role === 'user'"
                 [class.bot-bubble]="msg.role === 'assistant'">

              @if (msg.loading) {
                <div class="typing-dots">
                  <span></span><span></span><span></span>
                </div>
              } @else {
                <div class="msg-body" [innerHTML]="format(msg.content)"></div>

                @if (msg.role === 'assistant' && msg.citations?.length) {
                  <div class="citation-wrap">
                    <app-citation-panel [citations]="msg.citations!" />
                  </div>
                }

                @if (msg.role === 'assistant' && msg.confidenceScore && msg.confidenceScore > 0) {
                  <div class="conf-badge"
                       [class.conf-high]="msg.confidenceScore >= 0.75"
                       [class.conf-mid]="msg.confidenceScore >= 0.5 && msg.confidenceScore < 0.75"
                       [class.conf-low]="msg.confidenceScore < 0.5">
                    <mat-icon inline>shield</mat-icon>
                    {{ (msg.confidenceScore * 100).toFixed(0) }}% match
                  </div>
                }
              }
            </div>

            <time class="msg-time">{{ msg.timestamp | date:'HH:mm' }}</time>
          </div>

          <!-- User avatar -->
          @if (msg.role === 'user') {
            <div class="avatar user-av">
              <mat-icon>person</mat-icon>
            </div>
          }

        </div>
      }

      <!-- Bottom padding -->
      <div style="height: 16px"></div>

    </div>
  `,
  styles: [`
    .msg-list {
      display: flex;
      flex-direction: column;
      padding: 16px 20px 0;
      flex: 1;
    }

    /* ─── Empty state ─── */
    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 56px 24px 40px;
      flex: 1;
      text-align: center;
      min-height: 380px;
    }

    .empty-hero {
      position: relative;
      width: 80px;
      height: 80px;
      margin-bottom: 28px;
    }
    .hero-ring {
      position: absolute;
      inset: 0;
      border-radius: 50%;
      border: 2px solid var(--c-primary);
      opacity: 0.15;
      animation: ring-expand 2.4s ease-out infinite;
    }
    .hero-ring--1 { animation-delay: 0s; }
    .hero-ring--2 { animation-delay: 1.2s; }
    @keyframes ring-expand {
      0%   { transform: scale(1);   opacity: 0.25; }
      100% { transform: scale(1.8); opacity: 0;    }
    }
    .hero-icon {
      position: absolute;
      inset: 10px;
      border-radius: 50%;
      background: var(--c-primary);
      color: #fff;
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: var(--shadow-md);
      mat-icon { font-size: 26px; width: 26px; height: 26px; }
    }

    .empty-title {
      margin: 0 0 10px;
      font-size: 20px;
      font-weight: 700;
      color: var(--c-text);
      letter-spacing: -0.3px;
    }
    .empty-sub {
      margin: 0 0 32px;
      font-size: 13.5px;
      color: var(--c-text-secondary);
      line-height: 1.6;
    }
    .chips-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 8px;
      width: 100%;
      max-width: 440px;
    }
    .chip {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 10px 14px;
      background: var(--c-surface);
      border: 1px solid var(--c-border);
      border-radius: var(--radius-lg);
      color: var(--c-text);
      font-size: 13px;
      font-family: inherit;
      cursor: pointer;
      text-align: left;
      transition: border-color var(--t-fast), box-shadow var(--t-fast), background var(--t-fast);
    }
    .chip:hover {
      border-color: var(--c-primary);
      background: var(--c-primary-light);
      box-shadow: var(--shadow-xs);
    }
    .chip-icon   { font-size: 18px; flex-shrink: 0; }
    .chip-label  { flex: 1; font-weight: 500; }
    .chip-arrow  {
      font-size: 14px !important;
      width: 14px !important;
      height: 14px !important;
      color: var(--c-text-muted);
      opacity: 0;
      transition: opacity var(--t-fast), transform var(--t-fast);
      flex-shrink: 0;
    }
    .chip:hover .chip-arrow { opacity: 1; transform: translateX(2px); }

    /* ─── Message row ─── */
    .msg-wrap {
      display: flex;
      align-items: flex-end;
      gap: 8px;
      margin-bottom: 12px;
    }
    .user-wrap { flex-direction: row-reverse; }

    /* ─── Avatars ─── */
    .avatar {
      width: 32px;
      height: 32px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      mat-icon { font-size: 17px; width: 17px; height: 17px; }
    }
    .bot-av  {
      background: var(--c-primary);
      color: #fff;
      box-shadow: var(--shadow-xs);
    }
    .user-av {
      background: var(--c-surface-raised);
      color: var(--c-text-secondary);
      border: 1px solid var(--c-border);
    }

    /* ─── Bubble column ─── */
    .bubble-col {
      display: flex;
      flex-direction: column;
      gap: 3px;
      max-width: min(66%, 580px);
    }
    .user-col { align-items: flex-end; }

    .role-label {
      font-size: 11px;
      font-weight: 600;
      color: var(--c-text-muted);
      padding: 0 6px;
      letter-spacing: 0.1px;
    }

    /* ─── Bubbles ─── */
    .bubble {
      padding: 10px 14px;
      font-size: 14px;
      line-height: 1.6;
      word-break: break-word;
      transition: background var(--t-slow);
    }
    .user-bubble {
      background: var(--c-bubble-user-bg);
      color: var(--c-bubble-user-text);
      border-radius: var(--radius-xl) var(--radius-xl) var(--radius-sm) var(--radius-xl);
    }
    .bot-bubble {
      background: var(--c-bubble-bot-bg);
      color: var(--c-bubble-bot-text);
      border-radius: var(--radius-xl) var(--radius-xl) var(--radius-xl) var(--radius-sm);
      box-shadow: var(--shadow-xs);
      border: 1px solid var(--c-bubble-bot-border);
    }

    /* ─── Typing dots ─── */
    .typing-dots {
      display: flex;
      align-items: center;
      gap: 4px;
      padding: 4px 2px;
      min-width: 40px;
    }
    .typing-dots span {
      width: 7px;
      height: 7px;
      border-radius: 50%;
      background: var(--c-text-muted);
      animation: bounce 1.2s ease-in-out infinite;
      display: block;
    }
    .typing-dots span:nth-child(1) { animation-delay: 0s; }
    .typing-dots span:nth-child(2) { animation-delay: 0.2s; }
    .typing-dots span:nth-child(3) { animation-delay: 0.4s; }
    @keyframes bounce {
      0%, 60%, 100% { transform: translateY(0);    opacity: 0.4; }
      30%            { transform: translateY(-5px); opacity: 1;   }
    }

    /* ─── Msg body markdown ─── */
    .msg-body { white-space: pre-wrap; }
    .msg-body ::ng-deep strong { font-weight: 600; }
    .msg-body ::ng-deep em     { font-style: italic; }
    .msg-body ::ng-deep code {
      background: rgba(0,0,0,0.08);
      padding: 1px 5px;
      border-radius: 4px;
      font-size: 12.5px;
      font-family: 'Cascadia Code', 'Consolas', monospace;
    }
    .user-bubble .msg-body ::ng-deep code {
      background: rgba(255,255,255,0.2);
    }

    /* ─── Citation wrapper ─── */
    .citation-wrap {
      margin-top: 10px;
      padding-top: 10px;
      border-top: 1px solid var(--c-border-light);
    }

    /* ─── Confidence badge ─── */
    .conf-badge {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      font-size: 11px;
      font-weight: 600;
      padding: 3px 10px;
      border-radius: var(--radius-full);
      margin-top: 8px;
    }
    .conf-high { background: #DFF6DD; color: #107C10; }
    .conf-mid  { background: #FFF4CE; color: #7A5400; }
    .conf-low  { background: #FDE7E9; color: #C50F1F; }

    [data-theme="dark"] .conf-high { background: #143314; color: #92C353; }
    [data-theme="dark"] .conf-mid  { background: #3A2B00; color: #F8D22A; }
    [data-theme="dark"] .conf-low  { background: #3C1414; color: #F47574; }

    /* ─── Timestamp ─── */
    .msg-time {
      font-size: 10px;
      color: var(--c-text-muted);
      padding: 0 6px;
    }
  `]
})
export class MessageListComponent {
  @Input() messages: ChatMessage[] = [];

  readonly suggestions = [
    { emoji: '🏖️', label: 'Vacation days',  text: 'How many vacation days do I have?' },
    { emoji: '🏥', label: 'Sick leave',      text: 'What is the sick leave policy?' },
    { emoji: '🎁', label: 'Benefits',        text: 'What employee benefits are available?' },
    { emoji: '📋', label: 'Working hours',   text: 'What are the working hours policy?' }
  ];

  format(content: string): string {
    // Strip any raw HTML tags the AI might include to prevent unintended links/XSS
    const stripped = content.replace(/<[^>]*>/g, '');
    return stripped
      .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
      .replace(/\*(.*?)\*/g, '<em>$1</em>')
      .replace(/`(.*?)`/g, '<code>$1</code>');
  }

  onSuggestion(text: string): void {
    document.dispatchEvent(new CustomEvent('hr-suggestion', { detail: text }));
  }
}
