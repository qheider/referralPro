package com.actpro.referral.auth;

import com.actpro.referral.common.EmailService;
import com.actpro.referral.common.exception.BadRequestException;
import com.actpro.referral.company.Company;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private DashboardUserRepository dashboardUserRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private DashboardUser user;
    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(5L);

        user = new DashboardUser();
        user.setId(42L);
        user.setCompany(company);
        user.setUsername("admin@example.com");
        user.setRole(UserRole.COMPANY_ADMIN);
        user.setStatus(UserStatus.ACTIVE);
        user.setPassword("original-hash");
    }

    @Test
    void shouldIssueTokenAndEmailLinkForActiveUser() {
        when(dashboardUserRepository.findByUsername("admin@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findActiveByDashboardUserId(42L)).thenReturn(List.of());

        passwordResetService.requestReset("admin@example.com");

        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(captor.capture());
        PasswordResetToken saved = captor.getValue();
        assertEquals(user, saved.getDashboardUser());
        assertEquals(company, saved.getCompany());
        assertTrue(saved.getExpiresAt().isAfter(LocalDateTime.now()));
        assertTrue(saved.getExpiresAt().isBefore(LocalDateTime.now().plusMinutes(31)));

        verify(emailService).sendPasswordResetEmail(eq("admin@example.com"), anyString());
    }

    @Test
    void shouldNotRevealWhetherEmailIsRegistered() {
        when(dashboardUserRepository.findByUsername("unknown@example.com")).thenReturn(Optional.empty());

        // Must not throw - the caller (controller) always returns the same generic message.
        passwordResetService.requestReset("unknown@example.com");

        verify(passwordResetTokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void shouldNoOpForInactiveUser() {
        user.setStatus(UserStatus.PENDING);
        when(dashboardUserRepository.findByUsername("admin@example.com")).thenReturn(Optional.of(user));

        passwordResetService.requestReset("admin@example.com");

        verify(passwordResetTokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void shouldRevokePreviousOutstandingTokensWhenReissuing() {
        PasswordResetToken previous = new PasswordResetToken();
        previous.setId(9L);
        previous.setCreatedAt(LocalDateTime.now().minusMinutes(10));

        when(dashboardUserRepository.findByUsername("admin@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findActiveByDashboardUserId(42L)).thenReturn(List.of(previous));

        passwordResetService.requestReset("admin@example.com");

        assertNotNull(previous.getRevokedAt());
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    void shouldSkipReissueWithinCooldownWindow() {
        PasswordResetToken recent = new PasswordResetToken();
        recent.setId(9L);
        recent.setCreatedAt(LocalDateTime.now().minusSeconds(5));

        when(dashboardUserRepository.findByUsername("admin@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findActiveByDashboardUserId(42L)).thenReturn(List.of(recent));

        passwordResetService.requestReset("admin@example.com");

        verify(passwordResetTokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
        assertNull(recent.getRevokedAt(), "a token within the cooldown window should not be revoked either");
    }

    @Test
    void shouldResetPasswordForUsableToken() {
        PasswordResetToken token = usableToken();
        String rawToken = "raw-reset-token";
        when(passwordResetTokenRepository.findByTokenHash(sha256(rawToken))).thenReturn(Optional.of(token));
        when(passwordResetTokenRepository.findActiveByDashboardUserId(42L)).thenReturn(List.of());
        when(passwordEncoder.encode("newPassword123")).thenReturn("encoded-hash");

        PasswordResetService.ResetOutcome outcome = passwordResetService.resetPassword(rawToken, "newPassword123");

        assertEquals(42L, outcome.userId());
        assertEquals("admin@example.com", outcome.username());
        assertEquals("encoded-hash", user.getPassword());
        assertNotNull(token.getUsedAt());
        verify(emailService).sendPasswordChangedConfirmationEmail("admin@example.com");
    }

    @Test
    void shouldRevokeSiblingTokensOnSuccessfulReset() {
        PasswordResetToken token = usableToken();
        PasswordResetToken sibling = new PasswordResetToken();
        sibling.setId(11L);

        when(passwordResetTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));
        when(passwordResetTokenRepository.findActiveByDashboardUserId(42L)).thenReturn(List.of(sibling));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-hash");

        passwordResetService.resetPassword("raw-reset-token", "newPassword123");

        assertNotNull(sibling.getRevokedAt());
    }

    @Test
    void shouldRejectUnknownToken() {
        when(passwordResetTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class,
                () -> passwordResetService.resetPassword("unknown-token", "newPassword123"));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void shouldRejectExpiredToken() {
        PasswordResetToken token = usableToken();
        token.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(passwordResetTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));

        assertThrows(BadRequestException.class,
                () -> passwordResetService.resetPassword("raw-reset-token", "newPassword123"));
        assertEquals("original-hash", user.getPassword(), "password must not change on a rejected token");
    }

    @Test
    void shouldRejectAlreadyUsedToken() {
        PasswordResetToken token = usableToken();
        token.setUsedAt(LocalDateTime.now().minusMinutes(1));
        when(passwordResetTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));

        assertThrows(BadRequestException.class,
                () -> passwordResetService.resetPassword("raw-reset-token", "newPassword123"));
    }

    @Test
    void shouldRejectRevokedToken() {
        PasswordResetToken token = usableToken();
        token.setRevokedAt(LocalDateTime.now().minusMinutes(1));
        when(passwordResetTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));

        assertThrows(BadRequestException.class,
                () -> passwordResetService.resetPassword("raw-reset-token", "newPassword123"));
    }

    private PasswordResetToken usableToken() {
        PasswordResetToken token = new PasswordResetToken();
        token.setId(7L);
        token.setDashboardUser(user);
        token.setCompany(company);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        return token;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
