package com.stackcraft.retroflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "item_kind", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("FEEDBACK")
public class FeedbackItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 5000)
    @Column(nullable = false, length = 5000)
    private String content;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FeedbackType type;

    @NotBlank
    @Size(max = 100)
    @Column(name = "submitted_by", nullable = false, length = 100)
    private String submittedBy;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "retrospective_id", nullable = false)
    private Retrospective retrospective;

    public FeedbackItem() {
    }

    public FeedbackItem(Long id, String content, FeedbackType type, String submittedBy,
                        Retrospective retrospective) {
        this.id = id;
        this.content = content;
        this.type = type;
        this.submittedBy = submittedBy;
        this.retrospective = retrospective;
    }

    /** @return the feedback item identifier */
    public Long getId() {
        return id;
    }

    /** @param id the feedback item identifier */
    public void setId(Long id) {
        this.id = id;
    }

    /** @return the feedback content */
    public String getContent() {
        return content;
    }

    /** @param content the feedback content */
    public void setContent(String content) {
        this.content = content;
    }

    /** @return the feedback type */
    public FeedbackType getType() {
        return type;
    }

    /** @param type the feedback type */
    public void setType(FeedbackType type) {
        this.type = type;
    }

    /** @return the submitting member */
    public String getSubmittedBy() {
        return submittedBy;
    }

    /** @param submittedBy the submitting member */
    public void setSubmittedBy(String submittedBy) {
        this.submittedBy = submittedBy;
    }

    /** @return the owning retrospective */
    public Retrospective getRetrospective() {
        return retrospective;
    }

    /** @param retrospective the owning retrospective */
    public void setRetrospective(Retrospective retrospective) {
        this.retrospective = retrospective;
    }
}
