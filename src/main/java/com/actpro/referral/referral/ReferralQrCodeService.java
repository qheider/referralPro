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
 * Renders a referral link's {@code /r/{code}} URL as a scannable QR code PNG - scanning it is
 * equivalent to visiting the link by hand: same {@link com.actpro.referral.click.ReferralClickService}
 * click-tracking, same redirect, same 404 for a code that doesn't resolve. Deliberately doesn't
 * touch the DB to validate {@code code} - {@link ReferralRedirectController} already serves that
 * URL publicly and unauthenticated for any code, so there's nothing extra to protect or validate
 * here, and it keeps this endpoint fast and independent of DB availability.
 */
@Service
public class ReferralQrCodeService {

    private static final int DEFAULT_SIZE_PX = 320;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public byte[] generatePng(String code) {
        return generatePng(code, DEFAULT_SIZE_PX);
    }

    public byte[] generatePng(String code, int sizePx) {
        String url = baseUrl + "/r/" + code;
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
            throw new IllegalStateException("Failed to encode QR code for referral code: " + code, e);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to render QR code PNG for referral code: " + code, e);
        }
    }
}
