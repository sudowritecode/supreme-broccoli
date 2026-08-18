package za.hungu.plinth.rooms;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "account_interest_preferences")
@IdClass(AccountInterestPreference.AccountInterestPreferenceId.class)
public class AccountInterestPreference {
    @Id
    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Id
    @Column(nullable = false, length = 32)
    private String tag;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AccountInterestPreference() {
    }

    private AccountInterestPreference(UUID accountId, String tag, Instant createdAt) {
        this.accountId = accountId;
        this.tag = tag;
        this.createdAt = createdAt;
    }

    public static AccountInterestPreference create(UUID accountId, String tag, Instant createdAt) {
        return new AccountInterestPreference(accountId, tag, createdAt);
    }

    public UUID getAccountId() { return accountId; }
    public String getTag() { return tag; }
    public Instant getCreatedAt() { return createdAt; }

    public static class AccountInterestPreferenceId implements Serializable {
        private UUID accountId;
        private String tag;

        public AccountInterestPreferenceId() {
        }

        public AccountInterestPreferenceId(UUID accountId, String tag) {
            this.accountId = accountId;
            this.tag = tag;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof AccountInterestPreferenceId that)) return false;
            return Objects.equals(accountId, that.accountId) && Objects.equals(tag, that.tag);
        }

        @Override
        public int hashCode() {
            return Objects.hash(accountId, tag);
        }
    }
}
