import { Component, Inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

export interface DocumentViewerData {
  documentId: string;
  documentName: string;
}

interface DocumentContent {
  id: string;
  name: string;
  country: string;
  content: string;
}

@Component({
  selector: 'app-document-viewer-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  template: `
    <div class="dialog-wrapper">
      <div class="dialog-header">
        <div class="header-left">
          <mat-icon class="header-icon">description</mat-icon>
          <div class="header-text">
            <span class="doc-title">{{ data.documentName }}</span>
            @if (doc()) {
              <span class="doc-country">{{ doc()!.country !== 'ALL' ? doc()!.country : 'Global' }}</span>
            }
          </div>
        </div>
        <button mat-icon-button (click)="close()" class="close-btn" aria-label="Close">
          <mat-icon>close</mat-icon>
        </button>
      </div>

      <div class="dialog-content" mat-dialog-content>
        @if (loading()) {
          <div class="loading-state">
            <mat-spinner diameter="36" />
            <span>Loading document…</span>
          </div>
        } @else if (error()) {
          <div class="error-state">
            <mat-icon>error_outline</mat-icon>
            <span>{{ error() }}</span>
          </div>
        } @else if (doc()) {
          <pre class="doc-content">{{ doc()!.content }}</pre>
        }
      </div>

      <div class="dialog-actions" mat-dialog-actions align="end">
        <button mat-button (click)="close()">Close</button>
      </div>
    </div>
  `,
  styles: [`
    .dialog-wrapper { display: flex; flex-direction: column; width: 100%; background: var(--c-surface); }

    .dialog-header {
      display: flex; align-items: center; justify-content: space-between;
      padding: 16px 20px 12px;
      border-bottom: 1px solid var(--c-border);
      flex-shrink: 0;
    }
    .header-left { display: flex; align-items: center; gap: 12px; }
    .header-icon { color: var(--c-primary); font-size: 22px; width: 22px; height: 22px; }
    .header-text { display: flex; flex-direction: column; gap: 2px; }
    .doc-title { font-size: 15px; font-weight: 600; color: var(--c-text); line-height: 1.2; }
    .doc-country {
      font-size: 11px; font-weight: 500;
      background: var(--c-primary-light);
      color: var(--c-primary);
      padding: 1px 8px;
      border-radius: var(--radius-full);
      width: fit-content;
    }
    .close-btn { color: var(--c-text-secondary); }
    .close-btn:hover { color: var(--c-text); }

    .dialog-content {
      padding: 20px !important;
      max-height: 60vh;
      overflow-y: auto;
    }

    .loading-state, .error-state {
      display: flex; flex-direction: column; align-items: center;
      justify-content: center; gap: 12px; padding: 48px 0;
      color: var(--c-text-secondary);
    }
    .error-state mat-icon { font-size: 36px; width: 36px; height: 36px; color: #d83b01; }

    .doc-content {
      font-family: 'Segoe UI', system-ui, sans-serif;
      font-size: 13px;
      line-height: 1.7;
      color: var(--c-text);
      white-space: pre-wrap;
      word-break: break-word;
      margin: 0;
      padding: 0;
    }

    .dialog-actions {
      padding: 12px 20px;
      border-top: 1px solid var(--c-border);
      margin: 0 !important;
    }
  `]
})
export class DocumentViewerDialogComponent {
  loading = signal(true);
  error = signal<string | null>(null);
  doc = signal<DocumentContent | null>(null);

  constructor(
    public dialogRef: MatDialogRef<DocumentViewerDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: DocumentViewerData,
    private http: HttpClient
  ) {
    this.http.get<DocumentContent>(`${environment.apiUrl}/api/documents/${data.documentId}/content`)
      .subscribe({
        next: (d) => { this.doc.set(d); this.loading.set(false); },
        error: () => {
          this.error.set('Could not load document content.');
          this.loading.set(false);
        }
      });
  }

  close() { this.dialogRef.close(); }
}
