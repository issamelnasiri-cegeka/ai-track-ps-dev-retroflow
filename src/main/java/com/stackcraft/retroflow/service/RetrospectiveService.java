package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.entity.ActionItem;
import com.stackcraft.retroflow.entity.ActionPriority;
import com.stackcraft.retroflow.entity.FeedbackItem;
import com.stackcraft.retroflow.entity.FeedbackType;
import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.entity.RetrospectiveStatus;
import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.exception.ResourceNotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import com.stackcraft.retroflow.repository.FeedbackItemRepository;
import com.stackcraft.retroflow.repository.RetrospectiveRepository;
import com.stackcraft.retroflow.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Service layer for retrospectives and their feedback/action items.
 *
 * Enforces retrospective business rules and coordinates persistence.
 */
@Service
public class RetrospectiveService {

    private final RetrospectiveRepository retrospectiveRepository;
    private final TeamRepository teamRepository;
    private final FeedbackItemRepository feedbackItemRepository;

    public RetrospectiveService(RetrospectiveRepository retrospectiveRepository,
                                 TeamRepository teamRepository,
                                 FeedbackItemRepository feedbackItemRepository) {
        this.retrospectiveRepository = retrospectiveRepository;
        this.teamRepository = teamRepository;
        this.feedbackItemRepository = feedbackItemRepository;
    }

