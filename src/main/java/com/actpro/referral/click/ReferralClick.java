package com.actpro.referral.click;

import com.actpro.referral.referral.Referral;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "referral_clicks")
@Getter
@Setter
@NoArgsConstructor
public class ReferralClick {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referral_id", nullable = false)
    private Referral referral;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "clicked_at", nullable = false)
    private LocalDateTime clickedAt = LocalDateTime.now();
}
