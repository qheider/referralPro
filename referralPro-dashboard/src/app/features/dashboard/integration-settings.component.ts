import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { CompanyIntegrationService } from '../../core/services/company-integration.service';
import { CompanyIntegrationConfigResponse, IntegrationAuthType, TestConnectionResponse } from '../../shared/models/company-integration.model';
import { extractApiErrorMessage } from '../../shared/utils/error-message';

@Component({
  selector: 'app-integration-settings',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './integration-settings.component.html',
  styleUrl: './integration-settings.component.css'
})
export class IntegrationSettingsComponent implements OnInit {
  readonly form;

  config: CompanyIntegrationConfigResponse | null = null;
  isLoading = false;
  isSaving = false;
  isTesting = false;
  isTransitioning = false;
  errorMessage = '';
  testResult: TestConnectionResponse | null = null;

  isGeneratingWebhookSecret = false;
  webhookUrlCopied = false;
  // Shown once, immediately after generation - never persisted beyond this session, never
  // re-fetched from the backend (which never returns it decrypted again).
  generatedWebhookSecret: string | null = null;

  constructor(
    private fb: FormBuilder,
    private companyIntegrationService: CompanyIntegrationService,
    private cdr: ChangeDetectorRef
  ) {
    this.form = this.fb.group({
      apiBaseUrl: ['', [Validators.required, Validators.maxLength(500)]],
      authType: ['NONE' as IntegrationAuthType, [Validators.required]],
      apiKeyHeaderName: [''],
      apiKeyValue: [''],
      bearerToken: [''],
      basicUsername: [''],
      basicPassword: [''],
      requestTimeoutMs: [10000, [Validators.required, Validators.min(1)]],
      maxRetryAttempts: [5, [Validators.required, Validators.min(0)]],
      statusMappingJson: [''],
      rewardMappingJson: ['']
    });
  }

  ngOnInit(): void {
    this.load();
  }

  get authType(): IntegrationAuthType {
    return this.form.getRawValue().authType as IntegrationAuthType;
  }

  get canEnable(): boolean {
    return this.config?.status === 'DISABLED';
  }

  get canDisable(): boolean {
    return !!this.config && this.config.status !== 'DISABLED' && this.config.status !== 'NOT_CONFIGURED';
  }

  get statusBadgeClass(): string {
    switch (this.config?.status) {
      case 'ACTIVE':
        return 'bg-emerald-50 text-emerald-700';
      case 'ERROR':
        return 'bg-red-50 text-red-700';
      case 'DISABLED':
        return 'bg-slate-100 text-slate-600';
      case 'PENDING_VERIFICATION':
        return 'bg-amber-50 text-amber-700';
      default:
        return 'bg-slate-100 text-slate-600';
    }
  }

  load(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.companyIntegrationService
      .getConfig()
      .pipe(finalize(() => (this.isLoading = false)))
      .subscribe({
        next: config => {
          this.config = config;
          this.form.patchValue({
            apiBaseUrl: config.apiBaseUrl ?? '',
            authType: config.authType,
            requestTimeoutMs: config.requestTimeoutMs,
            maxRetryAttempts: config.maxRetryAttempts,
            statusMappingJson: config.statusMappingJson ?? '',
            rewardMappingJson: config.rewardMappingJson ?? ''
          });
        },
        error: (error: unknown) => {
          this.errorMessage = extractApiErrorMessage(error, 'Unable to load integration configuration.');
        }
      });
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    this.isSaving = true;
    this.errorMessage = '';
    this.testResult = null;

    this.companyIntegrationService
      .updateConfig({
        apiBaseUrl: raw.apiBaseUrl ?? '',
        authType: (raw.authType as IntegrationAuthType) ?? 'NONE',
        apiKeyHeaderName: raw.apiKeyHeaderName || null,
        apiKeyValue: raw.apiKeyValue || null,
        bearerToken: raw.bearerToken || null,
        basicUsername: raw.basicUsername || null,
        basicPassword: raw.basicPassword || null,
        requestTimeoutMs: raw.requestTimeoutMs ?? null,
        maxRetryAttempts: raw.maxRetryAttempts ?? null,
        statusMappingJson: raw.statusMappingJson || null,
        rewardMappingJson: raw.rewardMappingJson || null
      })
      .pipe(finalize(() => (this.isSaving = false)))
      .subscribe({
        next: config => {
          this.config = config;
          // Credential values are never echoed back - clear the input fields so the form doesn't
          // imply the entered value is still "there" beyond this save.
          this.form.patchValue({ apiKeyValue: '', bearerToken: '', basicUsername: '', basicPassword: '' });
        },
        error: (error: unknown) => {
          this.errorMessage = extractApiErrorMessage(error, 'Unable to save integration configuration.');
        }
      });
  }

  testConnection(): void {
    this.isTesting = true;
    this.errorMessage = '';
    this.testResult = null;

    this.companyIntegrationService
      .testConnection()
      .pipe(finalize(() => (this.isTesting = false)))
      .subscribe({
        next: result => {
          this.testResult = result;
          if (this.config) {
            this.config = { ...this.config, status: result.resultingStatus };
          }
        },
        error: (error: unknown) => {
          this.errorMessage = extractApiErrorMessage(error, 'Unable to test the connection.');
        }
      });
  }

  enable(): void {
    this.runTransition(() => this.companyIntegrationService.enable());
  }

  disable(): void {
    this.runTransition(() => this.companyIntegrationService.disable());
  }

  private runTransition(request: () => ReturnType<CompanyIntegrationService['enable']>): void {
    this.isTransitioning = true;
    this.errorMessage = '';

    request()
      .pipe(finalize(() => (this.isTransitioning = false)))
      .subscribe({
        next: config => {
          this.config = config;
        },
        error: (error: unknown) => {
          this.errorMessage = extractApiErrorMessage(error, 'Unable to update the integration status.');
        }
      });
  }

  copyWebhookUrl(): void {
    if (!this.config) {
      return;
    }
    navigator.clipboard.writeText(this.config.webhookUrl).then(() => {
      this.webhookUrlCopied = true;
      setTimeout(() => {
        this.webhookUrlCopied = false;
        this.cdr.markForCheck();
      }, 2000);
      this.cdr.markForCheck();
    });
  }

  generateWebhookSecret(): void {
    this.isGeneratingWebhookSecret = true;
    this.errorMessage = '';

    this.companyIntegrationService
      .generateWebhookSecret()
      .pipe(finalize(() => (this.isGeneratingWebhookSecret = false)))
      .subscribe({
        next: result => {
          this.generatedWebhookSecret = result.webhookSecret;
          if (this.config) {
            this.config = { ...this.config, hasWebhookSigningSecret: true };
          }
        },
        error: (error: unknown) => {
          this.errorMessage = extractApiErrorMessage(error, 'Unable to generate the webhook signing secret.');
        }
      });
  }

  dismissGeneratedWebhookSecret(): void {
    this.generatedWebhookSecret = null;
  }
}
