package com.stackcraft.retroflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Retrospective {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String title;

    @NotNull
    @Column(name = "retro_date", nullable = false)
    private LocalDate date;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RetrospectiveStatus status = RetrospectiveStatus.OPEN;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @OneToMany(mappedBy = "retrospective", fetch = FetchType.LAZY)
    private Set<FeedbackItem> feedbackItems = new HashSet<>();

    public Retrospective() {
    }

    public Retrospective(Long id, String title, LocalDate date, RetrospectiveStatus status, Team team,
                         Set<FeedbackItem> feedbackItems) {
        this.id = id;
        this.title = title;
        this.date = date;
        this.status = status;
        this.team = team;
        this.feedbackItems = feedbackItems;
    }

    /** @return the retrospective identifier */
    public Long getId() {
        return id;
    }

    /** @param id the retrospective identifier */
    public void setId(Long id) {
        this.id = id;
    }

    /** @return the retrospective title */
    public String getTitle() {
        return title;
    }

    /** @param title the retrospective title */
    public void setTitle(String title) {
        this.title = title;
    }

    /** @return the retrospective date */
    public LocalDate getDate() {
        return date;
    }

    /** @param date the retrospective date */
    public void setDate(LocalDate date) {
        this.date = date;
    }

    /** @return the retrospective status */
    public RetrospectiveStatus getStatus() {
        return status;
    }

    /** @param status the retrospective status */
    public void setStatus(RetrospectiveStatus status) {
        this.status = status;
    }

    /** @return the owning team */
    public Team getTeam() {
        return team;
    }

    /** @param team the owning team */
    public void setTeam(Team team) {
        this.team = team;
    }

    /** @return the retrospective feedback items */
    public Set<FeedbackItem> getFeedbackItems() {
        return feedbackItems;
    }

    /** @param feedbackItems the retrospective feedback items */
    public void setFeedbackItems(Set<FeedbackItem> feedbackItems) {
        this.feedbackItems = feedbackItems;
    }
}
