package com.actpro.referral.company;

import com.actpro.referral.common.ApiResponse;
import com.actpro.referral.company.dto.ApiKeySummaryResponse;
import com.actpro.referral.company.dto.IssuedApiKeyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "API Keys", description = "Company API key lifecycle management")
@RestController
@RequestMapping("/api/admin/api-keys")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COMPANY_ADMIN')")
public class ApiKeyAdminController {

    private final CompanyApiKeyService companyApiKeyService;

    @Operation(summary = "List this company's API keys", description = "Never returns the raw secret - only a short preview")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ApiKeySummaryResponse>>> listKeys() {
        return ResponseEntity.ok(ApiResponse.success(companyApiKeyService.listKeys()));
    }

    @Operation(summary = "Rotate the active API key", description = "Revokes all currently active keys and issues a new one. The raw secret is shown once, in this response only.")
    @PostMapping("/rotate")
    public ResponseEntity<ApiResponse<IssuedApiKeyResponse>> rotateKey() {
        return ResponseEntity.ok(ApiResponse.success("API key rotated successfully", companyApiKeyService.rotateKey()));
    }

    @Operation(summary = "Revoke an API key")
    @DeleteMapping("/{keyRecordId}")
    public ResponseEntity<ApiResponse<Void>> revokeKey(@PathVariable Long keyRecordId) {
        companyApiKeyService.revokeKey(keyRecordId);
        return ResponseEntity.ok(ApiResponse.success("API key revoked successfully", null));
    }
}
