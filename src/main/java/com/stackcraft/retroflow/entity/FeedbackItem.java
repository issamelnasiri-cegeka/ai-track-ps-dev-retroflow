package com.stackcraft.retroflow.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * STUB — left behind by the previous vendor.
 *
 * A FeedbackItem belongs to a Retrospective, has content (text), a type
 * (WENT_WELL, NEEDS_IMPROVEMENT, ACTION_ITEM), and the name of the team
 * member who submitted it. Items can be added to an OPEN retrospective;
 * once the retrospective is CLOSED, its feedback items can never be
 * modified or deleted.
 *
 * ActionItem is a special kind of FeedbackItem (type ACTION_ITEM) that
 * additionally tracks a priority and a completed flag — see the brief for
 * how that relationship should work. Deciding and implementing the
 * inheritance strategy between this class and ActionItem is part of
 * Block 1; nothing has been decided yet.
 */
@Entity
public class FeedbackItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

}
