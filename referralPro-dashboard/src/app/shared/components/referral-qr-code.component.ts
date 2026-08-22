import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

/**
 * Renders a scannable QR code for a referral link, backed by the backend's public
 * GET /r/{code}/qrcode endpoint (see ReferralRedirectController/ReferralQrCodeService) - the QR
 * encodes the exact same /r/{code} URL as the link itself, so scanning it does exactly what
 * clicking the link does (same click-tracking, same redirect).
 */
@Component({
  selector: 'app-referral-qr-code',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="flex items-center gap-4" *ngIf="referralUrl">
      <img
        [src]="qrCodeUrl"
        alt="QR code for this referral link"
        class="h-28 w-28 flex-none rounded-lg border border-slate-800 bg-white p-2"
      />
      <div class="space-y-2">
        <p class="text-xs uppercase tracking-wide text-slate-500">Scan to open this referral link</p>
        <button
          type="button"
          (click)="download()"
          [disabled]="isDownloading"
          class="text-sm text-cyan-300 underline decoration-dotted underline-offset-2 disabled:opacity-50"
        >
          {{ isDownloading ? 'Preparing download…' : 'Download QR code' }}
        </button>
        <p *ngIf="downloadError" class="text-xs text-red-400">{{ downloadError }}</p>
      </div>
    </div>
  `
})
export class ReferralQrCodeComponent {
  @Input({ required: true }) referralUrl!: string;

  isDownloading = false;
  downloadError = '';

  get qrCodeUrl(): string {
    return `${this.referralUrl}/qrcode`;
  }

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
      link.download = 'referral-qr-code.png';
      link.click();
      URL.revokeObjectURL(objectUrl);
    } catch {
      this.downloadError = 'Unable to download QR code.';
    } finally {
      this.isDownloading = false;
    }
  }
}
