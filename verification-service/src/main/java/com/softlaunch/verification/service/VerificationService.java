package com.softlaunch.verification.service;

import com.softlaunch.verification.client.UserIdentity;
import com.softlaunch.verification.client.UserIdentityClient;
import com.softlaunch.verification.dto.StartResponse;
import com.softlaunch.verification.event.UserVerifiedEvent;
import com.softlaunch.verification.dto.VerificationResponse;
import com.softlaunch.verification.kyc.KycException;
import com.softlaunch.verification.kyc.KycProvider;
import com.softlaunch.verification.kyc.Pkce;
import com.softlaunch.verification.kyc.VerifiedIdentity;
import com.softlaunch.verification.model.Verification;
import com.softlaunch.verification.model.VerificationStatus;
import com.softlaunch.verification.repository.VerificationRepository;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;

@Service
public class VerificationService {

    private static final Logger log = LoggerFactory.getLogger(VerificationService.class);
    private static final int MIN_AGE = 18;

    private final VerificationRepository repository;
    private final KycProvider kycProvider;          // interface: mock or real, decided by profile
    private final KycStateStore stateStore;
    private final SubjectHasher hasher;
    private final UserIdentityClient userIdentityClient;
    private final VerificationWriter writer;

    public VerificationService(VerificationRepository repository, KycProvider kycProvider,
                               KycStateStore stateStore, SubjectHasher hasher,
                               UserIdentityClient userIdentityClient, VerificationWriter writer) {
        this.repository = repository;
        this.kycProvider = kycProvider;
        this.stateStore = stateStore;
        this.hasher = hasher;
        this.userIdentityClient = userIdentityClient;
        this.writer = writer;
    }

    public StartResponse start(UUID userId) {
        if (repository.existsByUserIdAndStatus(userId, VerificationStatus.VERIFIED)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You are already verified");
        }
        String state = Pkce.randomToken();
        String verifier = Pkce.randomToken();
        stateStore.save(state, userId, verifier);
        return new StartResponse(kycProvider.authorizationUrl(state, Pkce.challengeOf(verifier)));
    }

    /** No @Transactional on purpose: never hold a DB transaction open while calling an external provider. */
    public VerificationStatus complete(String state, String code, String error) {
        KycStateStore.PendingKyc pending = stateStore.consume(state)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired state"));
        UUID userId = pending.userId();   // comes from OUR Redis, never from the request

        if (error != null || code == null) {
            return fail(userId, "DigiLocker consent was cancelled");
        }

        VerifiedIdentity identity;
        try {
            identity = kycProvider.exchange(code, pending.codeVerifier());
        } catch (KycException e) {
            log.warn("KYC exchange failed for user {}: {}", userId, e.getMessage());
            return fail(userId, "DigiLocker verification failed, please try again");
        }

        if (Period.between(identity.dateOfBirth(), LocalDate.now()).getYears() < MIN_AGE) {
            return fail(userId, "You must be 18 or older to use SoftLaunch");
        }

        String subjectHash = hasher.hash(identity.subjectId());
        if (repository.existsBySubjectHashAndStatus(subjectHash, VerificationStatus.VERIFIED)) {
            return fail(userId, "This identity is already linked to another account");
        }

        // Compare with what the user signed up with
        UserIdentity profile;
        try {
            profile = userIdentityClient.identity(userId);
        } catch (FeignException e) {
            log.warn("Could not load identity of user {} from user-service: {}", userId, e.status());
            return fail(userId, "We couldn't check your profile right now, please try again");
        }
        if (!identity.dateOfBirth().equals(profile.dateOfBirth())) {
            return fail(userId, "Your date of birth doesn't match your profile. Contact support if this is wrong");
        }
        boolean nameMatched = NameMatcher.matches(profile.displayName(), identity.fullName());

        Verification verification = Verification.verified(userId, subjectHash, identity.fullName(),
                identity.dateOfBirth(), nameMatched);
        try {
            writer.saveVerified(verification, UserVerifiedEvent.of(userId));   // verification + outbox, one transaction
        } catch (DataIntegrityViolationException e) {
            // two callbacks raced: the partial unique index in the DB caught it
            return fail(userId, "This identity is already linked to another account");
        }
        log.info("User {} verified via DigiLocker (name matched: {})", userId, nameMatched);   // never log name/DOB/token
        return VerificationStatus.VERIFIED;
    }

    private VerificationStatus fail(UUID userId, String reason) {
        repository.save(Verification.failed(userId, reason));
        log.info("User {} verification FAILED: {}", userId, reason);
        return VerificationStatus.FAILED;
    }

    public VerificationResponse latest(UUID userId) {
        return repository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .map(VerificationResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No verification yet"));
    }
}
