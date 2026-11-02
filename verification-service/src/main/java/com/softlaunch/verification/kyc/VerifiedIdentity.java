package com.softlaunch.verification.kyc;

import java.time.LocalDate;

/** What a KYC provider tells us after the user gave consent. subjectId = provider's unique id for the person. */
public record VerifiedIdentity(String subjectId, String fullName, LocalDate dateOfBirth) { }
