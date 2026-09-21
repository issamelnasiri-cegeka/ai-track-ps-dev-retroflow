package com.stackcraft.retroflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;

@Entity
@DiscriminatorValue("ACTION_ITEM")
public class ActionItem extends FeedbackItem {

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ActionPriority priority;

    @Column(nullable = false)
    private boolean completed = false;

    public ActionItem() {
    }

    public ActionItem(Long id, String content, FeedbackType type, String submittedBy,
                      Retrospective retrospective, ActionPriority priority, boolean completed) {
        super(id, content, type, submittedBy, retrospective);
        this.priority = priority;
        this.completed = completed;
    }

    public ActionPriority getPriority() {
        return priority;
    }

    public void setPriority(ActionPriority priority) {
        this.priority = priority;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
