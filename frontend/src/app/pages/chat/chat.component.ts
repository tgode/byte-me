import { Component } from '@angular/core';
import { ChatWindowComponent } from '../../components/chat-window/chat-window.component';

@Component({
  selector: 'app-chat-page',
  standalone: true,
  imports: [ChatWindowComponent],
  template: `
    <div class="chat-page">
      <app-chat-window />
    </div>
  `,
  styles: [`
    .chat-page {
      height: 100vh;
      display: flex;
      flex-direction: column;
      overflow: hidden;
    }
  `]
})
export class ChatPageComponent {}
