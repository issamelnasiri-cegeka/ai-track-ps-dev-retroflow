package com.stackcraft.retroflow.repository;

import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.entity.RetrospectiveStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RetrospectiveRepository extends JpaRepository<Retrospective, Long> {

    boolean existsByTeamIdAndStatus(Long teamId, RetrospectiveStatus status);

    List<Retrospective> findByTeamIdOrderByIdAsc(Long teamId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Retrospective r where r.id = :id")
    Optional<Retrospective> findByIdForUpdate(@Param("id") Long id);
}
