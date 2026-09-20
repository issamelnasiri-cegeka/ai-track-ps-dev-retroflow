package com.stackcraft.retroflow.repository;

import com.stackcraft.retroflow.entity.Retrospective;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * STUB — left behind by the previous vendor.
 *
 * Extends JpaRepository for basic CRUD out of the box; add derived query
 * methods here as the service layer needs them (e.g. finding the current
 * OPEN retrospective for a team).
 */
public interface RetrospectiveRepository extends JpaRepository<Retrospective, Long> {
}
