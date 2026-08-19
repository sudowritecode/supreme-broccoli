package za.hungu.plinth.payments;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SandboxPaymentProvider implements PaymentProviderPort {
    @Override
    public String createSandboxReference(UUID paymentIntentId, long amountMinor, String currency) {
        return "sandbox_pi_" + paymentIntentId;
    }
}
