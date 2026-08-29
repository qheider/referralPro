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

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
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
        return toPng(buildBareImage(url, sizePx));
    }

    /**
     * Same QR code as {@link #generateForUrl}, but composited onto a taller white canvas with
     * {@code headerLines} centered above it (first line bold/larger - meant for the company name,
     * remaining lines regular weight - meant for the campaign name). Used by the company-admin
     * dashboard's ambassador view (see {@link ReferralRedirectController}'s {@code withHeader}
     * query param) so a downloaded/printed QR code is self-identifying; blank lines are skipped
     * entirely rather than leaving empty space.
     */
    public byte[] generateForUrlWithHeader(String url, int sizePx, String... headerLines) {
        BufferedImage qrImage = buildBareImage(url, sizePx);

        java.util.List<String> lines = Arrays.stream(headerLines)
                .filter(line -> line != null && !line.isBlank())
                .toList();
        if (lines.isEmpty()) {
            return toPng(qrImage);
        }

        Font titleFont = new Font(Font.SANS_SERIF, Font.BOLD, Math.max(14, sizePx / 18));
        Font subtitleFont = new Font(Font.SANS_SERIF, Font.PLAIN, Math.max(12, sizePx / 22));
        int padding = Math.max(8, sizePx / 32);
        int lineGap = padding / 2;

        // Measure using a throwaway graphics context before committing to the canvas height.
        BufferedImage measuring = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D measuringGraphics = measuring.createGraphics();
        int headerHeight = padding;
        for (int i = 0; i < lines.size(); i++) {
            FontMetrics metrics = measuringGraphics.getFontMetrics(i == 0 ? titleFont : subtitleFont);
            headerHeight += metrics.getHeight();
            if (i < lines.size() - 1) {
                headerHeight += lineGap;
            }
        }
        headerHeight += padding;
        measuringGraphics.dispose();

        BufferedImage canvas = new BufferedImage(sizePx, headerHeight + sizePx, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = canvas.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        graphics.setColor(Color.BLACK);
        int y = padding;
        for (int i = 0; i < lines.size(); i++) {
            Font font = i == 0 ? titleFont : subtitleFont;
            graphics.setFont(font);
            FontMetrics metrics = graphics.getFontMetrics(font);
            int textWidth = metrics.stringWidth(lines.get(i));
            int x = Math.max(0, (canvas.getWidth() - textWidth) / 2);
            y += metrics.getAscent();
            graphics.drawString(lines.get(i), x, y);
            y += metrics.getDescent() + metrics.getLeading() + lineGap;
        }
        graphics.drawImage(qrImage, 0, headerHeight, null);
        graphics.dispose();

        return toPng(canvas);
    }

    private BufferedImage buildBareImage(String url, int sizePx) {
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
            return MatrixToImageWriter.toBufferedImage(matrix);
        } catch (WriterException e) {
            throw new IllegalStateException("Failed to encode QR code for URL: " + url, e);
        }
    }

    private byte[] toPng(BufferedImage image) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to render QR code PNG", e);
        }
    }
}
