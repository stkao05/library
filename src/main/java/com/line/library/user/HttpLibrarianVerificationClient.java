package com.line.library.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Profile("prod")
public class HttpLibrarianVerificationClient implements LibrarianVerificationClient {

    private final RestClient client;
    private final String authHeader;

    public HttpLibrarianVerificationClient(
            @Value("${app.verification.base-url:https://todo.com.tw}") String baseUrl,
            @Value("${app.verification.auth-header:todo}") String authHeader) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
        this.authHeader = authHeader;
    }

    @Override
    public boolean verifyEmail(String email) {
        try {
            var response = client
                    .get()
                    .uri(uriBuilder -> uriBuilder.queryParam("email", email).build())
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .retrieve()
                    .toBodilessEntity();
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception ex) {
            return false;
        }
    }
}

