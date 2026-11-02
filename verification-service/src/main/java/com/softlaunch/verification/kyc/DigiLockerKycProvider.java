package com.softlaunch.verification.kyc;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.softlaunch.verification.config.DigiLockerProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** Real DigiLocker (OAuth 2.0 authorization code + PKCE). Active only with SPRING_PROFILES_ACTIVE=prod. */
@Component
@Profile("prod")
public class DigiLockerKycProvider implements KycProvider {

    private static final DateTimeFormatter DOB = DateTimeFormatter.ofPattern("ddMMyyyy");

    private final DigiLockerProperties props;
    private final RestClient restClient;

    public DigiLockerKycProvider(DigiLockerProperties props) {
        this.props = props;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));   // never wait forever on a government API
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public String authorizationUrl(String state, String codeChallenge) {
        return UriComponentsBuilder.fromUriString(props.authorizeUrl())
                .queryParam("response_type", "code")
                .queryParam("client_id", props.clientId())
                .queryParam("redirect_uri", props.redirectUri())
                .queryParam("state", state)
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "S256")
                .encode()
                .toUriString();
    }

    @Override
    public VerifiedIdentity exchange(String code, String codeVerifier) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("client_id", props.clientId());
        form.add("client_secret", props.clientSecret());
        form.add("redirect_uri", props.redirectUri());
        form.add("code_verifier", codeVerifier);
        try {
            TokenResponse token = restClient.post()
                    .uri(props.tokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);
            if (token == null || token.digilockerid() == null || token.dob() == null) {
                throw new KycException("DigiLocker response missing identity fields");
            }
            return new VerifiedIdentity(token.digilockerid(), token.name(), LocalDate.parse(token.dob(), DOB));
        } catch (RestClientException | DateTimeParseException e) {
            throw new KycException("DigiLocker token exchange failed", e);
        }
    }

    /** DigiLocker's token response includes the verified user's details (dob is ddMMyyyy). */
    record TokenResponse(@JsonProperty("access_token") String accessToken,
                         String digilockerid, String name, String dob) { }
}
