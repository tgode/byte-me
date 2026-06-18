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
      @if (!messages || messages.length === 0) {
        <div class="empty-state">
          <mat-icon class="empty-icon">support_agent</mat-icon>
          <h3>Welcome to ByteHR AI</h3>
          <p>Ask me anything about HR policies, leave, benefits, or other HR topics.</p>
          <div class="suggestion-chips">
            <span class="chip" (click)="onSuggestion('How many vacation days do I have?')">
              🏖️ Vacation days
            </span>
            <span class="chip" (click)="onSuggestion('What is the sick leave policy?')">
              🏥 Sick leave
            </span>
            <span class="chip" (click)="onSuggestion('What employee benefits are available?')">
              🎁 Benefits
            </span>
          </div>
        </div>
      }
      @for (msg of messages; track msg.id) {
        <div class="message-row" [class.user-row]="msg.role === 'user'" [class.bot-row]="msg.role === 'assistant'">
          @if (msg.role === 'assistant') {
            <div class="avatar bot-avatar">
              <mat-icon>support_agent</mat-icon>
            </div>
          }
          <div class="bubble-wrapper">
            <div class="bubble"
                 [class.user-bubble]="msg.role === 'user'"
                 [class.bot-bubble]="msg.role === 'assistant'">
              @if (msg.loading) {
                <app-loading-indicator />
              } @else {
                <div class="message-content" [innerHTML]="formatContent(msg.content)"></div>
                @if (msg.role === 'assistant' && msg.citations && msg.citations.length > 0) {
                  <app-citation-panel [citations]="msg.citations" />
                }
                @if (msg.role === 'assistant' && msg.confidenceScore !== undefined && msg.confidenceScore > 0) {
                  <div class="confidence-badge" [class.high]="msg.confidenceScore >= 0.8" [class.low]="msg.confidenceScore < 0.6">
                    <mat-icon inline>verified</mat-icon>
                    {{ (msg.confidenceScore * 100).toFixed(0) }}% confidence
                  </div>
                }
              }
            </div>
            <span class="timestamp">{{ msg.timestamp | date:'HH:mm' }}</span>
          </div>
          @if (msg.role === 'user') {
            <div class="avatar user-avatar">
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
      gap: 16px;
      padding: 16px;
      flex: 1;
      overflow-y: auto;
    }
    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      flex: 1;
      text-align: center;
      color: #605E5C;
      padding: 40px 20px;
    }
    .empty-icon {
      font-size: 64px;
      width: 64px;
      height: 64px;
      color: #0078D4;
      margin-bottom: 16px;
    }
    .empty-state h3 {
      margin: 0 0 8px;
      color: #201F1E;
      font-size: 20px;
      font-weight: 600;
    }
    .empty-state p { margin: 0 0 20px; font-size: 14px; }
    .suggestion-chips {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
      justify-content: center;
    }
    .chip {
      background: #F0F6FF;
      border: 1px solid #C7E0F4;
      color: #0078D4;
      padding: 6px 14px;
      border-radius: 16px;
      cursor: pointer;
      font-size: 13px;
      transition: background 0.2s;
    }
    .chip:hover { background: #DEF0FF; }
    .message-row {
      display: flex;
      align-items: flex-start;
      gap: 10px;
    }
    .user-row { flex-direction: row-reverse; }
    .avatar {
      width: 36px;
      height: 36px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }
    .bot-avatar  { background: #0078D4; color: #fff; }
    .user-avatar { background: #E1DFDD; color: #323130; }
    .bubble-wrapper {
      display: flex;
      flex-direction: column;
      max-width: 70%;
    }
    .user-row .bubble-wrapper { align-items: flex-end; }
    .bubble {
      padding: 10px 14px;
      border-radius: 18px;
      font-size: 14px;
      line-height: 1.5;
      word-break: break-word;
    }
    .user-bubble {
      background: #0078D4;
      color: #fff;
      border-top-right-radius: 4px;
    }
    .bot-bubble {
      background: #fff;
      color: #201F1E;
      border-top-left-radius: 4px;
      box-shadow: 0 1px 4px rgba(0,0,0,0.1);
    }
    .message-content { white-space: pre-wrap; }
    .timestamp {
      font-size: 11px;
      color: #A19F9D;
      padding: 2px 4px;
    }
    .confidence-badge {
      display: inline-flex;
      align-items: center;
      gap: 3px;
      font-size: 11px;
      padding: 2px 8px;
      border-radius: 10px;
      margin-top: 6px;
      background: #EFF6FC;
      color: #0078D4;
    }
    .confidence-badge.high { background: #DFF6DD; color: #107C10; }
    .confidence-badge.low  { background: #FFF4CE; color: #797775; }
  `]
})
export class MessageListComponent {
  @Input() messages: ChatMessage[] = [];

  formatContent(content: string): string {
    return content
      .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
      .replace(/\*(.*?)\*/g, '<em>$1</em>')
      .replace(/`(.*?)`/g, '<code style="background:#F3F2F1;padding:1px 4px;border-radius:3px">$1</code>');
  }

  onSuggestion(text: string): void {
    // Emitting suggestion via DOM event so parent can intercept
    document.dispatchEvent(new CustomEvent('hr-suggestion', { detail: text }));
  }
}
