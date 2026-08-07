import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { CompanyIntegrationService } from '../../core/services/company-integration.service';
import { ApiSubmissionDetailResponse } from '../../shared/models/company-integration.model';
import { extractApiErrorMessage } from '../../shared/utils/error-message';

@Component({
  selector: 'app-integration-submission-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './integration-submission-detail.component.html',
  styleUrl: './integration-submission-detail.component.css'
})
export class IntegrationSubmissionDetailComponent implements OnInit {
  submission: ApiSubmissionDetailResponse | null = null;
  isLoading = false;
  errorMessage = '';

  constructor(
    private route: ActivatedRoute,
    private companyIntegrationService: CompanyIntegrationService
  ) {}

  ngOnInit(): void {
    const submissionId = Number(this.route.snapshot.paramMap.get('submissionId'));
    if (!Number.isInteger(submissionId) || submissionId <= 0) {
      this.errorMessage = 'Invalid submission id.';
      return;
    }

    this.isLoading = true;
    this.companyIntegrationService
      .getSubmission(submissionId)
      .pipe(finalize(() => (this.isLoading = false)))
      .subscribe({
        next: submission => {
          this.submission = submission;
        },
        error: (error: unknown) => {
          this.errorMessage = extractApiErrorMessage(error, 'Unable to load submission detail.');
        }
      });
  }
}
