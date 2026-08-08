package com.actpro.referral.auth;

import com.actpro.referral.auth.dto.AcceptInvitationResponse;
import com.actpro.referral.auth.dto.IssuedInvitationResponse;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountInvitationService {

    private static final String TOKEN_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final int TOKEN_LENGTH = 32;
    private static final int EXPIRY_DAYS = 7;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AccountInvitationRepository accountInvitationRepository;
    private final DashboardUserRepository dashboardUserRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Issues a fresh invitation token for the given user, revoking any invitation for that user
     * that is still outstanding (unaccepted and not already revoked) - a reissue supersedes
     * whatever was sent before rather than leaving multiple valid tokens alive at once.
     */
    @Transactional
    public IssuedInvitationResponse issueInvitation(DashboardUser user, InvitationPurpose purpose) {
        LocalDateTime now = LocalDateTime.now();
        for (AccountInvitation previous : accountInvitationRepository.findActiveByDashboardUserId(user.getId())) {
            previous.setRevokedAt(now);
        }

        String rawToken = randomString(TOKEN_ALPHABET, TOKEN_LENGTH);
        String tokenHash = hash(rawToken);
        
        log.info("Issuing invitation for user {}, purpose: {}", user.getId(), purpose);
        log.debug("Generated token (length {}): [{}]", rawToken.length(), rawToken);
        log.debug("Token hash: {}", tokenHash);

        AccountInvitation invitation = new AccountInvitation();
        invitation.setDashboardUser(user);
        invitation.setCompany(user.getCompany());
        invitation.setPurpose(purpose);
        invitation.setTokenHash(tokenHash);
        invitation.setExpiresAt(now.plusDays(EXPIRY_DAYS));
        invitation = accountInvitationRepository.save(invitation);
        
        log.info("Invitation created with ID: {}, expires at: {}", invitation.getId(), invitation.getExpiresAt());

        return new IssuedInvitationResponse(invitation.getId(), rawToken, invitation.getExpiresAt());
    }

    /**
     * Redeems a one-time invitation token: sets the user's real password, activates the account,
     * and marks the token spent. Purpose-specific follow-up (e.g. activating an AmbassadorProfile)
     * is the caller's responsibility - this service only owns account/credential setup.
     */
    @Transactional
    public AcceptInvitationResponse acceptInvitation(String rawToken, String newPassword) {
        // Trim whitespace that might have been copied accidentally
        if (rawToken != null) {
            rawToken = rawToken.trim();
        }
        
        AccountInvitation invitation = accountInvitationRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new BadRequestException("Invalid or expired invitation"));

        if (!invitation.isUsable()) {
            throw new BadRequestException("Invalid or expired invitation");
        }

        DashboardUser user = invitation.getDashboardUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setStatus(UserStatus.ACTIVE);
        invitation.setAcceptedAt(LocalDateTime.now());

        return new AcceptInvitationResponse(user.getId(), user.getUsername(), user.getRole().name());
    }

    /**
     * Redeems a COMPANY_EMAIL_VERIFICATION token: activates the account without touching its
     * password, unlike {@link #acceptInvitation}, since the admin already set a password at
     * registration and is only confirming ownership of the email address.
     */
    @Transactional
    public DashboardUser verifyEmail(String rawToken) {
        log.info("Verifying email with token (original length: {})", rawToken != null ? rawToken.length() : "null");
        
        // Trim whitespace that might have been copied accidentally
        if (rawToken != null) {
            String originalToken = rawToken;
            rawToken = rawToken.trim();
            if (!originalToken.equals(rawToken)) {
                log.warn("Token had surrounding whitespace - trimmed from {} to {} chars", 
                        originalToken.length(), rawToken.length());
            }
        }
        
        log.debug("Token after trim (length {}): [{}]", rawToken != null ? rawToken.length() : "null", rawToken);
        
        String tokenHash = hash(rawToken);
        log.debug("Computed token hash: {}", tokenHash);
        
        AccountInvitation invitation = accountInvitationRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> {
                    log.warn("Token hash not found in database: {}", tokenHash);
                    return new BadRequestException("Invalid or expired verification link");
                });

        log.info("Found invitation with ID: {}, purpose: {}, expires at: {}", 
                invitation.getId(), invitation.getPurpose(), invitation.getExpiresAt());
        
        if (invitation.getPurpose() != InvitationPurpose.COMPANY_EMAIL_VERIFICATION || !invitation.isUsable()) {
            log.warn("Invitation not usable - purpose: {}, isUsable: {}, acceptedAt: {}, revokedAt: {}, expiresAt: {}", 
                    invitation.getPurpose(), invitation.isUsable(), 
                    invitation.getAcceptedAt(), invitation.getRevokedAt(), invitation.getExpiresAt());
            throw new BadRequestException("Invalid or expired verification link");
        }

        DashboardUser user = invitation.getDashboardUser();
        user.setStatus(UserStatus.ACTIVE);
        invitation.setAcceptedAt(LocalDateTime.now());

        return user;
    }

    /**
     * Resends a verification email by issuing a new token for the given email address.
     * Only works for PENDING users (those who haven't verified their email yet).
     */
    @Transactional
    public IssuedInvitationResponse resendVerificationEmail(String email) {
        log.info("Resending verification email for: {}", email);
        
        DashboardUser user = dashboardUserRepository.findByUsername(email)
                .orElseThrow(() -> new BadRequestException("No account found with this email"));

        if (user.getStatus() != UserStatus.PENDING) {
            log.warn("Cannot resend verification for user {} with status: {}", email, user.getStatus());
            throw new BadRequestException("This account is already verified or cannot be verified");
        }

        // Issue a new invitation (this will revoke any existing ones)
        return issueInvitation(user, InvitationPurpose.COMPANY_EMAIL_VERIFICATION);
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
