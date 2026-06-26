package com.actpro.referral.reward;

import com.actpro.referral.common.ApiResponse;
import com.actpro.referral.reward.dto.RewardResponse;
import com.actpro.referral.reward.dto.UserRewardsResponse;
import com.actpro.referral.security.CompanyContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Reward", description = "Reward lookup APIs")
@RestController
@RequestMapping("/api/rewards")
@RequiredArgsConstructor
public class RewardController {

    private final RewardRepository rewardRepository;

    @Operation(
            summary = "Get rewards by user",
            description = "Get all rewards issued to a specific user",
            security = @SecurityRequirement(name = "ApiKey")
    )
    @GetMapping("/users/{externalUserId}")
    public ResponseEntity<ApiResponse<UserRewardsResponse>> getRewardsByUser(
            @PathVariable String externalUserId) {

        Long companyId = CompanyContext.getCurrentCompany().getId();

        List<Reward> rewards = rewardRepository.findByCompanyIdAndUserExternalUserId(companyId, externalUserId);

        List<RewardResponse> rewardResponses = rewards.stream()
                .map(reward -> new RewardResponse(
                        reward.getId(),
                        reward.getCouponCode(),
                        reward.getRewardType(),
                        reward.getRewardValue(),
                        reward.getStatus()
                ))
                .collect(Collectors.toList());

        UserRewardsResponse response = new UserRewardsResponse(externalUserId, rewardResponses);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
