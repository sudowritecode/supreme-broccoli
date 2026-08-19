package za.hungu.plinth.payments;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreatePaymentIntentRequest(
        @Min(1) long amountMinor,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency
) {
}
