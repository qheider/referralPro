package com.actpro.referral.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.enabled:true}")
    private boolean emailEnabled;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    /**
     * Send email verification link to newly registered company admin
     */
    public void sendVerificationEmail(String toEmail, String verificationToken) {
        if (!emailEnabled) {
            log.info("Email sending is disabled - skipping verification email to: {}", toEmail);
            return;
        }

        try {
            String verificationLink = frontendUrl + "/verify-email";
            String subject = "Verify Your ReferralPro Account";
            String body = "Welcome to ReferralPro!\n\n" +
                    "Please verify your email address by clicking the link below:\n" +
                    verificationLink + "?token=" + verificationToken + "\n\n" +
                    "This link will expire in 24 hours.\n\n" +
                    "If you did not register for this account, please ignore this email.\n\n" +
                    "Best regards,\n" +
                    "The ReferralPro Team";

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.info("Verification email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    /**
     * Send ambassador invitation email
     */
    public void sendAmbassadorInvitationEmail(String toEmail, String invitationToken, String ambassadorName) {
        if (!emailEnabled) {
            log.info("Email sending is disabled - skipping ambassador invitation email to: {}", toEmail);
            return;
        }

        try {
            String invitationLink = frontendUrl + "/accept-invitation";
            String subject = "You're Invited to Join as an Ambassador";
            String body = "Hello " + ambassadorName + "!\n\n" +
                    "You've been invited to become an ambassador.\n\n" +
                    "Click the link below to accept the invitation and set up your account:\n" +
                    invitationLink + "?token=" + invitationToken + "\n\n" +
                    "This link will expire in 7 days.\n\n" +
                    "Best regards,\n" +
                    "The ReferralPro Team";

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.info("Ambassador invitation email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send ambassador invitation email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send ambassador invitation email", e);
        }
    }

    /**
     * Send generic email
     */
    public void sendEmail(String toEmail, String subject, String body) {
        if (!emailEnabled) {
            log.info("Email sending is disabled - skipping email to: {}", toEmail);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.info("Email sent successfully to: {} with subject: {}", toEmail, subject);
        } catch (Exception e) {
            log.error("Failed to send email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
