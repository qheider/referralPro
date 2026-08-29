import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

/**
 * Renders a scannable QR code for a referral link. \`qrCodeUrl\` is always backend-resolved (see
 * ReferralLinkSummaryResponse.qrCodeUrl / ReferralRedirectController's /r/{code}/qrcode and
 * /r/link/{token}/qrcode routes) - this component never derives it itself, because in
 * direct-to-landing-page mode \`referralUrl\` (shown as plain text below the code) is the company's
 * own external landing page, not a ReferralPro path an image URL could be appended to.
 *
 * \`theme\` picks the text colors: 'dark' (default) matches the ambassador portal's dark shell;
 * 'light' matches the company-admin dashboard's white-card shell.
 */
@Component({
  selector: 'app-referral-qr-code',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="flex items-center gap-4" *ngIf="qrCodeUrl">
      <img
        [src]="qrCodeUrl"
        alt="QR code for this referral link"
        class="h-28 w-28 flex-none rounded-lg border p-2 bg-white"
        [class.border-slate-800]="theme === 'dark'"
        [class.border-slate-200]="theme === 'light'"
      />
      <div class="space-y-2">
        <p class="text-xs uppercase tracking-wide" [class.text-slate-500]="theme === 'dark'" [class.text-slate-400]="theme === 'light'">
          Scan to open this referral link
        </p>
        <button
          type="button"
          (click)="download()"
          [disabled]="isDownloading"
          class="text-sm underline decoration-dotted underline-offset-2 disabled:opacity-50"
          [class.text-cyan-300]="theme === 'dark'"
          [class.text-indigo-600]="theme === 'light'"
        >
          {{ isDownloading ? 'Preparing download…' : 'Download QR code' }}
        </button>
        <p *ngIf="downloadError" class="text-xs" [class.text-red-400]="theme === 'dark'" [class.text-red-600]="theme === 'light'">
          {{ downloadError }}
        </p>
      </div>
    </div>
  `
})
export class ReferralQrCodeComponent {
  @Input({ required: true }) qrCodeUrl!: string;
  // Lets callers give the downloaded file a meaningful name (e.g. company + campaign name for the
  // company-admin view's branded QR code) instead of the generic default.
  @Input() fileName = 'referral-qr-code.png';
  @Input() theme: 'dark' | 'light' = 'dark';

  isDownloading = false;
  downloadError = '';

  async download(): Promise<void> {
    this.isDownloading = true;
    this.downloadError = '';

    try {
      // Native fetch, not HttpClient: /r/{code}/qrcode is public and lives on the backend's root
      // origin, not under environment.apiUrl - HttpClient's authInterceptor attaches a JWT and
      // treats any error response as a session problem (logout + redirect to /login), neither of
      // which applies to this unauthenticated image endpoint. Fetching the bytes ourselves (rather
      // than relying on <a download> across origins, which most browsers ignore) is what makes the
      // download reliable.
      const response = await fetch(this.qrCodeUrl);
      if (!response.ok) {
        throw new Error(`Unexpected response (${response.status})`);
      }

      const blob = await response.blob();
      const objectUrl = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = objectUrl;
      link.download = this.fileName;
      link.click();
      URL.revokeObjectURL(objectUrl);
    } catch {
      this.downloadError = 'Unable to download QR code.';
    } finally {
      this.isDownloading = false;
    }
  }
}
