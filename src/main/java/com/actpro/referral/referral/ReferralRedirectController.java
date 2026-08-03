package com.actpro.referral.referral;

import com.actpro.referral.click.ReferralClickService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.view.RedirectView;

import java.time.Duration;
import java.util.UUID;
import java.util.regex.Pattern;

@Tag(name = "Referral Redirect", description = "Public referral link redirect")
@Controller
@RequiredArgsConstructor
public class ReferralRedirectController {

    private static final String ATTRIBUTION_COOKIE_NAME = "rp_attr_session";
    private static final Duration ATTRIBUTION_COOKIE_MAX_AGE = Duration.ofDays(30);
    // Matches the values this controller itself generates (UUID) - anything else from the
    // client is untrusted and is not reused as-is (also enforces the ReferralClick.sessionId
    // VARCHAR(100) column limit so an oversized cookie can't fail the insert).
    private static final Pattern VALID_SESSION_ID = Pattern.compile("^[A-Za-z0-9-]{1,100}$");

    private final ReferralClickService referralClickService;

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
                if (ATTRIBUTION_COOKIE_NAME.equals(cookie.getName()) && VALID_SESSION_ID.matcher(cookie.getValue()).matches()) {
                    return cookie.getValue();
                }
            }
        }

        String sessionId = UUID.randomUUID().toString();
        ResponseCookie cookie = ResponseCookie.from(ATTRIBUTION_COOKIE_NAME, sessionId)
                .httpOnly(true)
                .path("/")
                .maxAge(ATTRIBUTION_COOKIE_MAX_AGE)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return sessionId;
    }
}
