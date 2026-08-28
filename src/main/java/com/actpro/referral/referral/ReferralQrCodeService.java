package com.actpro.referral.referral;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

/**
 * Renders a URL as a scannable QR code PNG. {@link #generatePng} is the original, code-only entry
 * point: it renders a referral link's {@code /r/{code}} URL - scanning it is equivalent to
 * visiting the link by hand: same {@link com.actpro.referral.click.ReferralClickService}
 * click-tracking, same redirect, same 404 for a code that doesn't resolve. {@link #generateForUrl}
 * is the general form, used by {@link ReferralRedirectController}'s {@code /r/link/{token}/qrcode}
 * route to encode whatever URL {@link ReferralLinkUrlService} resolves for a given link (either an
 * {@code /r/{token}} URL or, in direct-to-landing-page mode, the company's own landing page).
 * Deliberately doesn't touch the DB to validate its input - the callers already do (or, for
 * generatePng's code param, deliberately don't - see {@link ReferralRedirectController}'s
 * {@code /r/{code}/qrcode}, which serves that URL publicly for any code) - so this stays fast and
 * independent of DB availability.
 */
@Service
public class ReferralQrCodeService {

    // Package-private (not private) so ReferralRedirectController can reuse it for the
    // /r/link/{token}/qrcode route's default size, matching /r/{code}/qrcode's.
    static final int DEFAULT_SIZE_PX = 320;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public byte[] generatePng(String code) {
        return generatePng(code, DEFAULT_SIZE_PX);
    }

    public byte[] generatePng(String code, int sizePx) {
        return generateForUrl(baseUrl + "/r/" + code, sizePx);
    }

    // Encodes an arbitrary URL as a QR PNG - used directly for the /r/link/{token}/qrcode route,
    // whose target URL may be ReferralPro's own /r/{token} link or (direct-to-landing-page mode) an
    // external company landing page; generatePng(code, size) above is just the code-only case of
    // this, kept as a thin wrapper so its existing callers/tests are unaffected.
    public byte[] generateForUrl(String url, int sizePx) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(
                    url,
                    BarcodeFormat.QR_CODE,
                    sizePx,
                    sizePx,
                    Map.of(
                            EncodeHintType.MARGIN, 1,
                            EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M
                    )
            );
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (WriterException e) {
            throw new IllegalStateException("Failed to encode QR code for URL: " + url, e);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to render QR code PNG for URL: " + url, e);
        }
    }
}
