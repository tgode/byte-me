import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink, RouterLinkActive } from '@angular/router';

export interface ConversationPreview {
  id: string;
  title: string;
  preview: string;
  timestamp: Date;
  unread?: boolean;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatTooltipModule, RouterLink, RouterLinkActive],
  template: `
    <aside class="sidebar">

      <!-- Logo bar -->
      <div class="sidebar-logo">
        <div class="logo-icon">
          <mat-icon>support_agent</mat-icon>
        </div>
        <span class="logo-text">ByteHR AI</span>
      </div>

      <!-- New conversation button -->
      <div class="sidebar-actions">
        <button class="new-chat-btn" (click)="newChat.emit()" matTooltip="Start new conversation">
          <mat-icon>edit_square</mat-icon>
          <span>New conversation</span>
        </button>
      </div>

      <!-- Section label -->
      <div class="section-label">Recent</div>

      <!-- Conversation list -->
      <nav class="conv-list" aria-label="Conversations">
        @if (conversations.length === 0) {
          <div class="conv-empty">No conversations yet</div>
        }
        @for (conv of conversations; track conv.id) {
          <div
            class="conv-item"
            [class.active]="conv.id === activeId"
            (click)="conversationSelected.emit(conv.id)"
            [attr.aria-current]="conv.id === activeId ? 'page' : null"
            role="button"
            tabindex="0"
            (keydown.enter)="conversationSelected.emit(conv.id)">

            <div class="conv-avatar">
              <mat-icon>chat_bubble_outline</mat-icon>
            </div>

            <div class="conv-meta">
              <div class="conv-title-row">
                <span class="conv-title">{{ conv.title }}</span>
                <span class="conv-time">{{ conv.timestamp | date:'HH:mm' }}</span>
              </div>
              <span class="conv-preview">{{ conv.preview }}</span>
            </div>

            @if (conv.unread) {
              <span class="unread-dot"></span>
            }
          </div>
        }
      </nav>

      <!-- Spacer -->
      <div class="sidebar-spacer"></div>

      <!-- Bottom nav -->
      <div class="sidebar-footer">
        <a routerLink="/settings" routerLinkActive="active-nav"
           class="footer-nav-item" matTooltip="Settings">
          <mat-icon>settings</mat-icon>
          <span>Settings</span>
        </a>
        <div class="footer-nav-item user-item">
          <div class="user-avatar">
            <mat-icon>person</mat-icon>
          </div>
          <span class="user-name">Employee</span>
        </div>
      </div>

    </aside>
  `,
  styles: [`
    .sidebar {
      width: var(--sidebar-width);
      height: 100%;
      background: var(--c-sidebar-bg);
      border-right: 1px solid var(--c-border);
      display: flex;
      flex-direction: column;
      overflow: hidden;
      flex-shrink: 0;
    }

    /* ── Logo ── */
    .sidebar-logo {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 0 16px;
      height: var(--header-height);
      border-bottom: 1px solid var(--c-border);
      flex-shrink: 0;
    }
    .logo-icon {
      width: 32px;
      height: 32px;
      border-radius: var(--radius-sm);
      background: var(--c-primary);
      color: #fff;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      mat-icon { font-size: 18px; width: 18px; height: 18px; }
    }
    .logo-text {
      font-size: 15px;
      font-weight: 600;
      color: var(--c-text);
      letter-spacing: -0.2px;
    }

    /* ── New chat button ── */
    .sidebar-actions {
      padding: 12px 12px 4px;
      flex-shrink: 0;
    }
    .new-chat-btn {
      width: 100%;
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 8px 12px;
      border: 1.5px solid var(--c-primary);
      border-radius: var(--radius-full);
      background: transparent;
      color: var(--c-primary);
      font-size: 13px;
      font-weight: 500;
      cursor: pointer;
      transition: background 0.15s, color 0.15s;
      font-family: inherit;
      mat-icon { font-size: 18px; width: 18px; height: 18px; }
    }
    .new-chat-btn:hover {
      background: var(--c-primary-light);
    }

    /* ── Section label ── */
    .section-label {
      padding: 12px 16px 4px;
      font-size: 11px;
      font-weight: 600;
      color: var(--c-text-muted);
      text-transform: uppercase;
      letter-spacing: 0.5px;
      flex-shrink: 0;
    }

    /* ── Conversation list ── */
    .conv-list {
      flex: 1;
      overflow-y: auto;
      padding: 2px 8px;
    }
    .conv-empty {
      padding: 16px;
      font-size: 12px;
      color: var(--c-text-muted);
      text-align: center;
    }
    .conv-item {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 8px 10px;
      border-radius: var(--radius-md);
      cursor: pointer;
      position: relative;
      transition: background 0.12s;
      outline: none;
    }
    .conv-item:hover  { background: var(--c-sidebar-hover); }
    .conv-item.active {
      background: var(--c-sidebar-active);
      border-left: 3px solid var(--c-primary);
      padding-left: 7px;
    }
    .conv-avatar {
      width: 34px;
      height: 34px;
      border-radius: var(--radius-full);
      background: #E1DFDD;
      color: var(--c-text-secondary);
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      mat-icon { font-size: 18px; width: 18px; height: 18px; }
    }
    .conv-item.active .conv-avatar {
      background: var(--c-primary);
      color: #fff;
    }
    .conv-meta {
      flex: 1;
      min-width: 0;
      display: flex;
      flex-direction: column;
      gap: 2px;
    }
    .conv-title-row {
      display: flex;
      align-items: center;
      gap: 4px;
    }
    .conv-title {
      flex: 1;
      font-size: 13px;
      font-weight: 500;
      color: var(--c-text);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .conv-time {
      font-size: 11px;
      color: var(--c-text-muted);
      white-space: nowrap;
      flex-shrink: 0;
    }
    .conv-preview {
      font-size: 12px;
      color: var(--c-text-secondary);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .unread-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: var(--c-primary);
      flex-shrink: 0;
    }

    /* ── Footer ── */
    .sidebar-spacer { flex: 1; }
    .sidebar-footer {
      border-top: 1px solid var(--c-border);
      padding: 8px 8px;
      display: flex;
      flex-direction: column;
      gap: 2px;
      flex-shrink: 0;
    }
    .footer-nav-item {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 8px 10px;
      border-radius: var(--radius-md);
      color: var(--c-text-secondary);
      font-size: 13px;
      cursor: pointer;
      transition: background 0.12s;
      text-decoration: none;
    }
    .footer-nav-item:hover { background: var(--c-sidebar-hover); color: var(--c-text); }
    .footer-nav-item.active-nav { color: var(--c-primary); background: var(--c-primary-light); }
    .footer-nav-item mat-icon { font-size: 20px; width: 20px; height: 20px; }
    .user-item { cursor: default; }
    .user-item:hover { background: transparent; }
    .user-avatar {
      width: 30px;
      height: 30px;
      border-radius: 50%;
      background: var(--c-primary);
      color: #fff;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      mat-icon { font-size: 18px; width: 18px; height: 18px; }
    }
    .user-name {
      font-size: 13px;
      font-weight: 500;
      color: var(--c-text);
    }
  `]
})
export class SidebarComponent {
  @Input() conversations: ConversationPreview[] = [];
  @Input() activeId = '';
  @Output() newChat = new EventEmitter<void>();
  @Output() conversationSelected = new EventEmitter<string>();
}
