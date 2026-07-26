import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

interface RegistrationRequest {
  companyName: string;
  companyEmail: string;
  companyWebsite: string;
  industry: string;
  country: string;
  taxId?: string;
  adminFullName: string;
  adminWorkEmail: string;
  adminPhone: string;
  password: string;
  adminRole: string;
  companySize: string;
  preferredCurrency: string;
  acceptedTerms: boolean;
  companyLogoUrl?: string;
  address?: {
    street?: string;
    city?: string;
    state?: string;
    zip?: string;
  };
  companyDescription?: string;
  socialLinks?: {
    linkedin?: string;
    twitter?: string;
    facebook?: string;
  };
  registrationCertificateUrl?: string;
  secondaryContactName?: string;
  department?: string;
  referralProgramManager?: string;
  estimatedMonthlyReferralVolume?: number;
  rewardTypePreference?: string;
  referralSource?: string;
  signupReferralCode?: string;
  gstVatNumber?: string;
  industryLicenseNumber?: string;
  dpaAccepted?: boolean;
}

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './register.component.html',
  styleUrl: './register.component.css'
})
export class RegisterComponent {
  registrationForm: FormGroup;
  loading = false;
  error = '';
  currentStep = 1;
  totalSteps = 4;

  companySizes = ['1-10', '11-50', '51-200', '201-500', '500+'];
  currencies = ['USD', 'EUR', 'GBP', 'INR', 'AUD', 'CAD'];
  rewardTypes = ['cash', 'gift_card', 'discount', 'points'];
  industries = [
    'Technology', 'Finance', 'Healthcare', 'Retail', 'Education',
    'Manufacturing', 'Real Estate', 'Consulting', 'Other'
  ];
  
  countries = [
    'United States', 'United Kingdom', 'Canada', 'Australia',
    'India', 'Germany', 'France', 'Singapore', 'Other'
  ];

  constructor(
    private fb: FormBuilder,
    private http: HttpClient,
    private router: Router
  ) {
    this.registrationForm = this.fb.group({
      // Step 1: Company Information
      companyName: ['', Validators.required],
      companyEmail: ['', [Validators.required, Validators.email]],
      companyWebsite: ['', Validators.required],
      industry: ['', Validators.required],
      country: ['', Validators.required],
      taxId: [''],
      companySize: ['', Validators.required],
      preferredCurrency: ['USD', Validators.required],
      companyDescription: [''],

      // Step 2: Admin Account
      adminFullName: ['', Validators.required],
      adminWorkEmail: ['', [Validators.required, Validators.email]],
      adminPhone: ['', Validators.required],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', Validators.required],
      adminRole: ['COMPANY_ADMIN', Validators.required],

      // Step 3: Additional Details (Optional)
      addressStreet: [''],
      addressCity: [''],
      addressState: [''],
      addressZip: [''],
      companyLogoUrl: [''],
      linkedinUrl: [''],
      twitterUrl: [''],
      facebookUrl: [''],
      secondaryContactName: [''],
      department: [''],
      referralProgramManager: [''],

      // Step 4: Business Preferences
      estimatedMonthlyReferralVolume: [null],
      rewardTypePreference: [''],
      referralSource: [''],
      signupReferralCode: [''],
      gstVatNumber: [''],
      industryLicenseNumber: [''],
      registrationCertificateUrl: [''],
      dpaAccepted: [false],
      acceptedTerms: [false, Validators.requiredTrue]
    }, { validators: this.passwordMatchValidator });
  }

  passwordMatchValidator(group: FormGroup) {
    const password = group.get('password')?.value;
    const confirmPassword = group.get('confirmPassword')?.value;
    return password === confirmPassword ? null : { passwordMismatch: true };
  }

  nextStep() {
    if (this.currentStep < this.totalSteps) {
      this.currentStep++;
    }
  }

  previousStep() {
    if (this.currentStep > 1) {
      this.currentStep--;
    }
  }

