package com.stackcraft.retroflow.repository;

import com.stackcraft.retroflow.entity.FeedbackItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FeedbackItemRepository extends JpaRepository<FeedbackItem, Long> {

    List<FeedbackItem> findByRetrospectiveIdOrderByIdAsc(Long retrospectiveId);

    @Query("select f.retrospective.id from FeedbackItem f where f.id = :id")
    Optional<Long> findRetrospectiveIdById(@Param("id") Long id);
}
