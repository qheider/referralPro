package com.actpro.referral.auth;

import com.actpro.referral.auth.dto.AcceptInvitationResponse;
import com.actpro.referral.auth.dto.IssuedInvitationResponse;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountInvitationServiceTest {

    @Mock
    private AccountInvitationRepository accountInvitationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AccountInvitationService accountInvitationService;

    private DashboardUser user;
    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(5L);

        user = new DashboardUser();
        user.setId(42L);
        user.setCompany(company);
        user.setUsername("ambassador@example.com");
        user.setRole(UserRole.AMBASSADOR);
        user.setStatus(UserStatus.PENDING);
    }

    @Test
    void shouldIssueInvitationWithHashedTokenAndRawTokenReturnedOnce() {
        when(accountInvitationRepository.findActiveByDashboardUserId(42L)).thenReturn(List.of());
        when(accountInvitationRepository.save(any(AccountInvitation.class))).thenAnswer(invocation -> {
            AccountInvitation invitation = invocation.getArgument(0);
            invitation.setId(1L);
            return invitation;
        });

        IssuedInvitationResponse response = accountInvitationService.issueInvitation(user, InvitationPurpose.AMBASSADOR_ONBOARDING);

        assertEquals(1L, response.id());
        assertNotNull(response.token());
        assertTrue(response.expiresAt().isAfter(LocalDateTime.now()));

        ArgumentCaptor<AccountInvitation> captor = ArgumentCaptor.forClass(AccountInvitation.class);
        verify(accountInvitationRepository).save(captor.capture());
        AccountInvitation saved = captor.getValue();
        assertEquals(user, saved.getDashboardUser());
        assertEquals(company, saved.getCompany());
        assertEquals(InvitationPurpose.AMBASSADOR_ONBOARDING, saved.getPurpose());
        assertEquals(sha256(response.token()), saved.getTokenHash());
        assertFalse(saved.getTokenHash().equals(response.token()), "raw token must never be stored");
    }

    @Test
    void shouldRevokePreviousOutstandingInvitationsWhenReissuing() {
        AccountInvitation previous = new AccountInvitation();
        previous.setId(9L);
        when(accountInvitationRepository.findActiveByDashboardUserId(42L)).thenReturn(List.of(previous));
        when(accountInvitationRepository.save(any(AccountInvitation.class))).thenAnswer(invocation -> {
            AccountInvitation invitation = invocation.getArgument(0);
            invitation.setId(2L);
            return invitation;
        });

        accountInvitationService.issueInvitation(user, InvitationPurpose.AMBASSADOR_ONBOARDING);

        assertNotNull(previous.getRevokedAt());
    }

    @Test
    void shouldAcceptUsableInvitationAndActivateUser() {
        AccountInvitation invitation = usableInvitation();
        String rawToken = "raw-token-value";
        when(accountInvitationRepository.findByTokenHash(sha256(rawToken))).thenReturn(Optional.of(invitation));
        when(passwordEncoder.encode("newPassword123")).thenReturn("encoded-hash");

        AcceptInvitationResponse response = accountInvitationService.acceptInvitation(rawToken, "newPassword123");

        assertEquals(42L, response.userId());
        assertEquals("ambassador@example.com", response.username());
        assertEquals("AMBASSADOR", response.role());
        assertEquals("encoded-hash", user.getPassword());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertNotNull(invitation.getAcceptedAt());
    }

    @Test
    void shouldRejectUnknownToken() {
        when(accountInvitationRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class,
                () -> accountInvitationService.acceptInvitation("unknown-token", "newPassword123"));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void shouldRejectExpiredInvitation() {
        AccountInvitation invitation = usableInvitation();
        invitation.setExpiresAt(LocalDateTime.now().minusDays(1));
        when(accountInvitationRepository.findByTokenHash(any())).thenReturn(Optional.of(invitation));

        assertThrows(BadRequestException.class,
                () -> accountInvitationService.acceptInvitation("raw-token-value", "newPassword123"));
        assertEquals(UserStatus.PENDING, user.getStatus(), "user must not be activated on a rejected invitation");
    }

    @Test
    void shouldRejectAlreadyAcceptedInvitation() {
        AccountInvitation invitation = usableInvitation();
        invitation.setAcceptedAt(LocalDateTime.now().minusHours(1));
        when(accountInvitationRepository.findByTokenHash(any())).thenReturn(Optional.of(invitation));

        assertThrows(BadRequestException.class,
                () -> accountInvitationService.acceptInvitation("raw-token-value", "newPassword123"));
    }

    @Test
    void shouldRejectRevokedInvitation() {
        AccountInvitation invitation = usableInvitation();
        invitation.setRevokedAt(LocalDateTime.now().minusHours(1));
        when(accountInvitationRepository.findByTokenHash(any())).thenReturn(Optional.of(invitation));

        assertThrows(BadRequestException.class,
                () -> accountInvitationService.acceptInvitation("raw-token-value", "newPassword123"));
    }

    @Test
    void shouldVerifyEmailAndActivateUserWithoutTouchingPassword() {
        user.setPassword("original-hash");
        AccountInvitation invitation = usableInvitation(InvitationPurpose.COMPANY_EMAIL_VERIFICATION);
        String rawToken = "raw-verification-token";
        when(accountInvitationRepository.findByTokenHash(sha256(rawToken))).thenReturn(Optional.of(invitation));

        DashboardUser result = accountInvitationService.verifyEmail(rawToken);

        assertEquals(user, result);
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertEquals("original-hash", user.getPassword(), "email verification must not reset the password");
        assertNotNull(invitation.getAcceptedAt());
    }

    @Test
    void shouldRejectEmailVerificationWithWrongPurposeToken() {
        AccountInvitation invitation = usableInvitation(InvitationPurpose.AMBASSADOR_ONBOARDING);
        String rawToken = "raw-onboarding-token";
        when(accountInvitationRepository.findByTokenHash(sha256(rawToken))).thenReturn(Optional.of(invitation));

        assertThrows(BadRequestException.class, () -> accountInvitationService.verifyEmail(rawToken));
        assertEquals(UserStatus.PENDING, user.getStatus());
    }

    @Test
    void shouldRejectExpiredEmailVerificationToken() {
        AccountInvitation invitation = usableInvitation(InvitationPurpose.COMPANY_EMAIL_VERIFICATION);
        invitation.setExpiresAt(LocalDateTime.now().minusDays(1));
        String rawToken = "raw-verification-token";
        when(accountInvitationRepository.findByTokenHash(sha256(rawToken))).thenReturn(Optional.of(invitation));

        assertThrows(BadRequestException.class, () -> accountInvitationService.verifyEmail(rawToken));
        assertEquals(UserStatus.PENDING, user.getStatus());
    }

    private AccountInvitation usableInvitation() {
        return usableInvitation(InvitationPurpose.AMBASSADOR_ONBOARDING);
    }

    private AccountInvitation usableInvitation(InvitationPurpose purpose) {
        AccountInvitation invitation = new AccountInvitation();
        invitation.setId(7L);
        invitation.setDashboardUser(user);
        invitation.setCompany(company);
        invitation.setPurpose(purpose);
        invitation.setExpiresAt(LocalDateTime.now().plusDays(7));
        return invitation;
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
