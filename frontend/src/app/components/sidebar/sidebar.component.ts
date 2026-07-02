import { Component, Input, Output, EventEmitter, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { appTheme } from '../../app.component';

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
    <aside class="sidebar" [class.mobile-open]="mobileOpen">

      <!-- Logo bar -->
      <div class="sidebar-logo">
        <div class="logo-mark">
          <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
            <rect width="20" height="20" rx="5" fill="currentColor" opacity="0.15"/>
            <path d="M4 14V7l4 4 4-4v7" stroke="currentColor" stroke-width="1.8"
                  stroke-linecap="round" stroke-linejoin="round"/>
            <circle cx="15.5" cy="5.5" r="2.5" fill="currentColor"/>
          </svg>
        </div>
        <span class="logo-text">ByteHR AI</span>
        <span class="logo-badge">BETA</span>
        <button class="mobile-close-btn" (click)="closed.emit()" aria-label="Close sidebar">
          <mat-icon>close</mat-icon>
        </button>
      </div>

      <!-- New conversation -->
      <div class="new-chat-wrap">
        <button class="new-chat-btn" (click)="newChat.emit()">
          <mat-icon>add</mat-icon>
          <span>New conversation</span>
        </button>
      </div>

      <!-- Section header -->
      <div class="section-header">
        <span>Recent</span>
      </div>

      <!-- Conversation list -->
      <nav class="conv-list" aria-label="Conversations">
        @if (conversations.length === 0) {
          <div class="conv-empty">
            <mat-icon>forum</mat-icon>
            <span>No conversations yet</span>
          </div>
        }
        @for (conv of conversations; track conv.id) {
          <div
            class="conv-item"
            [class.active]="conv.id === activeId"
            (click)="conversationSelected.emit(conv.id)"
            role="button"
            tabindex="0"
            (keydown.enter)="conversationSelected.emit(conv.id)"
            [attr.title]="conv.title">

            <div class="conv-icon">
              <mat-icon>{{ conv.id === activeId ? 'chat_bubble' : 'chat_bubble_outline' }}</mat-icon>
            </div>

            <div class="conv-meta">
              <div class="conv-title-row">
                <span class="conv-title">{{ conv.title }}</span>
                <span class="conv-time">{{ conv.timestamp | date:'HH:mm' }}</span>
              </div>
              <span class="conv-preview">{{ conv.preview }}</span>
            </div>

            @if (conv.unread) {
              <span class="unread-dot" aria-label="Unread"></span>
            }
          </div>
        }
      </nav>

      <div class="spacer"></div>
      <div class="sidebar-divider"></div>

      <!-- Footer -->
      <div class="sidebar-footer">

        <button class="footer-btn theme-toggle" (click)="toggleTheme()"
          [matTooltip]="isDark() ? 'Switch to light mode' : 'Switch to dark mode'">
          <mat-icon>{{ isDark() ? 'light_mode' : 'dark_mode' }}</mat-icon>
          <span>{{ isDark() ? 'Light mode' : 'Dark mode' }}</span>
        </button>

        <a routerLink="/settings" routerLinkActive="footer-btn--active"
           class="footer-btn" matTooltip="Settings">
          <mat-icon>settings</mat-icon>
          <span>Settings</span>
        </a>

        <div class="footer-user">
          <div class="user-avatar"><mat-icon>person</mat-icon></div>
          <div class="user-info">
            <span class="user-name">Employee</span>
            <span class="user-role">HR Portal</span>
          </div>
          <div class="user-status"></div>
        </div>

      </div>
    </aside>
  `,
  styles: [`
    :host { display: contents; }

    .sidebar {
      width: var(--sidebar-width);
      height: 100%;
      background: var(--c-sidebar-bg);
      border-right: 1px solid var(--c-sidebar-border);
      display: flex;
      flex-direction: column;
      overflow: hidden;
      flex-shrink: 0;
      transition: background var(--t-slow), border-color var(--t-slow);
    }

    .sidebar-logo {
      display: flex;
      align-items: center;
      gap: 9px;
      padding: 0 14px;
      height: var(--header-height);
      border-bottom: 1px solid var(--c-sidebar-border);
      flex-shrink: 0;
    }
    .logo-mark {
      width: 32px; height: 32px;
      border-radius: var(--radius-sm);
      background: var(--c-primary); color: #fff;
      display: flex; align-items: center; justify-content: center; flex-shrink: 0;
      svg { color: #fff; }
    }
    .logo-text { font-size: 15px; font-weight: 700; color: var(--c-text); letter-spacing: -0.3px; }
    .logo-badge {
      font-size: 9px; font-weight: 700; letter-spacing: 0.8px;
      color: var(--c-primary); background: var(--c-primary-light);
      padding: 2px 5px; border-radius: var(--radius-xs); margin-left: auto;
    }

    .mobile-close-btn {
      display: none;
      align-items: center; justify-content: center;
      width: 30px; height: 30px; padding: 0;
      border: none; background: transparent;
      color: var(--c-text-secondary); cursor: pointer;
      border-radius: var(--radius-md);
      transition: background var(--t-fast), color var(--t-fast);
      flex-shrink: 0;
      mat-icon { font-size: 18px; width: 18px; height: 18px; }
    }
    .mobile-close-btn:hover { background: var(--c-sidebar-hover); color: var(--c-text); }

    .new-chat-wrap { padding: 10px 10px 6px; flex-shrink: 0; }
    .new-chat-btn {
      width: 100%; display: flex; align-items: center; gap: 8px;
      padding: 8px 14px;
      border: 1.5px solid var(--c-primary); border-radius: var(--radius-full);
      background: transparent; color: var(--c-primary);
      font-size: 13px; font-weight: 600; cursor: pointer;
      transition: background var(--t-fast), box-shadow var(--t-fast);
      mat-icon { font-size: 18px; width: 18px; height: 18px; }
    }
    .new-chat-btn:hover { background: var(--c-primary-light); box-shadow: var(--shadow-xs); }
    .new-chat-btn:active { transform: scale(0.98); }

    .section-header {
      display: flex; align-items: center; padding: 10px 16px 4px;
      font-size: 11px; font-weight: 600; color: var(--c-text-muted);
      text-transform: uppercase; letter-spacing: 0.6px; flex-shrink: 0;
    }

    .conv-list { flex: 1; overflow-y: auto; padding: 2px 6px; }
    .conv-empty {
      display: flex; flex-direction: column; align-items: center;
      gap: 8px; padding: 32px 16px;
      color: var(--c-text-muted); font-size: 12px; text-align: center;
      mat-icon { font-size: 28px; width: 28px; height: 28px; opacity: 0.4; }
    }
    .conv-item {
      display: flex; align-items: center; gap: 10px;
      padding: 8px 10px; border-radius: var(--radius-md);
      cursor: pointer; outline: none; position: relative;
      transition: background var(--t-fast);
    }
    .conv-item:hover { background: var(--c-sidebar-hover); }
    .conv-item:focus-visible { outline: 2px solid var(--c-primary); outline-offset: -2px; }
    .conv-item.active { background: var(--c-sidebar-active); }
    .conv-item.active::before {
      content: ''; position: absolute; left: 0; top: 8px; bottom: 8px;
      width: 3px; border-radius: 0 3px 3px 0; background: var(--c-primary);
    }
    .conv-icon {
      width: 32px; height: 32px; border-radius: var(--radius-full);
      background: var(--c-sidebar-hover); color: var(--c-text-secondary);
      display: flex; align-items: center; justify-content: center; flex-shrink: 0;
      transition: background var(--t-fast), color var(--t-fast);
      mat-icon { font-size: 16px; width: 16px; height: 16px; }
    }
    .conv-item.active .conv-icon { background: var(--c-primary); color: #fff; }
    .conv-meta { flex: 1; min-width: 0; }
    .conv-title-row { display: flex; align-items: center; gap: 4px; }
    .conv-title {
      flex: 1; font-size: 13px; font-weight: 500; color: var(--c-text);
      white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
    }
    .conv-time { font-size: 10px; color: var(--c-text-muted); white-space: nowrap; flex-shrink: 0; }
    .conv-preview {
      display: block; font-size: 11.5px; color: var(--c-text-secondary);
      white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin-top: 1px;
    }
    .unread-dot { width: 7px; height: 7px; border-radius: 50%; background: var(--c-primary); flex-shrink: 0; }

    .spacer { flex: 1; min-height: 8px; }
    .sidebar-divider { height: 1px; background: var(--c-sidebar-border); margin: 0 10px; flex-shrink: 0; }
    .sidebar-footer { padding: 6px 6px 10px; display: flex; flex-direction: column; gap: 1px; flex-shrink: 0; }
    .footer-btn {
      display: flex; align-items: center; gap: 10px;
      padding: 8px 10px; border-radius: var(--radius-md);
      color: var(--c-text-secondary); font-size: 13px; cursor: pointer;
      transition: background var(--t-fast), color var(--t-fast);
      text-decoration: none; border: none; background: transparent;
      font-family: inherit; width: 100%; text-align: left;
      mat-icon { font-size: 18px; width: 18px; height: 18px; flex-shrink: 0; }
    }
    .footer-btn:hover { background: var(--c-sidebar-hover); color: var(--c-text); }
    .footer-btn--active { color: var(--c-primary) !important; background: var(--c-primary-light) !important; }
    .footer-user {
      display: flex; align-items: center; gap: 10px;
      padding: 8px 10px; border-radius: var(--radius-md); margin-top: 2px;
    }
    .user-avatar {
      width: 30px; height: 30px; border-radius: 50%;
      background: var(--c-primary); color: #fff;
      display: flex; align-items: center; justify-content: center; flex-shrink: 0;
      mat-icon { font-size: 18px; width: 18px; height: 18px; }
    }
    .user-info { flex: 1; min-width: 0; display: flex; flex-direction: column; }
    .user-name {
      font-size: 13px; font-weight: 600; color: var(--c-text);
      white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
    }
    .user-role { font-size: 11px; color: var(--c-text-muted); }
    .user-status {
      width: 8px; height: 8px; border-radius: 50%;
      background: var(--c-online); flex-shrink: 0;
      box-shadow: 0 0 0 2px var(--c-sidebar-bg);
    }

    /* Mobile: sidebar becomes a slide-in drawer */
    @media (max-width: 768px) {
      .sidebar {
        position: fixed;
        top: 0; left: 0;
        height: 100%; z-index: 200;
        transform: translateX(-100%);
        transition: transform 0.25s ease, background var(--t-slow), border-color var(--t-slow);
        box-shadow: 4px 0 24px rgba(0, 0, 0, 0.22);
      }
      .sidebar.mobile-open { transform: translateX(0); }
      .logo-badge { margin-left: 0; }
      .mobile-close-btn { display: flex; }
    }
  `]
})
export class SidebarComponent {
  @Input() conversations: ConversationPreview[] = [];
  @Input() activeId = '';
  @Input() mobileOpen = false;
  @Output() newChat = new EventEmitter<void>();
  @Output() conversationSelected = new EventEmitter<string>();
  @Output() closed = new EventEmitter<void>();

  readonly isDark = computed(() => appTheme() === 'dark');

  toggleTheme(): void {
    appTheme.set(appTheme() === 'dark' ? 'light' : 'dark');
  }
}
