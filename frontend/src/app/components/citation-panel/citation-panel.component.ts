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
      background: #F3F2F1;
      border-left: 3px solid #0078D4;
      border-radius: 4px;
      padding: 8px 12px;
      margin-top: 8px;
      font-size: 12px;
    }
    .citation-header {
      display: flex;
      align-items: center;
      gap: 6px;
      font-weight: 600;
      color: #323130;
      margin-bottom: 6px;
    }
    .citation-icon {
      font-size: 14px;
      width: 14px;
      height: 14px;
      color: #0078D4;
    }
    .citations-list {
      display: flex;
      flex-direction: column;
      gap: 6px;
    }
    .citation-item {
      display: flex;
      align-items: flex-start;
      gap: 6px;
    }
    .doc-icon {
      font-size: 14px;
      width: 14px;
      height: 14px;
      color: #605E5C;
      margin-top: 1px;
      flex-shrink: 0;
    }
    .citation-content {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 6px;
    }
    .doc-name {
      font-weight: 500;
      color: #201F1E;
    }
    .meta {
      color: #605E5C;
      background: #E1DFDD;
      padding: 1px 6px;
      border-radius: 10px;
    }
    .view-link {
      display: flex;
      align-items: center;
      gap: 2px;
      color: #0078D4;
      text-decoration: none;
      font-size: 11px;
    }
    .view-link:hover { text-decoration: underline; }
  `]
})
export class CitationPanelComponent {
  @Input() citations: Citation[] = [];
}
