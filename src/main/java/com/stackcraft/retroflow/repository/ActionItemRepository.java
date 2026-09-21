package com.stackcraft.retroflow.repository;

import com.stackcraft.retroflow.entity.ActionItem;
import com.stackcraft.retroflow.entity.ActionPriority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ActionItemRepository extends JpaRepository<ActionItem, Long> {

    @Query("select a.retrospective.id from ActionItem a where a.id = :id")
    Optional<Long> findRetrospectiveIdById(@Param("id") Long id);

    @Query("""
            select a from ActionItem a
            where a.retrospective.id = :retrospectiveId
              and (:priority is null or a.priority = :priority)
              and (:completed is null or a.completed = :completed)
            order by a.id
            """)
    List<ActionItem> findActionItems(@Param("retrospectiveId") Long retrospectiveId,
                                    @Param("priority") ActionPriority priority,
                                    @Param("completed") Boolean completed);
}
