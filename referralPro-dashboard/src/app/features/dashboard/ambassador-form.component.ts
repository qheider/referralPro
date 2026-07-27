import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize, Observable } from 'rxjs';
import { AmbassadorAdminService } from '../../core/services/ambassador-admin.service';
import { AmbassadorDetail } from '../../shared/models/ambassador.model';
import { extractApiErrorMessage } from '../../shared/utils/error-message';

@Component({
  selector: 'app-ambassador-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <section class="mx-auto max-w-4xl space-y-6">
      <div class="rounded-3xl bg-white p-6 shadow-sm">
        <p class="text-sm font-semibold uppercase tracking-wide text-indigo-600">{{ isEditMode ? 'Update' : 'Create' }}</p>
        <h2 class="mt-1 text-2xl font-semibold text-slate-900">{{ isEditMode ? 'Ambassador profile' : 'New ambassador' }}</h2>
        <p class="mt-2 text-sm text-slate-500">
          {{ isEditMode ? 'Edit ambassador profile details and account status.' : 'Invite a new ambassador into your company program.' }}
        </p>
      </div>

      <div *ngIf="errorMessage" class="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
        {{ errorMessage }}
      </div>

      <form [formGroup]="form" (ngSubmit)="submit()" class="rounded-3xl bg-white p-6 shadow-sm">
        <div class="grid gap-4 md:grid-cols-2">
          <label class="space-y-2">
            <span class="text-sm font-medium text-slate-700">First name</span>
            <input formControlName="firstName" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-indigo-500" />
          </label>

          <label class="space-y-2">
            <span class="text-sm font-medium text-slate-700">Last name</span>
            <input formControlName="lastName" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-indigo-500" />
          </label>

          <label class="space-y-2" *ngIf="!isEditMode">
            <span class="text-sm font-medium text-slate-700">Email</span>
            <input formControlName="email" type="email" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-indigo-500" />
          </label>

          <div *ngIf="isEditMode" class="space-y-2">
            <span class="text-sm font-medium text-slate-700">Email</span>
            <div class="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-600">{{ currentEmail }}</div>
          </div>

          <label class="space-y-2">
            <span class="text-sm font-medium text-slate-700">Display name</span>
            <input formControlName="displayName" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-indigo-500" />
          </label>

          <label class="space-y-2">
            <span class="text-sm font-medium text-slate-700">Phone</span>
            <input formControlName="phone" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-indigo-500" />
          </label>

          <label class="space-y-2">
            <span class="text-sm font-medium text-slate-700">Social platform</span>
            <input formControlName="socialMediaPlatform" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-indigo-500" />
          </label>

          <label class="space-y-2">
            <span class="text-sm font-medium text-slate-700">Social handle</span>
            <input formControlName="socialMediaHandle" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-indigo-500" />
          </label>

          <label class="space-y-2 md:col-span-2" *ngIf="isEditMode">
            <span class="text-sm font-medium text-slate-700">Status</span>
            <select formControlName="status" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-indigo-500">
              <option value="INVITED">INVITED</option>
              <option value="ACTIVE">ACTIVE</option>
              <option value="INACTIVE">INACTIVE</option>
              <option value="SUSPENDED">SUSPENDED</option>
            </select>
          </label>

          <label class="space-y-2 md:col-span-2" *ngIf="isEditMode">
            <span class="text-sm font-medium text-slate-700">Bio</span>
            <textarea formControlName="bio" rows="4" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-indigo-500"></textarea>
          </label>

          <label class="space-y-2 md:col-span-2" *ngIf="isEditMode">
            <span class="text-sm font-medium text-slate-700">Profile image URL</span>
            <input formControlName="profileImageUrl" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-indigo-500" />
          </label>
        </div>

        <div class="mt-6 flex flex-wrap gap-3">
          <button type="submit" class="rounded-xl bg-slate-900 px-4 py-2 text-sm font-semibold text-white hover:bg-slate-700" [disabled]="isSaving || form.invalid">
            {{ isSaving ? 'Saving...' : isEditMode ? 'Save changes' : 'Create ambassador' }}
          </button>
          <a routerLink="/dashboard/ambassadors" class="rounded-xl border border-slate-200 px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50">Cancel</a>
        </div>
      </form>
    </section>
  `
})
export class AmbassadorFormComponent implements OnInit {
  readonly form;

  isEditMode = false;
  isSaving = false;
  errorMessage = '';
  ambassadorId: number | null = null;
  currentEmail = '';

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private ambassadorAdminService: AmbassadorAdminService
  ) {
    this.form = this.fb.group({
      firstName: ['', [Validators.required, Validators.maxLength(100)]],
      lastName: ['', [Validators.required, Validators.maxLength(100)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
      displayName: [''],
      phone: [''],
      socialMediaPlatform: [''],
      socialMediaHandle: [''],
      status: ['INVITED'],
      bio: [''],
      profileImageUrl: ['']
    });
  }

  ngOnInit(): void {
    const ambassadorId = this.route.snapshot.paramMap.get('ambassadorId');
    if (!ambassadorId) {
      return;
    }

    this.isEditMode = true;
    this.ambassadorId = Number(ambassadorId);
    this.loadAmbassador();
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSaving = true;
    this.errorMessage = '';

    const rawValue = this.form.getRawValue();
    const request = {
      firstName: rawValue.firstName ?? '',
      lastName: rawValue.lastName ?? '',
      displayName: rawValue.displayName || null,
      phone: rawValue.phone || null,
      socialMediaPlatform: rawValue.socialMediaPlatform || null,
      socialMediaHandle: rawValue.socialMediaHandle || null
    };

    let operation: Observable<unknown>;
    if (this.isEditMode && this.ambassadorId) {
      operation = this.ambassadorAdminService.updateAmbassador(this.ambassadorId, {
        ...request,
        status: (rawValue.status as 'INVITED' | 'ACTIVE' | 'INACTIVE' | 'SUSPENDED') || 'INVITED',
        bio: rawValue.bio || null,
        profileImageUrl: rawValue.profileImageUrl || null
      });
    } else {
      operation = this.ambassadorAdminService.createAmbassador({
        ...request,
        email: rawValue.email ?? ''
      });
    }

    operation
      .pipe(finalize(() => (this.isSaving = false)))
      .subscribe({
        next: () => {
          if (this.isEditMode && this.ambassadorId) {
            this.router.navigate(['/dashboard/ambassadors', this.ambassadorId]);
            return;
          }
          this.router.navigate(['/dashboard/ambassadors']);
        },
        error: (error: unknown) => {
          this.errorMessage = extractApiErrorMessage(error, 'Unable to save ambassador.');
        }
      });
  }

  private loadAmbassador(): void {
    if (!this.ambassadorId) {
      return;
    }

    this.ambassadorAdminService.getAmbassador(this.ambassadorId).subscribe({
      next: ambassador => this.patchAmbassador(ambassador),
      error: error => {
        this.errorMessage = extractApiErrorMessage(error, 'Unable to load ambassador.');
      }
    });
  }

  private patchAmbassador(ambassador: AmbassadorDetail): void {
    this.currentEmail = ambassador.email;
    this.form.patchValue({
      firstName: ambassador.firstName,
      lastName: ambassador.lastName,
      email: ambassador.email,
      displayName: ambassador.displayName ?? '',
      phone: ambassador.phone ?? '',
      socialMediaPlatform: ambassador.socialMediaPlatform ?? '',
      socialMediaHandle: ambassador.socialMediaHandle ?? '',
      status: ambassador.status,
      bio: ambassador.bio ?? '',
      profileImageUrl: ambassador.profileImageUrl ?? ''
    });
  }
}
