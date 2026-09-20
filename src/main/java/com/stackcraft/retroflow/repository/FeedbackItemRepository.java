package com.stackcraft.retroflow.repository;

import com.stackcraft.retroflow.entity.FeedbackItem;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * STUB — left behind by the previous vendor.
 *
 * Extends JpaRepository for basic CRUD out of the box; add derived query
 * methods here as the service layer needs them (e.g. finding items by
 * retrospective, or filtering action items by priority/completed status).
 */
public interface FeedbackItemRepository extends JpaRepository<FeedbackItem, Long> {
}
