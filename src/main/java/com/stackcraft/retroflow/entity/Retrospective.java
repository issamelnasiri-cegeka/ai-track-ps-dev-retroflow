package com.stackcraft.retroflow.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * STUB — left behind by the previous vendor.
 *
 * A Retrospective belongs to a Team, has a title, a date, and a status
 * (OPEN or CLOSED). A team can have multiple retrospectives but only one
 * OPEN at a time; once closed, a retrospective can never be reopened. See
 * the RetroFlow brief for the full requirements — fields, the status
 * enum, and the relationship to Team still need to be designed and
 * implemented.
 */
@Entity
public class Retrospective {

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
