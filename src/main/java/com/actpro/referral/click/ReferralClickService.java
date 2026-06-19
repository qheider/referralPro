package com.actpro.referral.click;

import com.actpro.referral.referral.Referral;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReferralClickService {

    private final ReferralClickRepository referralClickRepository;

    @Transactional
    public void recordClick(Referral referral, String ipAddress, String userAgent) {
        log.info("Recording click for referral code: {} from IP: {}", referral.getReferralCode(), ipAddress);

        ReferralClick click = new ReferralClick();
        click.setReferral(referral);
        click.setIpAddress(ipAddress);
        click.setUserAgent(userAgent);

        referralClickRepository.save(click);
    }
}
