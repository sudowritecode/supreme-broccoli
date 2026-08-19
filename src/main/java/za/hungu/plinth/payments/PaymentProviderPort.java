package za.hungu.plinth.payments;

import java.util.UUID;

public interface PaymentProviderPort {
    String createSandboxReference(UUID paymentIntentId, long amountMinor, String currency);
}
