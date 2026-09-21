package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.entity.ActionItem;
import com.stackcraft.retroflow.entity.ActionPriority;
import com.stackcraft.retroflow.entity.FeedbackItem;
import com.stackcraft.retroflow.entity.FeedbackType;
import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.entity.RetrospectiveStatus;
import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.exception.ActionItemUncompletionNotAllowedException;
import com.stackcraft.retroflow.exception.InvalidActionPriorityException;
import com.stackcraft.retroflow.exception.InvalidFeedbackTypeException;
import com.stackcraft.retroflow.exception.OpenRetrospectiveExistsException;
import com.stackcraft.retroflow.exception.ResourceNotFoundException;
import com.stackcraft.retroflow.exception.RetrospectiveClosedException;
import com.stackcraft.retroflow.exception.RetrospectiveReopeningNotAllowedException;
import com.stackcraft.retroflow.exception.SubmitterNotTeamMemberException;
import com.stackcraft.retroflow.repository.ActionItemRepository;
import com.stackcraft.retroflow.repository.FeedbackItemRepository;
import com.stackcraft.retroflow.repository.RetrospectiveRepository;
import com.stackcraft.retroflow.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static com.stackcraft.retroflow.service.ServiceValidation.requireId;
import static com.stackcraft.retroflow.service.ServiceValidation.requireText;

/**
 * Service layer for retrospectives and their feedback/action items.
 */
@Service
@Transactional(readOnly = true)
public class RetrospectiveService {

    private final RetrospectiveRepository retrospectiveRepository;
    private final TeamRepository teamRepository;
    private final FeedbackItemRepository feedbackItemRepository;
    private final ActionItemRepository actionItemRepository;

    public RetrospectiveService(RetrospectiveRepository retrospectiveRepository,
                                TeamRepository teamRepository,
                                FeedbackItemRepository feedbackItemRepository,
                                ActionItemRepository actionItemRepository) {
        this.retrospectiveRepository = retrospectiveRepository;
        this.teamRepository = teamRepository;
        this.feedbackItemRepository = feedbackItemRepository;
        this.actionItemRepository = actionItemRepository;
    }

