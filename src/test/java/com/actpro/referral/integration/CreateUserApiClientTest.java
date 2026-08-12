package com.actpro.referral.integration;

import com.actpro.referral.company.CompanyIntegration;
import com.actpro.referral.integration.dto.CreateUserApiCallResult;
import com.actpro.referral.integration.dto.CreateUserApiRequestPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateUserApiClientTest {

    private final CredentialEncryptionService credentialEncryptionService = new CredentialEncryptionService("test-key");
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CreateUserApiClient client = new CreateUserApiClient(credentialEncryptionService, objectMapper);

    private CompanyIntegration integration;

    @BeforeEach
    void setUp() {
        integration = new CompanyIntegration();
        integration.setRequestTimeoutMs(1000);
        integration.setAuthType(IntegrationAuthType.NONE);
    }

    @Test
    void shouldReturnConnectionErrorForUnreachableHost() throws IOException {
        // An unused local port should fail fast with a connection-refused error.
        int freePort = findFreePort();
        integration.setApiBaseUrl("http://127.0.0.1:" + freePort + "/create-user");

        CreateUserApiCallResult result = client.call(integration, new CreateUserApiRequestPayload("req_1", "Jane", "jane@example.com", "SUMMER", "ABC123"));

        assertFalse(result.ioSuccess());
        assertNotNull(result.ioFailureCategory());
        assertTrue(result.ioFailureCategory() == FailureCategory.CONNECTION_ERROR || result.ioFailureCategory() == FailureCategory.TIMEOUT);
    }

    @Test
    void shouldBuildApiKeyHeaderFromDecryptedCredentials() {
        integration.setAuthType(IntegrationAuthType.API_KEY);
        integration.setEncryptedCredentials(credentialEncryptionService.encrypt("{\"headerName\":\"X-Api-Key\",\"headerValue\":\"secret-value\"}"));
        integration.setApiBaseUrl("http://127.0.0.1:1/unused");

        // We only assert this doesn't blow up building the request (header construction happens
        // before the network call); the network failure itself is covered by the test above.
        CreateUserApiCallResult result = client.call(integration, new CreateUserApiRequestPayload("req_2", "Jane", "jane@example.com", "SUMMER", "ABC123"));

        assertFalse(result.ioSuccess());
    }

    @Test
    void shouldReturnTimeoutForAConnectionThatAcceptsButNeverResponds() throws Exception {
        // Phase 9 hardening: shouldReturnConnectionErrorForUnreachableHost above only proves the
        // connect-time failure path; this proves the separate read-timeout path (a server that
        // accepts the TCP connection but never writes a response) is classified as TIMEOUT, not
        // lumped in with CONNECTION_ERROR - the distinction ApiSubmissionDispatchService's own
        // TRANSIENT_CATEGORIES/backoff logic doesn't actually depend on, but CreateUserApiClient's
        // own classifyIoFailure does, and it was previously untested.
        integration.setRequestTimeoutMs(300);
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            serverSocket.setReuseAddress(true);
            integration.setApiBaseUrl("http://127.0.0.1:" + serverSocket.getLocalPort() + "/create-user");

            ExecutorService acceptor = Executors.newSingleThreadExecutor();
            try {
                acceptor.submit(() -> {
                    try (Socket ignored = serverSocket.accept()) {
                        // Accept the connection, then just hold it open - deliberately never read
                        // the request or write a response, forcing the client's read timeout.
                        Thread.sleep(5000);
                    } catch (Exception ignoredException) {
                        // Test teardown closing the server socket races this thread - fine either way.
                    }
                    return null;
                });

                CreateUserApiCallResult result = client.call(
                        integration, new CreateUserApiRequestPayload("req_3", "Jane", "jane@example.com", "SUMMER", "ABC123"));

                assertFalse(result.ioSuccess());
                assertEquals(FailureCategory.TIMEOUT, result.ioFailureCategory());
            } finally {
                acceptor.shutdownNow();
            }
        }
    }

    private int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }
}
