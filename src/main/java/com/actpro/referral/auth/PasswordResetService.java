package com.actpro.referral.auth;

import com.actpro.referral.common.EmailService;
import com.actpro.referral.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;

/**
 * "Forgot password" flow for dashboard users (company admins and ambassadors share the same
 * login, so this is one unified flow rather than a per-role one). Kept as its own token table
 * (password_reset_tokens) rather than reusing AccountInvitation - see PasswordResetToken /
 * V34__create_password_reset_tokens.sql for why: a much shorter expiry, and reissuing must not
 * revoke an unrelated pending account invitation for the same user.
 *
 * requestReset never reveals whether an email is registered - it always completes successfully
 * from the caller's point of view (PasswordResetController returns the same generic message
 * either way), so an attacker can't use this endpoint to enumerate accounts.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final String TOKEN_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final int TOKEN_LENGTH = 32;
    private static final int EXPIRY_MINUTES = 30;
    // Skip issuing (and emailing) a new token if a still-usable one was created this recently -
    // absorbs accidental double-submits/refresh-spam without needing dedicated rate-limit infra,
    // which doesn't exist anywhere else in this codebase yet.
    private static final int REISSUE_COOLDOWN_SECONDS = 60;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final DashboardUserRepository dashboardUserRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Transactional
    public void requestReset(String email) {
        DashboardUser user = dashboardUserRepository.findByUsername(email).orElse(null);
        if (user == null || user.getStatus() != UserStatus.ACTIVE) {
            log.info("Password reset requested for an email with no active account - no-op");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        var activeTokens = passwordResetTokenRepository.findActiveByDashboardUserId(user.getId());
        boolean recentlyIssued = activeTokens.stream()
                .anyMatch(t -> t.getCreatedAt().isAfter(now.minusSeconds(REISSUE_COOLDOWN_SECONDS)));
        if (recentlyIssued) {
            log.info("Password reset already issued recently for user {} - skipping reissue", user.getId());
            return;
        }

        for (PasswordResetToken previous : activeTokens) {
            previous.setRevokedAt(now);
        }

        String rawToken = randomString(TOKEN_ALPHABET, TOKEN_LENGTH);
        PasswordResetToken token = new PasswordResetToken();
        token.setDashboardUser(user);
        token.setCompany(user.getCompany());
        token.setTokenHash(hash(rawToken));
        token.setExpiresAt(now.plusMinutes(EXPIRY_MINUTES));
        passwordResetTokenRepository.save(token);

        log.info("Issued password reset token for user {}, expires at {}", user.getId(), token.getExpiresAt());

        // Don't let a mail-server hiccup roll back the token we just issued - same pattern as
        // AmbassadorAdminService.provisionAmbassadorAccount's invitation email.
        try {
            emailService.sendPasswordResetEmail(user.getUsername(), rawToken);
        } catch (Exception e) {
            log.warn("Failed to send password reset email, but the reset token was issued.", e);
        }
    }

    @Transactional
    public ResetOutcome resetPassword(String rawToken, String newPassword) {
        if (rawToken != null) {
            rawToken = rawToken.trim();
        }

        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset link"));

        if (!token.isUsable()) {
            throw new BadRequestException("Invalid or expired reset link");
        }

        DashboardUser user = token.getDashboardUser();
        user.setPassword(passwordEncoder.encode(newPassword));

        LocalDateTime now = LocalDateTime.now();
        token.setUsedAt(now);

        // Defense in depth: invalidate any other outstanding reset tokens for this user so an
        // older, still-valid link can't be used after the password has already changed.
        for (PasswordResetToken other : passwordResetTokenRepository.findActiveByDashboardUserId(user.getId())) {
            other.setRevokedAt(now);
        }

        try {
            emailService.sendPasswordChangedConfirmationEmail(user.getUsername());
        } catch (Exception e) {
            log.warn("Failed to send password-changed confirmation email, but the password was reset.", e);
        }

        return new ResetOutcome(user.getId(), user.getUsername());
    }

    public record ResetOutcome(Long userId, String username) {
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private String randomString(String alphabet, int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(alphabet.charAt(SECURE_RANDOM.nextInt(alphabet.length())));
        }
        return builder.toString();
    }
}