    /**
     * Creates a new retrospective for the given team.
     *
     * @param teamId the id of the team the retrospective belongs to
     * @param title  the retrospective's title
     * @return the created retrospective
     * @throws ResourceNotFoundException if no team exists with the given id
     * @throws OpenRetrospectiveExistsException if the team already has an OPEN retrospective
     */
    @Transactional
    public Retrospective createRetrospective(Long teamId, String title) {
        requireId(teamId, "teamId");
        requireText(title, "title", 200);

        // Lock the team even when it has no retrospectives, serializing concurrent creates.
        Team team = teamRepository.findByIdForUpdate(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team " + teamId + " not found"));
        if (retrospectiveRepository.existsByTeamIdAndStatus(teamId, RetrospectiveStatus.OPEN)) {
            throw new OpenRetrospectiveExistsException(teamId);
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
        requireId(id, "retrospectiveId");
        return retrospectiveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Retrospective " + id + " not found"));
    }

    /**
     * Lists all retrospectives for the given team.
     *
     * @param teamId the id of the team
     * @return the team's retrospectives
     * @throws ResourceNotFoundException if no team exists with the given id
     */
    public List<Retrospective> getRetrospectivesForTeam(Long teamId) {
        requireId(teamId, "teamId");
        if (!teamRepository.existsById(teamId)) {
            throw new ResourceNotFoundException("Team " + teamId + " not found");
        }
        return retrospectiveRepository.findByTeamIdOrderByIdAsc(teamId);
    }

    /**
     * Closes an open retrospective.
     *
     * @param id the retrospective id
     * @return the closed retrospective
     * @throws ResourceNotFoundException if no retrospective exists with the given id
     * @throws RetrospectiveClosedException if the retrospective is already closed
     */
    @Transactional
    public Retrospective closeRetrospective(Long id) {
        Retrospective retrospective = getRetrospectiveForUpdate(id);
        requireOpen(retrospective);
        retrospective.setStatus(RetrospectiveStatus.CLOSED);
        return retrospectiveRepository.save(retrospective);
    }

    /**
     * Returns an already-open retrospective unchanged; never reopens a closed one.
     *
     * @throws RetrospectiveReopeningNotAllowedException if the retrospective is closed
     */
    public Retrospective reopenRetrospective(Long id) {
        Retrospective retrospective = getRetrospectiveById(id);
        if (retrospective.getStatus() == RetrospectiveStatus.CLOSED) {
            throw new RetrospectiveReopeningNotAllowedException(id);
        }
        return retrospective;
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
     * @throws RetrospectiveClosedException if the retrospective is closed
     * @throws SubmitterNotTeamMemberException if the submitter is not a team member
     * @throws InvalidFeedbackTypeException if the type is invalid or requires an action item
     */
    @Transactional
    public FeedbackItem addFeedbackItem(Long retrospectiveId, String content, String type, String submittedBy) {
        requireText(content, "content", 5000);
        requireText(submittedBy, "submittedBy", 100);
        FeedbackType feedbackType = parseFeedbackType(type);
        Retrospective retrospective = getRetrospectiveForUpdate(retrospectiveId);
        requireOpen(retrospective);
        requireTeamMember(retrospective, submittedBy);

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
     * @throws RetrospectiveClosedException if the retrospective is closed
     * @throws SubmitterNotTeamMemberException if the submitter is not a team member
     * @throws InvalidActionPriorityException if the priority is invalid
     */
    @Transactional
    public ActionItem addActionItem(Long retrospectiveId, String content, String priority, String submittedBy) {
        requireText(content, "content", 5000);
        requireText(submittedBy, "submittedBy", 100);
        ActionPriority actionPriority = parsePriority(priority);
        Retrospective retrospective = getRetrospectiveForUpdate(retrospectiveId);
        requireOpen(retrospective);
        requireTeamMember(retrospective, submittedBy);

        ActionItem item = new ActionItem();
        item.setContent(content);
        item.setType(FeedbackType.ACTION_ITEM);
        item.setSubmittedBy(submittedBy);
        item.setRetrospective(retrospective);
        item.setPriority(actionPriority);
        item.setCompleted(false);
        return actionItemRepository.save(item);
    }

    /**
     * Retrieves all feedback, including action items, even after closure.
     */
    public List<FeedbackItem> getFeedbackItemsForRetrospective(Long retrospectiveId) {
        getRetrospectiveById(retrospectiveId);
        return feedbackItemRepository.findByRetrospectiveIdOrderByIdAsc(retrospectiveId);
    }

    /**
     * Filters action items within a retrospective. Null filters are omitted;
     * when both are supplied, both must match. Closed retrospectives remain readable.
     */
    public List<ActionItem> getActionItems(Long retrospectiveId, String priority, Boolean completed) {
        ActionPriority actionPriority = priority == null ? null : parsePriority(priority);
        getRetrospectiveById(retrospectiveId);
        return actionItemRepository.findActionItems(retrospectiveId, actionPriority, completed);
    }

    /**
     * Updates content without changing type, submitter, or action completion state.
     */
    @Transactional
    public FeedbackItem updateFeedbackItem(Long feedbackItemId, String content) {
        requireText(content, "content", 5000);
        FeedbackItem item = getFeedbackItemForUpdate(feedbackItemId);
        requireOpen(item.getRetrospective());
        item.setContent(content);
        return feedbackItemRepository.save(item);
    }

    /**
     * Updates action content and priority without changing its completion state.
     */
    @Transactional
    public ActionItem updateActionItem(Long actionItemId, String content, String priority) {
        requireText(content, "content", 5000);
        ActionPriority actionPriority = parsePriority(priority);
        ActionItem item = getActionItemForUpdate(actionItemId);
        requireOpen(item.getRetrospective());
        item.setContent(content);
        item.setPriority(actionPriority);
        return actionItemRepository.save(item);
    }

    /**
     * Deletes either a regular feedback item or an action item from an open retrospective.
     */
    @Transactional
    public void deleteFeedbackItem(Long feedbackItemId) {
        FeedbackItem item = getFeedbackItemForUpdate(feedbackItemId);
        requireOpen(item.getRetrospective());
        feedbackItemRepository.delete(item);
    }

    /**
     * Marks an action item as completed, or returns it unchanged if already completed.
     *
     * @param actionItemId the action item id
     * @return the updated action item
     * @throws ResourceNotFoundException if no action item exists with the given id
     * @throws RetrospectiveClosedException if the retrospective is closed
     */
    @Transactional
    public ActionItem completeActionItem(Long actionItemId) {
        ActionItem item = getActionItemForUpdate(actionItemId);
        requireOpen(item.getRetrospective());
        if (item.isCompleted()) {
            return item;
        }
        item.setCompleted(true);
        return actionItemRepository.save(item);
    }

    /**
     * Returns an incomplete action item unchanged; never uncompletes a completed one.
     *
     * @param actionItemId the action item id
     * @return the updated action item
     * @throws ResourceNotFoundException if no action item exists with the given id
     * @throws ActionItemUncompletionNotAllowedException if the action item is already completed
     * @throws RetrospectiveClosedException if the retrospective is closed
     */
    @Transactional
    public ActionItem uncompleteActionItem(Long actionItemId) {
        ActionItem item = getActionItemForUpdate(actionItemId);
        requireOpen(item.getRetrospective());
        if (item.isCompleted()) {
            throw new ActionItemUncompletionNotAllowedException(actionItemId);
        }
        return item;
    }

    private Retrospective getRetrospectiveForUpdate(Long id) {
        requireId(id, "retrospectiveId");
        return retrospectiveRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Retrospective " + id + " not found"));
    }

    private FeedbackItem getFeedbackItemForUpdate(Long id) {
        requireId(id, "feedbackItemId");
        // Load only the parent ID first: item state must be read after acquiring the parent lock.
        Long retrospectiveId = feedbackItemRepository.findRetrospectiveIdById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback item " + id + " not found"));
        getRetrospectiveForUpdate(retrospectiveId);
        return feedbackItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback item " + id + " not found"));
    }

    private ActionItem getActionItemForUpdate(Long id) {
        requireId(id, "actionItemId");
        Long retrospectiveId = actionItemRepository.findRetrospectiveIdById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Action item " + id + " not found"));
        getRetrospectiveForUpdate(retrospectiveId);
        return actionItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Action item " + id + " not found"));
    }

    private void requireOpen(Retrospective retrospective) {
        if (retrospective.getStatus() == RetrospectiveStatus.CLOSED) {
            throw new RetrospectiveClosedException(retrospective.getId());
        }
    }

    private void requireTeamMember(Retrospective retrospective, String submittedBy) {
        Team team = retrospective.getTeam();
        if (!team.getMembers().contains(submittedBy)) {
            throw new SubmitterNotTeamMemberException(submittedBy, team.getId());
        }
    }

    private FeedbackType parseFeedbackType(String type) {
        return switch (type) {
            case "WENT_WELL" -> FeedbackType.WENT_WELL;
            case "NEEDS_IMPROVEMENT" -> FeedbackType.NEEDS_IMPROVEMENT;
            case null, default -> throw new InvalidFeedbackTypeException(type);
        };
    }

    private ActionPriority parsePriority(String priority) {
        return switch (priority) {
            case "LOW" -> ActionPriority.LOW;
            case "MEDIUM" -> ActionPriority.MEDIUM;
            case "HIGH" -> ActionPriority.HIGH;
            case null, default -> throw new InvalidActionPriorityException(priority);
        };
    }

}
