package com.actpro.referral.referral;

import com.actpro.referral.click.ReferralClickService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.view.RedirectView;

import java.time.Duration;
import java.util.UUID;

@Tag(name = "Referral Redirect", description = "Public referral link redirect")
@Controller
@RequiredArgsConstructor
public class ReferralRedirectController {

    private static final Duration ATTRIBUTION_COOKIE_MAX_AGE = Duration.ofDays(30);

    private final ReferralClickService referralClickService;
    private final ReferralQrCodeService referralQrCodeService;

    @Operation(
            summary = "Referral redirect",
            description = "Public endpoint to track clicks and redirect to campaign landing page. " +
                    "Accepts both ambassador referral-link tokens and legacy referral codes."
    )
    @GetMapping("/r/{code}")
    public RedirectView handleReferralRedirect(
            @PathVariable String code,
            HttpServletRequest request,
            HttpServletResponse response) {

        String ipAddress = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        String refererUrl = request.getHeader("Referer");
        String sessionId = resolveSessionId(request, response);

        String redirectUrl = referralClickService.resolveAndRecordClick(code, ipAddress, userAgent, refererUrl, sessionId);

        return new RedirectView(redirectUrl);
    }

    @Operation(
            summary = "Referral link QR code",
            description = "Public endpoint returning a PNG QR code encoding this same /r/{code} URL - " +
                    "scanning it does exactly what clicking the link does (same click-tracking, same redirect)."
    )
    @GetMapping(value = "/r/{code}/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> getReferralQrCode(@PathVariable String code) {
        byte[] png = referralQrCodeService.generatePng(code);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                .body(png);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String resolveSessionId(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (AttributionSession.COOKIE_NAME.equals(cookie.getName()) && AttributionSession.isValid(cookie.getValue())) {
                    return cookie.getValue();
                }
            }
        }

        String sessionId = UUID.randomUUID().toString();
        ResponseCookie cookie = ResponseCookie.from(AttributionSession.COOKIE_NAME, sessionId)
                .httpOnly(true)
                .path("/")
                .maxAge(ATTRIBUTION_COOKIE_MAX_AGE)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return sessionId;
    }
}
