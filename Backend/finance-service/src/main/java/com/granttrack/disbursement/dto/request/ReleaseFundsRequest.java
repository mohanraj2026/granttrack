package com.granttrack.disbursement.dto.request;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Details captured by Finance when releasing funds for an approved milestone.
 * The amount is taken from the milestone itself and is not entered here.
 */
public record ReleaseFundsRequest(
        /** Beneficiary bank account / reference the funds are paid into. */
        @Size(max = 100) String receivingAccountRef,
        /** Bank/transaction reference for the payment (e.g. UTR / NEFT ref). */
        @Size(max = 100) String paymentReference,
        /** Date the funds were released; defaults to today when omitted. */
        LocalDate releaseDate
) {
}
