package za.hungu.plinth.rooms;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomParticipantRepository extends JpaRepository<RoomParticipant, RoomParticipant.RoomParticipantId> {
    Optional<RoomParticipant> findByRoomIdAndAccountId(UUID roomId, UUID accountId);
    boolean existsByRoomIdAndAccountIdAndStatus(UUID roomId, UUID accountId, RoomParticipantStatus status);
    long countByRoomIdAndStatus(UUID roomId, RoomParticipantStatus status);
    List<RoomParticipant> findByRoomIdAndStatusOrderByRequestedAtAsc(UUID roomId, RoomParticipantStatus status);
    List<RoomParticipant> findByAccountIdAndStatus(UUID accountId, RoomParticipantStatus status);
}
