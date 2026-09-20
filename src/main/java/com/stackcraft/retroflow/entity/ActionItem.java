package com.stackcraft.retroflow.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * STUB — left behind by the previous vendor.
 *
 * An ActionItem is a special kind of FeedbackItem (type ACTION_ITEM) that
 * additionally tracks a priority (LOW/MEDIUM/HIGH) and a boolean
 * completed flag. Action items can be marked completed, but a completed
 * action item can never be uncompleted. They must also be retrievable
 * filtered by priority and by completed status.
 *
 * This class is not yet related to FeedbackItem — deciding and
 * implementing the inheritance strategy between the two (single table,
 * joined, or a composition approach) is part of Block 1. Nothing has
 * been decided yet; this is deliberately just a bare stub.
 */
@Entity
public class ActionItem {

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
