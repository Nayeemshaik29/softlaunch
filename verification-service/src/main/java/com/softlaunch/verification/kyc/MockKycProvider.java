package com.softlaunch.verification.kyc;

import com.softlaunch.verification.config.DigiLockerProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Local/dev stand-in for DigiLocker. Codes:
 *   MOCK_FAIL                 provider error
 *   MOCK_UNDERAGE             a 16-year-old
 *   MOCK_OK                   adult "User Mock", born 2000-05-14
 *   MOCK_OK_PRIYA_19990820    adult "Priya Mock", born 1999-08-20 (name tag + yyyyMMdd are optional)
 * The identity (who the person is) = "mock-" + code without the date, so the same tag = the same person.
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
            default -> parse(code);
        };
    }

    private static VerifiedIdentity parse(String code) {
        String[] parts = code.split("_");          // MOCK, OK, [TAG], [yyyyMMdd]
        String tag = parts.length > 2 ? parts[2] : "USER";
        LocalDate dob = parts.length > 3
                ? LocalDate.parse(parts[3], DateTimeFormatter.BASIC_ISO_DATE)
                : LocalDate.of(2000, 5, 14);
        String subject = "mock-" + code.replaceFirst("_\\d{8}$", "");
        String name = tag.charAt(0) + tag.substring(1).toLowerCase() + " Mock";
        return new VerifiedIdentity(subject, name, dob);
    }
}
