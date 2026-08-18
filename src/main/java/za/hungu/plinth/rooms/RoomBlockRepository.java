package za.hungu.plinth.rooms;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RoomBlockRepository extends JpaRepository<RoomBlock, RoomBlock.RoomBlockId> {
    boolean existsByRoomIdAndBlockedAccountId(UUID roomId, UUID blockedAccountId);
}
