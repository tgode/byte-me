import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { Citation } from '../../models/chat.model';

@Component({
  selector: 'app-citation-panel',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatIconModule, MatChipsModule],
  template: `
    @if (citations && citations.length > 0) {
      <div class="citation-panel">
        <div class="citation-header">
          <mat-icon class="citation-icon">library_books</mat-icon>
          <span>Sources ({{ citations.length }})</span>
        </div>
        <div class="citations-list">
          @for (citation of citations; track citation.documentName) {
            <div class="citation-item">
              <mat-icon class="doc-icon">description</mat-icon>
              <div class="citation-content">
                <span class="doc-name">{{ citation.documentName }}</span>
                @if (citation.pageNumber) {
                  <span class="meta">Page {{ citation.pageNumber }}</span>
                }
                @if (citation.section) {
                  <span class="meta">{{ citation.section }}</span>
                }
                @if (citation.sourcePath || citation.webUrl) {
                  <a [href]="citation.webUrl || citation.sourcePath"
                     target="_blank"
                     class="view-link">
                    <mat-icon inline>open_in_new</mat-icon> View
                  </a>
                }
              </div>
            </div>
          }
        </div>
      </div>
    }
  `,
  styles: [`
    .citation-panel {
      background: var(--c-primary-light);
      border-left: 3px solid var(--c-primary);
      border-radius: var(--radius-sm);
      padding: 8px 12px;
      font-size: 12px;
    }
    .citation-header {
      display: flex;
      align-items: center;
      gap: 6px;
      font-weight: 600;
      color: var(--c-text);
      margin-bottom: 6px;
    }
    .citation-icon {
      font-size: 14px;
      width: 14px;
      height: 14px;
      color: var(--c-primary);
    }
    .citations-list { display: flex; flex-direction: column; gap: 6px; }
    .citation-item  { display: flex; align-items: flex-start; gap: 6px; }
    .doc-icon {
      font-size: 14px;
      width: 14px;
      height: 14px;
      color: var(--c-text-secondary);
      margin-top: 1px;
      flex-shrink: 0;
    }
    .citation-content {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 6px;
    }
    .doc-name { font-weight: 500; color: var(--c-text); }
    .meta {
      color: var(--c-text-secondary);
      background: var(--c-border);
      padding: 1px 7px;
      border-radius: var(--radius-full);
    }
    .view-link {
      display: flex;
      align-items: center;
      gap: 2px;
      color: var(--c-primary);
      text-decoration: none;
      font-size: 11px;
    }
    .view-link:hover { text-decoration: underline; }
  `]
})
export class CitationPanelComponent {
  @Input() citations: Citation[] = [];
}
