import { Component, signal } from '@angular/core';
import { SidebarComponent, ConversationPreview } from '../../components/sidebar/sidebar.component';
import { ChatWindowComponent } from '../../components/chat-window/chat-window.component';
import { uuidv4 } from '../../shared/uuid';

@Component({
  selector: 'app-chat-page',
  standalone: true,
  imports: [SidebarComponent, ChatWindowComponent],
  template: `
    <div class="app-shell">

      <app-sidebar
        [conversations]="conversations()"
        [activeId]="activeConversationId()"
        (newChat)="startNewConversation()"
        (conversationSelected)="selectConversation($event)"
      />

      <main class="main-area">
        <app-chat-window
          [conversationId]="activeConversationId()"
          (conversationStarted)="onConversationStarted($event)"
          (conversationCleared)="onConversationCleared()"
        />
      </main>

    </div>
  `,
  styles: [`
    .app-shell {
      display: flex;
      height: 100vh;
      overflow: hidden;
      background: var(--c-bg);
    }
    .main-area {
      flex: 1;
      min-width: 0;
      display: flex;
      flex-direction: column;
      overflow: hidden;
    }
  `]
})
export class ChatPageComponent {

  readonly activeConversationId = signal<string>(uuidv4());
  readonly conversations = signal<ConversationPreview[]>([]);

  startNewConversation(): void {
    const newId = uuidv4();
    this.activeConversationId.set(newId);
  }

  selectConversation(id: string): void {
    this.activeConversationId.set(id);
  }

  onConversationStarted(event: { id: string; title: string; preview: string }): void {
    this.conversations.update(list => {
      const existing = list.find(c => c.id === event.id);
      if (existing) {
        return list.map(c => c.id === event.id
          ? { ...c, title: event.title, preview: event.preview, timestamp: new Date() }
          : c
        );
      }
      return [
        { id: event.id, title: event.title, preview: event.preview, timestamp: new Date() },
        ...list
      ];
    });
  }

  onConversationCleared(): void {
    const newId = uuidv4();
    this.activeConversationId.set(newId);
  }
}

