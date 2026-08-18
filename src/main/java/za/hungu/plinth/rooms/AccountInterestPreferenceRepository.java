package za.hungu.plinth.rooms;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AccountInterestPreferenceRepository extends JpaRepository<AccountInterestPreference, AccountInterestPreference.AccountInterestPreferenceId> {
    List<AccountInterestPreference> findByAccountId(UUID accountId);
    boolean existsByAccountIdAndTag(UUID accountId, String tag);
    long deleteByAccountIdAndTagIn(UUID accountId, Collection<String> tags);
}
