package com.stackcraft.retroflow.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * STUB — left behind by the previous vendor.
 *
 * A Team has a name and a list of members (member names only, no
 * authentication) and must have at least one member. See the RetroFlow
 * brief for the full requirements — fields, validation, and relationships
 * to Retrospective still need to be designed and implemented.
 */
@Entity
public class Team {

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
