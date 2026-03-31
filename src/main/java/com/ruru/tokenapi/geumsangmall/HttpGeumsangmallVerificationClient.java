package com.ruru.tokenapi.geumsangmall;

import com.ruru.tokenapi.config.TokenApiProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HttpGeumsangmallVerificationClient implements GeumsangmallVerificationClient {
    private static final String SECRET_HEADER = "X-Token-Exchange-Secret";

    private final RestClient restClient;
    private final TokenApiProperties properties;

    public HttpGeumsangmallVerificationClient(RestClient restClient, TokenApiProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public GeumsangmallVerificationResponse verify(GeumsangmallVerificationRequest request) {
        String url = properties.getGeumsangmall().getVerificationUrl();
        String secret = properties.getGeumsangmall().getVerificationSecret();
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("Geumsangmall verification URL is not configured");
        }
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("Geumsangmall verification secret is not configured");
        }
        try {
            GeumsangmallVerificationResponse response = restClient.post()
                .uri(url)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(SECRET_HEADER, secret)
                .body(request)
                .retrieve()
                .body(GeumsangmallVerificationResponse.class);
            if (response == null) {
                throw new IllegalStateException("Geumsangmall verification response is empty");
            }
            return response;
        } catch (RestClientException e) {
            throw new IllegalStateException("Failed to verify Geumsangmall session", e);
        }
    }
}
