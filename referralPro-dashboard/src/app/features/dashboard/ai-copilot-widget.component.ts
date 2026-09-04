import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { AiCopilotService } from '../../core/services/ai-copilot.service';
import { AskDashboardResponse } from '../../shared/models/ai.model';
import { extractApiErrorMessage } from '../../shared/utils/error-message';

@Component({
  selector: 'app-ai-copilot-widget',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ai-copilot-widget.component.html',
  styleUrl: './ai-copilot-widget.component.css'
})
export class AiCopilotWidgetComponent {
  isOpen = false;
  isLoading = false;
  question = '';
  errorMessage = '';
  response: AskDashboardResponse | null = null;

  constructor(private aiCopilotService: AiCopilotService) {}

  toggle(): void {
    this.isOpen = !this.isOpen;
  }

  ask(): void {
    const trimmed = this.question.trim();
    if (!trimmed || this.isLoading) {
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    this.response = null;

    this.aiCopilotService
      .askDashboard({ question: trimmed })
      .pipe(finalize(() => (this.isLoading = false)))
      .subscribe({
        next: response => (this.response = response),
        error: (error: unknown) => (this.errorMessage = extractApiErrorMessage(error, 'Unable to answer that right now.'))
      });
  }
}
