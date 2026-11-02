package com.softlaunch.verification.kyc;

import com.softlaunch.verification.config.DigiLockerProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Local/dev stand-in for DigiLocker. Codes: MOCK_FAIL = provider error, MOCK_UNDERAGE = 16 years old,
 * anything else = verified adult whose identity is "mock-" + code (so MOCK_OK twice = same person).
 */
@Component
@Profile("!prod")
public class MockKycProvider implements KycProvider {

    private final DigiLockerProperties props;

    public MockKycProvider(DigiLockerProperties props) { this.props = props; }

    @Override
    public String authorizationUrl(String state, String codeChallenge) {
        // Pretend the user already tapped "Allow" and jump straight to our callback
        return props.redirectUri() + "?code=MOCK_OK&state=" + state;
    }

    @Override
    public VerifiedIdentity exchange(String code, String codeVerifier) {
        return switch (code) {
            case "MOCK_FAIL" -> throw new KycException("Mock: DigiLocker rejected the code");
            case "MOCK_UNDERAGE" -> new VerifiedIdentity("mock-minor", "Mock Minor", LocalDate.now().minusYears(16));
            default -> new VerifiedIdentity("mock-" + code, "Mock User", LocalDate.of(2000, 5, 14));
        };
    }
}
