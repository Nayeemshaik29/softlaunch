package com.softlaunch.verification.kyc;

/** The "socket shape". The service only knows this interface; the Spring profile picks the real or mock adapter. */
public interface KycProvider {

    String authorizationUrl(String state, String codeChallenge);

    /** Swap the one-time code for the verified identity. Throws KycException on any failure. */
    VerifiedIdentity exchange(String code, String codeVerifier);
}
