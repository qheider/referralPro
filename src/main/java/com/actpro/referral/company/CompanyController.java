package com.actpro.referral.company;

import com.actpro.referral.common.ApiResponse;
import com.actpro.referral.company.dto.RegisterCompanyRequest;
import com.actpro.referral.company.dto.RegisterCompanyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Company", description = "Company management APIs")
@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @Operation(summary = "Register a new company", description = "Register a new company and receive an API key for integration")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterCompanyResponse>> registerCompany(
            @Valid @RequestBody RegisterCompanyRequest request) {
        RegisterCompanyResponse response = companyService.registerCompany(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Company registered successfully", response));
    }
}
