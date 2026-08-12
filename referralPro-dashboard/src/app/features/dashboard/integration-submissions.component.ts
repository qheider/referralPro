import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { CompanyIntegrationService } from '../../core/services/company-integration.service';
import { ApiSubmissionStatus, ApiSubmissionSummaryResponse } from '../../shared/models/company-integration.model';
import { extractApiErrorMessage } from '../../shared/utils/error-message';

const STATUS_FILTERS: (ApiSubmissionStatus | null)[] = [
  null,
  'PENDING',
  'PROCESSING',
  'RETRY_SCHEDULED',
  'SUCCEEDED',
  'PERMANENTLY_FAILED',
  'CANCELLED'
];

@Component({
  selector: 'app-integration-submissions',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './integration-submissions.component.html',
  styleUrl: './integration-submissions.component.css'
})
export class IntegrationSubmissionsComponent implements OnInit {
  readonly statusFilters = STATUS_FILTERS;

  submissions: ApiSubmissionSummaryResponse[] = [];
  selectedStatus: ApiSubmissionStatus | null = null;
  isLoading = false;
  errorMessage = '';

  constructor(private companyIntegrationService: CompanyIntegrationService) {}

  ngOnInit(): void {
    this.load();
  }

  selectStatus(status: ApiSubmissionStatus | null): void {
    this.selectedStatus = status;
    this.load();
  }

  load(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.companyIntegrationService
      .listSubmissions(this.selectedStatus)
      .pipe(finalize(() => (this.isLoading = false)))
      .subscribe({
        next: submissions => {
          this.submissions = submissions;
        },
        error: (error: unknown) => {
          this.errorMessage = extractApiErrorMessage(error, 'Unable to load submissions.');
        }
      });
  }

  statusBadgeClass(status: ApiSubmissionStatus): string {
    switch (status) {
      case 'SUCCEEDED':
        return 'bg-emerald-50 text-emerald-700';
      case 'PERMANENTLY_FAILED':
        return 'bg-red-50 text-red-700';
      case 'RETRY_SCHEDULED':
        return 'bg-amber-50 text-amber-700';
      case 'CANCELLED':
        return 'bg-slate-100 text-slate-500';
      default:
        return 'bg-indigo-50 text-indigo-700';
    }
  }
}
