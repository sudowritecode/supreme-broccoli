package za.hungu.plinth.signal;

public record SignalKyberPrekeyResponse(
        long prekeyId,
        String publicKey,
        String signature,
        boolean lastResort
) {
}
