package com.actpro.referral.conversion;

import com.actpro.referral.common.ApiResponse;
import com.actpro.referral.conversion.dto.ConversionRequest;
import com.actpro.referral.conversion.dto.ConversionResponse;
import com.actpro.referral.reward.dto.RewardResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Conversion", description = "Conversion tracking and reward issuance APIs")
@RestController
@RequestMapping("/api/conversions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COMPANY')")
public class ConversionController {

    private final ConversionService conversionService;

    @Operation(
            summary = "Complete conversion",
            description = "Record a successful conversion and issue rewards to both referrer and referee",
            security = @SecurityRequirement(name = "ApiKey")
    )
    @PostMapping
    public ResponseEntity<ApiResponse<ConversionResponse>> completeConversion(
            @Valid @RequestBody ConversionRequest request) {

        ConversionService.ConversionWithRewards result = conversionService.completeConversion(request);
        Conversion conversion = result.getConversion();
        RewardResult rewardResult = result.getRewardResult();

        // Build response with reward information
        ConversionResponse response = new ConversionResponse();
        response.setConversionId(conversion.getId());
        response.setStatus(conversion.getStatus());

        // Referrer-side reward: from the legacy Reward when this was a PlatformUser-referrer
        // conversion, or from the AmbassadorReward (no couponCode - it's a payable balance visible
        // via the ambassador portal's earnings screens, not a redeemable coupon) when ambassador-driven.
        if (rewardResult.getReferrerReward() != null) {
            response.setReferrerReward(new ConversionResponse.RewardInfo(
                    rewardResult.getReferrerReward().getCouponCode(),
                    rewardResult.getReferrerReward().getRewardValue().toString()
            ));
        } else if (rewardResult.getAmbassadorReward() != null) {
            response.setReferrerReward(new ConversionResponse.RewardInfo(
                    null,
                    rewardResult.getAmbassadorReward().getRewardValue().toString()
            ));
        }

        // Add referee reward info
        ConversionResponse.RewardInfo refereeRewardInfo = new ConversionResponse.RewardInfo(
                rewardResult.getRefereeReward().getCouponCode(),
                rewardResult.getRefereeReward().getRewardValue().toString()
        );
        response.setRefereeReward(refereeRewardInfo);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Conversion completed and rewards issued successfully", response));
    }
}
