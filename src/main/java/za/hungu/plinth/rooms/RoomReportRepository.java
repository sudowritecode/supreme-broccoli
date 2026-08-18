package za.hungu.plinth.rooms;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RoomReportRepository extends JpaRepository<RoomReport, UUID> {
}
