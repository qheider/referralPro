package com.actpro.referral.integration;

import com.actpro.referral.company.CompanyIntegration;
import com.actpro.referral.integration.dto.CreateUserApiCallResult;
import com.actpro.referral.integration.dto.CreateUserApiRequestPayload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.SocketTimeoutException;

/**
 * The app's first (and so far only) outgoing HTTP client - Spring 6's {@link RestClient}, already
 * available via {@code spring-boot-starter-web}, no new dependency needed. Registers a no-op
 * status handler so 4xx/5xx responses come back as normal {@link ResponseEntity}s instead of
 * throwing - all failure categorization happens in {@link ApiSubmissionDispatchService}/
 * {@link CompanyIntegrationService}, not here. Only genuine I/O failures (timeout, connection
 * refused, DNS) are caught and mapped to a {@link FailureCategory}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CreateUserApiClient {

    private final CredentialEncryptionService credentialEncryptionService;
    private final ObjectMapper objectMapper;

    /** Performs the real Create User call (POST, JSON body). */
    public CreateUserApiCallResult call(CompanyIntegration integration, CreateUserApiRequestPayload payload) {
        return execute(integration, HttpMethod.POST, requestSpec -> requestSpec
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload));
    }

    /**
     * Best-effort connectivity+auth probe for the Test Connection action. There's no known
     * real health-check contract for an arbitrary company's Create User endpoint, so this just
     * issues a GET against the configured base URL with the configured auth applied - the caller
     * (CompanyIntegrationService) interprets any response other than 401/403 as "reachable".
     */
    public CreateUserApiCallResult testConnection(CompanyIntegration integration) {
        return execute(integration, HttpMethod.GET, requestSpec -> requestSpec);
    }

    private CreateUserApiCallResult execute(
            CompanyIntegration integration, HttpMethod method, RequestCustomizer customizer) {
        RestClient restClient = buildClient(integration);
        try {
            RestClient.RequestBodySpec requestSpec = restClient.method(method)
                    .uri(integration.getApiBaseUrl())
                    .headers(headers -> applyAuth(headers, integration));
            ResponseEntity<String> response = customizer.customize(requestSpec)
                    .retrieve()
                    .onStatus(status -> true, (req, res) -> { /* no-op: never throw on HTTP error status */ })
                    .toEntity(String.class);
            return toResult(response);
        } catch (RestClientException e) {
            // Broadened from ResourceAccessException (Phase 9 hardening, found via a real
            // read-timeout test, not a mock): a timeout that occurs while RestClient is reading
            // the status line/headers (e.g. inside the onStatus predicate above, which touches
            // the response before body conversion) surfaces as a generic RestClientException
            // wrapping SocketTimeoutException, not a ResourceAccessException - catching only the
            // latter let it escape uncaught up through ApiSubmissionDispatchService.dispatchOne,
            // which has no catch of its own (it trusts this method to always return a result,
            // never throw) - leaving the submission (and the rest of its claimed batch, since the
            // scheduled loop has no per-item catch either) permanently stuck in PROCESSING with
            // no further retry. Catching the RestClientException supertype restores that contract
            // for every I/O failure shape, not just the one the original narrower catch handled.
            FailureCategory category = classifyIoFailure(e);
            log.warn("Outgoing integration call to {} failed ({}): {}", integration.getApiBaseUrl(), category, e.getMessage());
            return CreateUserApiCallResult.ioFailure(category, sanitize(e.getMessage()));
        }
    }

    private RestClient buildClient(CompanyIntegration integration) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(integration.getRequestTimeoutMs());
        requestFactory.setReadTimeout(integration.getRequestTimeoutMs());
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    private void applyAuth(HttpHeaders headers, CompanyIntegration integration) {
        if (integration.getAuthType() == IntegrationAuthType.NONE || integration.getEncryptedCredentials() == null) {
            return;
        }
        JsonNode credentials = readCredentials(integration);
        switch (integration.getAuthType()) {
            case API_KEY -> headers.set(credentials.path("headerName").asText(), credentials.path("headerValue").asText());
            case BEARER_TOKEN -> headers.setBearerAuth(credentials.path("token").asText());
            case BASIC -> headers.setBasicAuth(credentials.path("username").asText(), credentials.path("password").asText());
            case NONE -> { /* unreachable, guarded above */ }
        }
    }

    private JsonNode readCredentials(CompanyIntegration integration) {
        String decrypted = credentialEncryptionService.decrypt(integration.getEncryptedCredentials());
        try {
            return objectMapper.readTree(decrypted);
        } catch (Exception e) {
            throw new IllegalStateException("Stored integration credentials are not valid JSON", e);
        }
    }

    private CreateUserApiCallResult toResult(ResponseEntity<String> response) {
        String body = response.getBody();
        String customerReference = extractField(body, "customerId", "customerReference", "userId", "id");
        String transactionReference = extractField(body, "transactionId", "transactionReference", "referenceId");
        return CreateUserApiCallResult.httpResponse(response.getStatusCode().value(), body, customerReference, transactionReference);
    }

    // Best-effort extraction since the target API's response shape is generic/unknown - if the
    // body isn't JSON, or none of the candidate keys are present, references stay null.
    private String extractField(String body, String... candidateKeys) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(body);
            for (String key : candidateKeys) {
                if (node.hasNonNull(key)) {
                    return node.get(key).asText();
                }
            }
        } catch (Exception e) {
            // Not JSON, or unexpected shape - fine, references simply stay unset.
        }
        return null;
    }

    private FailureCategory classifyIoFailure(RestClientException e) {
        // Walk the full cause chain, not just e.getCause(): the wrapping depth differs depending
        // on which layer of RestClient the failure surfaces from (see the catch site's comment).
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof SocketTimeoutException) {
                return FailureCategory.TIMEOUT;
            }
            cause = cause.getCause();
        }
        return FailureCategory.CONNECTION_ERROR;
    }

    private String sanitize(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 2000 ? message.substring(0, 2000) : message;
    }

    @FunctionalInterface
    private interface RequestCustomizer {
        RestClient.RequestBodySpec customize(RestClient.RequestBodySpec requestSpec);
    }
}
