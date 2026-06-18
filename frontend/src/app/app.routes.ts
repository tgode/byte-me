import { Routes } from '@angular/router';
import { ChatPageComponent } from './pages/chat/chat.component';
import { SettingsPageComponent } from './pages/settings/settings.component';

export const routes: Routes = [
  { path: '',         component: ChatPageComponent },
  { path: 'settings', component: SettingsPageComponent },
  { path: '**',       redirectTo: '' }
];
