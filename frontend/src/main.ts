import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';
import { app } from '@microsoft/teams-js';

// Bootstrap Angular immediately — never block on Teams SDK
bootstrapApplication(AppComponent, appConfig).catch(err => console.error(err));

// Init Teams SDK in the background
app.initialize().then(() => {
  app.notifyAppLoaded();
  app.notifySuccess();
}).catch(() => {});