    /**
     * Creates a new retrospective for the given team.
     *
     * @param teamId the id of the team the retrospective belongs to
     * @param title  the retrospective's title
     * @return the created retrospective
     * @throws ResourceNotFoundException if no team exists with the given id
     * @throws RetroflowException        if the team already has an OPEN retrospective
     */
    @Transactional
    public Retrospective createRetrospective(Long teamId, String title) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("[Team] [" + teamId + "]: not found"));
        boolean hasOpenRetrospective = retrospectiveRepository.findAll().stream()
                .anyMatch(retrospective -> retrospective.getTeam().getId().equals(teamId)
                        && retrospective.getStatus() == RetrospectiveStatus.OPEN);
        if (hasOpenRetrospective) {
            throw new RetroflowException("[Team] [" + teamId + "]: already has an open retrospective");
        }

        Retrospective retrospective = new Retrospective();
        retrospective.setTeam(team);
        retrospective.setTitle(title);
        retrospective.setDate(LocalDate.now());
        retrospective.setStatus(RetrospectiveStatus.OPEN);
        return retrospectiveRepository.save(retrospective);
    }

    /**
     * Retrieves a retrospective by its id.
     *
     * @param id the retrospective id
     * @return the matching retrospective
     * @throws ResourceNotFoundException if no retrospective exists with the given id
     */
    public Retrospective getRetrospectiveById(Long id) {
        return retrospectiveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("[Retrospective] [" + id + "]: not found"));
    }

    /**
     * Lists all retrospectives for the given team.
     *
     * @param teamId the id of the team
     * @return the team's retrospectives
     * @throws ResourceNotFoundException if no team exists with the given id
     */
    public List<Retrospective> getRetrospectivesForTeam(Long teamId) {
        teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("[Team] [" + teamId + "]: not found"));
        return retrospectiveRepository.findAll().stream()
                .filter(retrospective -> retrospective.getTeam().getId().equals(teamId))
                .toList();
    }

    /**
     * Closes an open retrospective.
     *
     * @param id the retrospective id
     * @return the closed retrospective
     * @throws ResourceNotFoundException if no retrospective exists with the given id
     * @throws RetroflowException        if the retrospective is already closed
     */
    @Transactional
    public Retrospective closeRetrospective(Long id) {
        Retrospective retrospective = getRetrospectiveById(id);
        if (retrospective.getStatus() == RetrospectiveStatus.CLOSED) {
            throw new RetroflowException("[Retrospective] [" + id + "]: is already closed");
        }
        retrospective.setStatus(RetrospectiveStatus.CLOSED);
        return retrospectiveRepository.save(retrospective);
    }

    /**
     * Adds a feedback item to a retrospective.
     *
     * @param retrospectiveId the retrospective id
     * @param content         the feedback content
     * @param type            the feedback type (e.g. WENT_WELL, NEEDS_IMPROVEMENT)
     * @param submittedBy     the name of the member submitting the feedback
     * @return the created feedback item
     * @throws ResourceNotFoundException if no retrospective exists with the given id
     * @throws RetroflowException        if the retrospective is closed
     */
    @Transactional
    public FeedbackItem addFeedbackItem(Long retrospectiveId, String content, String type, String submittedBy) {
        Retrospective retrospective = getRetrospectiveById(retrospectiveId);
        ensureOpen(retrospective);

        FeedbackType feedbackType;
        try {
            feedbackType = FeedbackType.valueOf(type);
        } catch (IllegalArgumentException ex) {
            throw new RetroflowException("[Retrospective] [" + retrospectiveId
                    + "]: invalid feedback type " + type);
        }

        FeedbackItem item = new FeedbackItem();
        item.setContent(content);
        item.setType(feedbackType);
        item.setSubmittedBy(submittedBy);
        item.setRetrospective(retrospective);
        return feedbackItemRepository.save(item);
    }

    /**
     * Adds an action item to a retrospective.
     *
     * @param retrospectiveId the retrospective id
     * @param content         the action item description
     * @param priority        the action item priority (LOW, MEDIUM, HIGH)
     * @param submittedBy     the name of the member submitting the action item
     * @return the created action item
     * @throws ResourceNotFoundException if no retrospective exists with the given id
     * @throws RetroflowException        if the retrospective is closed
     */
    @Transactional
    public ActionItem addActionItem(Long retrospectiveId, String content, String priority, String submittedBy) {
        Retrospective retrospective = getRetrospectiveById(retrospectiveId);
        ensureOpen(retrospective);

        ActionPriority actionPriority;
        try {
            actionPriority = ActionPriority.valueOf(priority);
        } catch (IllegalArgumentException ex) {
            throw new RetroflowException("[Retrospective] [" + retrospectiveId
                    + "]: invalid action priority " + priority);
        }

        ActionItem item = new ActionItem();
        item.setContent(content);
        item.setType(FeedbackType.ACTION_ITEM);
        item.setPriority(actionPriority);
        item.setSubmittedBy(submittedBy);
        item.setRetrospective(retrospective);
        return feedbackItemRepository.save(item);
    }

    /**
     * Marks an action item as completed.
     *
     * @param actionItemId the action item id
     * @return the updated action item
     * @throws ResourceNotFoundException if no action item exists with the given id
     */
    @Transactional
    public ActionItem completeActionItem(Long actionItemId) {
        ActionItem actionItem = getActionItemById(actionItemId);
        actionItem.setCompleted(true);
        return feedbackItemRepository.save(actionItem);
    }

    /**
     * Marks a previously completed action item as not completed.
     *
     * @param actionItemId the action item id
     * @return the updated action item
     * @throws ResourceNotFoundException if no action item exists with the given id
     * @throws RetroflowException        if the action item is already completed
     *                                   and cannot be uncompleted
     */
    @Transactional
    public ActionItem uncompleteActionItem(Long actionItemId) {
        ActionItem actionItem = getActionItemById(actionItemId);
        if (actionItem.isCompleted()) {
            throw new RetroflowException("[ActionItem] [" + actionItemId + "]: is already completed");
        }
        actionItem.setCompleted(false);
        return feedbackItemRepository.save(actionItem);
    }

    private void ensureOpen(Retrospective retrospective) {
        if (retrospective.getStatus() == RetrospectiveStatus.CLOSED) {
            throw new RetroflowException("[Retrospective] [" + retrospective.getId()
                    + "]: is closed");
        }
    }

    private ActionItem getActionItemById(Long actionItemId) {
        FeedbackItem item = feedbackItemRepository.findById(actionItemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "[ActionItem] [" + actionItemId + "]: not found"));
        if (!(item instanceof ActionItem actionItem)) {
            throw new ResourceNotFoundException("[ActionItem] [" + actionItemId + "]: not found");
        }
        return actionItem;
    }

}
