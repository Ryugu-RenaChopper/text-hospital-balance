package io.github.ryugurenachopper.texthospital.balance.governance;

public class RandomResamplingException extends RuntimeException {
    private final String reason;

    public RandomResamplingException(String reason, String message) {
        super(message);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
