package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.entity.ActionItem;
import com.stackcraft.retroflow.entity.FeedbackItem;
import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.exception.ResourceNotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import com.stackcraft.retroflow.repository.FeedbackItemRepository;
import com.stackcraft.retroflow.repository.RetrospectiveRepository;
import com.stackcraft.retroflow.repository.TeamRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service layer for retrospectives and their feedback/action items.
 *
 * STUB — method signatures only. Business logic (persistence, invariant
 * enforcement, etc.) will be implemented separately.
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
    public Retrospective createRetrospective(Long teamId, String title) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Retrieves a retrospective by its id.
     *
     * @param id the retrospective id
     * @return the matching retrospective
     * @throws ResourceNotFoundException if no retrospective exists with the given id
     */
    public Retrospective getRetrospectiveById(Long id) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Lists all retrospectives for the given team.
     *
     * @param teamId the id of the team
     * @return the team's retrospectives
     * @throws ResourceNotFoundException if no team exists with the given id
     */
    public List<Retrospective> getRetrospectivesForTeam(Long teamId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Closes an open retrospective.
     *
     * @param id the retrospective id
     * @return the closed retrospective
     * @throws ResourceNotFoundException if no retrospective exists with the given id
     * @throws RetroflowException        if the retrospective is already closed
     */
    public Retrospective closeRetrospective(Long id) {
        throw new UnsupportedOperationException("Not implemented yet");
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
    public FeedbackItem addFeedbackItem(Long retrospectiveId, String content, String type, String submittedBy) {
        throw new UnsupportedOperationException("Not implemented yet");
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
    public ActionItem addActionItem(Long retrospectiveId, String content, String priority, String submittedBy) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Marks an action item as completed.
     *
     * @param actionItemId the action item id
     * @return the updated action item
     * @throws ResourceNotFoundException if no action item exists with the given id
     */
    public ActionItem completeActionItem(Long actionItemId) {
        throw new UnsupportedOperationException("Not implemented yet");
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
    public ActionItem uncompleteActionItem(Long actionItemId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

}
