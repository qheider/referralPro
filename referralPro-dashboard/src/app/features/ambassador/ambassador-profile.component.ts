import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { AmbassadorPortalService } from '../../core/services/ambassador-portal.service';
import { AmbassadorProfile } from '../../shared/models/ambassador-portal.model';
import { extractApiErrorMessage } from '../../shared/utils/error-message';

@Component({
  selector: 'app-ambassador-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="space-y-6">
      <div>
        <h2 class="text-2xl font-semibold">My profile</h2>
        <p class="text-sm text-slate-400">Update the public information tied to your ambassador account.</p>
      </div>

      <p *ngIf="successMessage" class="rounded-lg border border-emerald-500/30 bg-emerald-500/10 px-4 py-3 text-sm text-emerald-200">{{ successMessage }}</p>
      <p *ngIf="errorMessage" class="rounded-lg border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-200">{{ errorMessage }}</p>

      <form [formGroup]="profileForm" class="grid gap-4 md:grid-cols-2" (ngSubmit)="save()">
        <label class="space-y-2">
          <span class="text-sm text-slate-400">First name</span>
          <input formControlName="firstName" class="w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2 text-sm" />
        </label>
        <label class="space-y-2">
          <span class="text-sm text-slate-400">Last name</span>
          <input formControlName="lastName" class="w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2 text-sm" />
        </label>
        <label class="space-y-2">
          <span class="text-sm text-slate-400">Display name</span>
          <input formControlName="displayName" class="w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2 text-sm" />
        </label>
        <label class="space-y-2">
          <span class="text-sm text-slate-400">Phone</span>
          <input formControlName="phone" class="w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2 text-sm" />
        </label>
        <label class="space-y-2">
          <span class="text-sm text-slate-400">Social platform</span>
          <input formControlName="socialMediaPlatform" class="w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2 text-sm" />
        </label>
        <label class="space-y-2">
          <span class="text-sm text-slate-400">Social handle</span>
          <input formControlName="socialMediaHandle" class="w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2 text-sm" />
        </label>
        <label class="space-y-2 md:col-span-2">
          <span class="text-sm text-slate-400">Profile image URL</span>
          <input formControlName="profileImageUrl" class="w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2 text-sm" />
        </label>
        <label class="space-y-2 md:col-span-2">
          <span class="text-sm text-slate-400">Bio</span>
          <textarea formControlName="bio" rows="4" class="w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2 text-sm"></textarea>
        </label>
        <div class="md:col-span-2 flex items-center justify-between rounded-lg border border-slate-800 bg-slate-950 px-4 py-3 text-sm">
          <div>
            <p class="text-slate-400">Ambassador code</p>
            <p class="font-medium">{{ profile?.ambassadorCode || '—' }}</p>
          </div>
          <button type="submit" class="rounded-md bg-cyan-500 px-4 py-2 font-medium text-slate-950">Save profile</button>
        </div>
      </form>
    </div>
  `
})
export class AmbassadorProfileComponent implements OnInit {
  profile: AmbassadorProfile | null = null;
  successMessage = '';
  errorMessage = '';
  profileForm: FormGroup;

  constructor(
    private formBuilder: FormBuilder,
    private ambassadorPortalService: AmbassadorPortalService
  ) {
    this.profileForm = this.formBuilder.group({
      firstName: [''],
      lastName: [''],
      displayName: [''],
      phone: [''],
      bio: [''],
      socialMediaPlatform: [''],
      socialMediaHandle: [''],
      profileImageUrl: ['']
    });
  }

  ngOnInit(): void {
    this.ambassadorPortalService.getProfile().subscribe({
      next: profile => {
        this.profile = profile;
        this.profileForm.patchValue({
          firstName: profile.firstName || '',
          lastName: profile.lastName || '',
          displayName: profile.displayName || '',
          phone: profile.phone || '',
          bio: profile.bio || '',
          socialMediaPlatform: profile.socialMediaPlatform || '',
          socialMediaHandle: profile.socialMediaHandle || '',
          profileImageUrl: profile.profileImageUrl || ''
        });
      },
      error: error => {
        this.errorMessage = extractApiErrorMessage(error, 'Unable to load your profile.');
      }
    });
  }

  save(): void {
    this.successMessage = '';
    this.errorMessage = '';

    this.ambassadorPortalService.updateProfile(this.profileForm.getRawValue()).subscribe({
      next: profile => {
        this.profile = profile;
        this.successMessage = 'Profile updated successfully.';
      },
      error: error => {
        this.errorMessage = extractApiErrorMessage(error, 'Unable to update your profile.');
      }
    });
  }
}