  canProceedToNextStep(): boolean {
    switch (this.currentStep) {
      case 1:
        return this.registrationForm.get('companyName')?.valid &&
               this.registrationForm.get('companyEmail')?.valid &&
               this.registrationForm.get('companyWebsite')?.valid &&
               this.registrationForm.get('industry')?.valid &&
               this.registrationForm.get('country')?.valid &&
               this.registrationForm.get('companySize')?.valid &&
               this.registrationForm.get('preferredCurrency')?.valid || false;
      case 2:
        return this.registrationForm.get('adminFullName')?.valid &&
               this.registrationForm.get('adminWorkEmail')?.valid &&
               this.registrationForm.get('adminPhone')?.valid &&
               this.registrationForm.get('password')?.valid &&
               this.registrationForm.get('confirmPassword')?.valid &&
               !this.registrationForm.errors?.['passwordMismatch'] || false;
      case 3:
        return true; // Optional fields
      case 4:
        return this.registrationForm.get('acceptedTerms')?.valid || false;
      default:
        return false;
    }
  }

  onSubmit() {
    if (!this.registrationForm.valid) {
      this.error = 'Please fill all required fields correctly';
      return;
    }

    this.loading = true;
    this.error = '';

    const formValue = this.registrationForm.value;

    const request: RegistrationRequest = {
      companyName: formValue.companyName,
      companyEmail: formValue.companyEmail,
      companyWebsite: formValue.companyWebsite,
      industry: formValue.industry,
      country: formValue.country,
      taxId: formValue.taxId || undefined,
      adminFullName: formValue.adminFullName,
      adminWorkEmail: formValue.adminWorkEmail,
      adminPhone: formValue.adminPhone,
      password: formValue.password,
      adminRole: formValue.adminRole,
      companySize: formValue.companySize,
      preferredCurrency: formValue.preferredCurrency,
      acceptedTerms: formValue.acceptedTerms,
      companyLogoUrl: formValue.companyLogoUrl || undefined,
      companyDescription: formValue.companyDescription || undefined,
      secondaryContactName: formValue.secondaryContactName || undefined,
      department: formValue.department || undefined,
      referralProgramManager: formValue.referralProgramManager || undefined,
      estimatedMonthlyReferralVolume: formValue.estimatedMonthlyReferralVolume || undefined,
      rewardTypePreference: formValue.rewardTypePreference || undefined,
      referralSource: formValue.referralSource || undefined,
      signupReferralCode: formValue.signupReferralCode || undefined,
      gstVatNumber: formValue.gstVatNumber || undefined,
      industryLicenseNumber: formValue.industryLicenseNumber || undefined,
      registrationCertificateUrl: formValue.registrationCertificateUrl || undefined,
      dpaAccepted: formValue.dpaAccepted || undefined
    };

    // Add address if any field is filled
    if (formValue.addressStreet || formValue.addressCity || formValue.addressState || formValue.addressZip) {
      request.address = {
        street: formValue.addressStreet || undefined,
        city: formValue.addressCity || undefined,
        state: formValue.addressState || undefined,
        zip: formValue.addressZip || undefined
      };
    }

    // Add social links if any field is filled
    if (formValue.linkedinUrl || formValue.twitterUrl || formValue.facebookUrl) {
      request.socialLinks = {
        linkedin: formValue.linkedinUrl || undefined,
        twitter: formValue.twitterUrl || undefined,
        facebook: formValue.facebookUrl || undefined
      };
    }

    this.http.post(`${environment.apiUrl}/companies/register`, request)
      .subscribe({
        next: (response: any) => {
          console.log('Registration successful', response);
          alert(`Registration successful! Your API Key: ${response.data.apiKey}`);
          this.router.navigate(['/login']);
        },
        error: (error) => {
          console.error('Registration error', error);
          this.error = error.error?.message || 'Registration failed. Please try again.';
          this.loading = false;
        }
      });
  }
}
