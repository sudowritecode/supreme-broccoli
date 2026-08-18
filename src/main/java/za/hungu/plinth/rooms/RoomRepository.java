package za.hungu.plinth.rooms;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RoomRepository extends JpaRepository<Room, UUID> {
    List<Room> findByStatusOrderByCreatedAtDesc(RoomStatus status);
}
