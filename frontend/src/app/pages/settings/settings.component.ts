import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatDividerModule } from '@angular/material/divider';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { CitationService } from '../../services/citation.service';
import { AnalyticsSummary } from '../../models/chat.model';

@Component({
  selector: 'app-settings-page',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterLink,
    MatCardModule, MatFormFieldModule, MatInputModule, MatButtonModule,
    MatIconModule, MatToolbarModule, MatSelectModule, MatSlideToggleModule,
    MatDividerModule, MatSnackBarModule
  ],
  template: `
    <div class="settings-page">
      <mat-toolbar color="primary">
        <button mat-icon-button routerLink="/" aria-label="Back to chat">
          <mat-icon>arrow_back</mat-icon>
        </button>
        <mat-icon>settings</mat-icon>
        <span style="margin-left:8px">Settings & Analytics</span>
      </mat-toolbar>

      <div class="settings-content">

        <!-- Analytics Card -->
        <mat-card class="settings-card">
          <mat-card-header>
            <mat-icon mat-card-avatar>analytics</mat-icon>
            <mat-card-title>Usage Analytics</mat-card-title>
            <mat-card-subtitle>System performance metrics</mat-card-subtitle>
          </mat-card-header>
          <mat-card-content>
            @if (analytics()) {
              <div class="stats-grid">
                <div class="stat-item">
                  <span class="stat-value">{{ analytics()!.totalQuestions }}</span>
                  <span class="stat-label">Total Questions</span>
                </div>
                <div class="stat-item">
                  <span class="stat-value success">{{ analytics()!.answeredQuestions }}</span>
                  <span class="stat-label">Answered</span>
                </div>
                <div class="stat-item">
                  <span class="stat-value warn">{{ analytics()!.unansweredQuestions }}</span>
                  <span class="stat-label">Unanswered</span>
                </div>
                <div class="stat-item">
                  <span class="stat-value">{{ analytics()!.answerRate.toFixed(1) }}%</span>
                  <span class="stat-label">Answer Rate</span>
                </div>
                @if (analytics()!.avgResponseTimeMs) {
                  <div class="stat-item">
                    <span class="stat-value">{{ (analytics()!.avgResponseTimeMs! / 1000).toFixed(1) }}s</span>
                    <span class="stat-label">Avg Response Time</span>
                  </div>
                }
                @if (analytics()!.avgConfidenceScore) {
                  <div class="stat-item">
                    <span class="stat-value">{{ (analytics()!.avgConfidenceScore! * 100).toFixed(0) }}%</span>
                    <span class="stat-label">Avg Confidence</span>
                  </div>
                }
              </div>
            } @else {
              <p class="no-data">Loading analytics…</p>
            }
          </mat-card-content>
          <mat-card-actions>
            <button mat-button color="primary" (click)="loadAnalytics()">
              <mat-icon>refresh</mat-icon> Refresh
            </button>
          </mat-card-actions>
        </mat-card>

        <!-- About Card -->
        <mat-card class="settings-card">
          <mat-card-header>
            <mat-icon mat-card-avatar>info</mat-icon>
            <mat-card-title>About ByteHR AI</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <div class="about-info">
              <p><strong>Version:</strong> 1.0.0</p>
              <p><strong>AI Model:</strong> qwen3:14b (via Ollama)</p>
              <p><strong>Embeddings:</strong> nomic-embed-text</p>
              <p><strong>Supported Languages:</strong> Albanian, Serbian, English</p>
              <p><strong>Supported Countries:</strong> Albania (AL), Serbia (RS)</p>
            </div>
          </mat-card-content>
        </mat-card>

      </div>
    </div>
  `,
  styles: [`
    .settings-page {
      height: 100vh;
      display: flex;
      flex-direction: column;
      background: #F5F5F5;
    }
    mat-toolbar { flex-shrink: 0; gap: 8px; }
    .settings-content {
      flex: 1;
      overflow-y: auto;
      padding: 24px;
      display: flex;
      flex-direction: column;
      gap: 20px;
      max-width: 800px;
      margin: 0 auto;
      width: 100%;
    }
    .settings-card { width: 100%; }
    .stats-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(130px, 1fr));
      gap: 16px;
      padding: 8px 0;
    }
    .stat-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      background: #F3F2F1;
      border-radius: 8px;
      padding: 16px 8px;
    }
    .stat-value {
      font-size: 28px;
      font-weight: 700;
      color: #0078D4;
    }
    .stat-value.success { color: #107C10; }
    .stat-value.warn    { color: #D83B01; }
    .stat-label { font-size: 12px; color: #605E5C; margin-top: 4px; }
    .no-data { color: #A19F9D; text-align: center; padding: 20px 0; }
    .about-info p { margin: 6px 0; font-size: 14px; }
  `]
})
export class SettingsPageComponent implements OnInit {
  private readonly citationService = inject(CitationService);
  private readonly snackBar = inject(MatSnackBar);

  readonly analytics = signal<AnalyticsSummary | null>(null);

  ngOnInit(): void {
    this.loadAnalytics();
  }

  loadAnalytics(): void {
    this.citationService.getAnalytics(20).subscribe({
      next: (data) => this.analytics.set(data),
      error: () => this.snackBar.open('Could not load analytics.', 'Dismiss', { duration: 3000 })
    });
  }
}
