package com.stackcraft.retroflow.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Set;

@Entity
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    @NotNull
    @Size(min = 1)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "team_members", joinColumns = @JoinColumn(name = "team_id"))
    @Column(name = "member_name", nullable = false, length = 100)
    private Set<@NotBlank @Size(max = 100) String> members = new HashSet<>();

    @OneToMany(mappedBy = "team", fetch = FetchType.LAZY)
    private Set<Retrospective> retrospectives = new HashSet<>();

    public Team() {
    }

    public Team(Long id, String name, Set<String> members, Set<Retrospective> retrospectives) {
        this.id = id;
        this.name = name;
        this.members = members;
        this.retrospectives = retrospectives;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getMembers() {
        return members;
    }

    public void setMembers(Set<String> members) {
        this.members = members;
    }

    public Set<Retrospective> getRetrospectives() {
        return retrospectives;
    }

    public void setRetrospectives(Set<Retrospective> retrospectives) {
        this.retrospectives = retrospectives;
    }
}
