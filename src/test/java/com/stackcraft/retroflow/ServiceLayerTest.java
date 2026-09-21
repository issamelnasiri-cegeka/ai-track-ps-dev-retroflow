package com.stackcraft.retroflow;

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
import com.stackcraft.retroflow.exception.InvalidInputException;
import com.stackcraft.retroflow.exception.OpenRetrospectiveExistsException;
import com.stackcraft.retroflow.exception.ResourceNotFoundException;
import com.stackcraft.retroflow.exception.RetrospectiveClosedException;
import com.stackcraft.retroflow.exception.RetrospectiveReopeningNotAllowedException;
import com.stackcraft.retroflow.exception.SubmitterNotTeamMemberException;
import com.stackcraft.retroflow.exception.TeamMustHaveMembersException;
import com.stackcraft.retroflow.service.RetrospectiveService;
import com.stackcraft.retroflow.service.TeamService;
import com.stackcraft.retroflow.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

@SpringBootTest
class ServiceLayerTest {

    private static final long MISSING_ID = Long.MAX_VALUE;

    private final TeamService teamService;
    private final RetrospectiveService retrospectiveService;
    private final GlobalExceptionHandler exceptionHandler;
    private Team team;
    private Retrospective retrospective;

    @Autowired
    ServiceLayerTest(TeamService teamService, RetrospectiveService retrospectiveService,
                     GlobalExceptionHandler exceptionHandler) {
        this.teamService = teamService;
        this.retrospectiveService = retrospectiveService;
        this.exceptionHandler = exceptionHandler;
    }

    @BeforeEach
    void setUp() {
        // Shared arrangement: a team and its OPEN retrospective, isolated by generated IDs.
        team = teamService.createTeam("Team", List.of("Alice", "Bob"));
        retrospective = retrospectiveService.createRetrospective(team.getId(), "Sprint");
    }

    @Test
    void teamsArePersistedWithCopiedDistinctMembersAndMayShareNames() {
        // Arrange
        List<String> members = new ArrayList<>(List.of("Alice", "Alice", "Bob"));

        // Act
        Team created = teamService.createTeam("Team", members);
        members.clear();
        Team stored = teamService.getTeamById(created.getId());

        // Assert
        assertThat(stored.getId()).isNotEqualTo(team.getId());
        assertThat(stored.getName()).isEqualTo("Team");
        assertThat(stored.getMembers()).containsExactlyInAnyOrder("Alice", "Bob");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void creatingTeamWithoutMembersFailsWithBadRequest(List<String> members) {
        // Arrange: the name is valid, so only the missing members cause the failure.
        String name = "Team without members";

        // Act
        TeamMustHaveMembersException exception = catchThrowableOfType(
                () -> teamService.createTeam(name, members), TeamMustHaveMembersException.class);

        // Assert
        assertThat(exception)
                .isExactlyInstanceOf(TeamMustHaveMembersException.class)
                .hasMessage("A team must have at least one member");
        assertThat(exceptionHandler.handleInvalidInput(exception).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void retrievingMissingTeamThrowsResourceNotFound() {
        // Arrange: this positive ID is outside the range of generated fixture IDs.
        Long missingTeamId = MISSING_ID;

        // Act
        ResourceNotFoundException exception = catchThrowableOfType(
                () -> teamService.getTeamById(missingTeamId), ResourceNotFoundException.class);

        // Assert
        assertThat(exception)
                .isExactlyInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Team " + missingTeamId + " not found");
        assertThat(exceptionHandler.handleNotFound(exception).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void namesAndMemberNamesMustBeNonblank(String name) {
        // Arrange
        List<String> validMembers = List.of("Alice");
        List<String> invalidMembers = Arrays.asList(name);

        // Act
        Throwable nameFailure = catchThrowable(() -> teamService.createTeam(name, validMembers));
        Throwable memberFailure = catchThrowable(() -> teamService.createTeam("Team", invalidMembers));

        // Assert
        assertThat(nameFailure).isExactlyInstanceOf(InvalidInputException.class);
        assertThat(memberFailure).isExactlyInstanceOf(InvalidInputException.class);
    }

    @Test
    void serviceEnforcesTextLengthsWithoutRelyingOnPersistenceValidation() {
        // Arrange
        String oversizedName = "x".repeat(101);
        String oversizedTitle = "x".repeat(201);
        String oversizedContent = "x".repeat(5001);

        // Act
        Throwable nameFailure = catchThrowable(() -> teamService.createTeam(oversizedName, List.of("Alice")));
        Throwable memberFailure = catchThrowable(() -> teamService.createTeam("Team", List.of(oversizedName)));
        Throwable titleFailure = catchThrowable(() ->
                retrospectiveService.createRetrospective(team.getId(), oversizedTitle));
        Throwable contentFailure = catchThrowable(() -> retrospectiveService.addFeedbackItem(
                retrospective.getId(), oversizedContent, "WENT_WELL", "Alice"));
        Throwable submitterFailure = catchThrowable(() -> retrospectiveService.addActionItem(
                retrospective.getId(), "Fix", "HIGH", oversizedName));

        // Assert
        assertThat(nameFailure).isExactlyInstanceOf(InvalidInputException.class);
        assertThat(memberFailure).isExactlyInstanceOf(InvalidInputException.class);
        assertThat(titleFailure).isExactlyInstanceOf(InvalidInputException.class);
        assertThat(contentFailure).isExactlyInstanceOf(InvalidInputException.class);
        assertThat(submitterFailure).isExactlyInstanceOf(InvalidInputException.class);
    }

    @Test
    void retrospectiveIsCreatedOpenWithTodaysLocalDateAndTeam() {
        // Arrange
        Team otherTeam = teamService.createTeam("Other", List.of("Carol"));
        LocalDate before = LocalDate.now();

        // Act
        Retrospective created = retrospectiveService.createRetrospective(otherTeam.getId(), "Sprint 2");
        LocalDate after = LocalDate.now();
        Retrospective stored = retrospectiveService.getRetrospectiveById(created.getId());

        // Assert
        assertThat(stored.getTitle()).isEqualTo("Sprint 2");
        assertThat(stored.getDate()).isBetween(before, after);
        assertThat(stored.getStatus()).isEqualTo(RetrospectiveStatus.OPEN);
        assertThat(stored.getTeam().getId()).isEqualTo(otherTeam.getId());
        assertThat(retrospectiveService.getRetrospectivesForTeam(otherTeam.getId()))
                .extracting(Retrospective::getId).containsExactly(created.getId());
    }

    @Test
    void emptyTeamHistoryIsDistinctFromMissingTeam() {
        // Arrange
        Team newTeam = teamService.createTeam("New", List.of("Alice"));

        // Act
        List<Retrospective> history = retrospectiveService.getRetrospectivesForTeam(newTeam.getId());
        Throwable missingTeamFailure = catchThrowable(() -> retrospectiveService.getRetrospectivesForTeam(MISSING_ID));

        // Assert
        assertThat(history).isEmpty();
        assertThat(missingTeamFailure).isExactlyInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void secondOpenRetrospectiveIsRejected() {
        // Arrange: setUp has already created the team's OPEN retrospective.
        Long teamId = team.getId();

        // Act
        Throwable exception = catchThrowable(() -> retrospectiveService.createRetrospective(teamId, "Duplicate"));

        // Assert
        assertThat(exception).isExactlyInstanceOf(OpenRetrospectiveExistsException.class);
    }

    @Test
    void newRetrospectiveAfterClosureIsAllowed() {
        // Arrange
        Retrospective closed = retrospectiveService.closeRetrospective(retrospective.getId());

        // Act
        Retrospective next = retrospectiveService.createRetrospective(team.getId(), "Next");

        // Assert
        assertThat(closed.getStatus()).isEqualTo(RetrospectiveStatus.CLOSED);
        assertThat(next.getStatus()).isEqualTo(RetrospectiveStatus.OPEN);
        assertThat(retrospectiveService.getRetrospectivesForTeam(team.getId()))
                .extracting(Retrospective::getId).containsExactly(retrospective.getId(), next.getId());
    }

    @Test
    void reopeningOpenRetrospectiveReturnsItUnchanged() {
        // Arrange
        Long retrospectiveId = retrospective.getId();

        // Act
        Retrospective unchanged = retrospectiveService.reopenRetrospective(retrospectiveId);

        // Assert
        assertThat(unchanged.getId()).isEqualTo(retrospectiveId);
        assertThat(unchanged.getStatus()).isEqualTo(RetrospectiveStatus.OPEN);
    }

    @Test
    void reopeningClosedRetrospectiveIsRejected() {
        // Arrange
        retrospectiveService.closeRetrospective(retrospective.getId());

        // Act
        Throwable exception = catchThrowable(() -> retrospectiveService.reopenRetrospective(retrospective.getId()));

        // Assert
        assertThat(exception).isExactlyInstanceOf(RetrospectiveReopeningNotAllowedException.class);
        assertThat(retrospectiveService.getRetrospectiveById(retrospective.getId()).getStatus())
                .isEqualTo(RetrospectiveStatus.CLOSED);
    }

    @Test
    void closingAlreadyClosedRetrospectiveThrowsConflict() {
        // Arrange: closure is deliberately non-idempotent.
        Long retrospectiveId = retrospective.getId();
        retrospectiveService.closeRetrospective(retrospectiveId);

        // Act
        RetrospectiveClosedException exception = catchThrowableOfType(
                () -> retrospectiveService.closeRetrospective(retrospectiveId), RetrospectiveClosedException.class);

        // Assert
        assertThat(exception)
                .isExactlyInstanceOf(RetrospectiveClosedException.class)
                .hasMessage("Retrospective " + retrospectiveId + " is CLOSED and cannot be modified");
        assertThat(exceptionHandler.handleBusinessRuleViolation(exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(retrospectiveService.getRetrospectiveById(retrospectiveId).getStatus())
                .isEqualTo(RetrospectiveStatus.CLOSED);
    }

    @Test
    void addingFeedbackToOpenRetrospectiveReturnsSavedItem() {
        // Arrange: setUp creates an OPEN retrospective with Alice as a team member.
        Long retrospectiveId = retrospective.getId();
        String content = "The team collaborated well";
        String submittedBy = "Alice";
        FeedbackType type = FeedbackType.WENT_WELL;

        // Act
        FeedbackItem saved = retrospectiveService.addFeedbackItem(
                retrospectiveId, content, type.name(), submittedBy);

        // Assert: both the returned item and a fresh read contain the persisted values.
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getContent()).isEqualTo(content);
        assertThat(saved.getType()).isEqualTo(type);
        assertThat(saved.getSubmittedBy()).isEqualTo(submittedBy);
        assertThat(saved.getRetrospective().getId()).isEqualTo(retrospectiveId);
        assertThat(retrospectiveService.getFeedbackItemsForRetrospective(retrospectiveId))
                .singleElement().satisfies(stored -> {
                    assertThat(stored.getId()).isEqualTo(saved.getId());
                    assertThat(stored.getContent()).isEqualTo(content);
                    assertThat(stored.getType()).isEqualTo(type);
                    assertThat(stored.getSubmittedBy()).isEqualTo(submittedBy);
                    assertThat(stored.getRetrospective().getId()).isEqualTo(retrospectiveId);
                });
        assertThat(retrospectiveService.getRetrospectiveById(retrospectiveId).getStatus())
                .isEqualTo(RetrospectiveStatus.OPEN);
    }

    @ParameterizedTest
    @EnumSource(value = FeedbackType.class, names = {"WENT_WELL", "NEEDS_IMPROVEMENT"})
    void regularFeedbackCanBeUpdatedWithoutChangingItsMetadata(FeedbackType type) {
        // Arrange
        FeedbackItem item = retrospectiveService.addFeedbackItem(
                retrospective.getId(), "Original", type.name(), "Alice");

        // Act
        FeedbackItem updated = retrospectiveService.updateFeedbackItem(item.getId(), "Updated");

        // Assert
        assertThat(updated.getId()).isEqualTo(item.getId());
        assertThat(updated.getType()).isEqualTo(type);
        assertThat(updated.getSubmittedBy()).isEqualTo("Alice");
        assertThat(updated.getRetrospective().getId()).isEqualTo(retrospective.getId());
        assertThat(retrospectiveService.getFeedbackItemsForRetrospective(retrospective.getId()))
                .extracting(FeedbackItem::getContent).containsExactly("Updated");
    }

    @ParameterizedTest
    @EnumSource(value = FeedbackType.class, names = {"WENT_WELL", "NEEDS_IMPROVEMENT"})
    void regularFeedbackCanBeDeletedOnlyOnce(FeedbackType type) {
        // Arrange
        FeedbackItem item = retrospectiveService.addFeedbackItem(
                retrospective.getId(), "Original", type.name(), "Alice");

        // Act
        retrospectiveService.deleteFeedbackItem(item.getId());
        Throwable secondDeletionFailure = catchThrowable(() -> retrospectiveService.deleteFeedbackItem(item.getId()));

        // Assert
        assertThat(retrospectiveService.getFeedbackItemsForRetrospective(retrospective.getId())).isEmpty();
        assertThat(secondDeletionFailure).isExactlyInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void actionItemsAreCreatedIncompleteWithActionType() {
        // Arrange
        String priority = "LOW";

        // Act
        ActionItem item = addAction(priority);

        // Assert
        assertThat(item.isCompleted()).isFalse();
        assertThat(item.getType()).isEqualTo(FeedbackType.ACTION_ITEM);
    }

    @Test
    void uncompletingIncompleteActionReturnsItUnchanged() {
        // Arrange
        ActionItem item = addAction("LOW");

        // Act
        ActionItem unchanged = retrospectiveService.uncompleteActionItem(item.getId());

        // Assert
        assertThat(unchanged.getId()).isEqualTo(item.getId());
        assertThat(unchanged.isCompleted()).isFalse();
    }

    @Test
    void completingActionTwiceReturnsCompletedItem() {
        // Arrange
        ActionItem item = addAction("LOW");

        // Act
        ActionItem completed = retrospectiveService.completeActionItem(item.getId());
        ActionItem unchanged = retrospectiveService.completeActionItem(item.getId());

        // Assert
        assertThat(completed.isCompleted()).isTrue();
        assertThat(unchanged.getId()).isEqualTo(item.getId());
        assertThat(unchanged.isCompleted()).isTrue();
    }

    @Test
    void actionUpdatesPreserveSubtypeSubmitterAndCompletion() {
        // Arrange
        ActionItem item = addAction("LOW");
        retrospectiveService.completeActionItem(item.getId());

        // Act
        ActionItem updated = retrospectiveService.updateActionItem(item.getId(), "Changed", "HIGH");
        FeedbackItem contentUpdated = retrospectiveService.updateFeedbackItem(item.getId(), "Changed again");
        ActionItem stored = retrospectiveService.getActionItems(retrospective.getId(), null, null).getFirst();

        // Assert
        assertThat(updated.isCompleted()).isTrue();
        assertThat(updated.getPriority()).isEqualTo(ActionPriority.HIGH);
        assertThat(updated.getSubmittedBy()).isEqualTo("Alice");
        assertThat(contentUpdated).isInstanceOf(ActionItem.class);
        assertThat(stored.getContent()).isEqualTo("Changed again");
        assertThat(stored.getType()).isEqualTo(FeedbackType.ACTION_ITEM);
        assertThat(stored.isCompleted()).isTrue();
        assertThat(stored.getPriority()).isEqualTo(ActionPriority.HIGH);
    }

    @Test
    void completedActionCannotBeUncompleted() {
        // Arrange
        ActionItem item = addAction("LOW");
        retrospectiveService.completeActionItem(item.getId());

        // Act
        Throwable exception = catchThrowable(() -> retrospectiveService.uncompleteActionItem(item.getId()));

        // Assert
        assertThat(exception).isExactlyInstanceOf(ActionItemUncompletionNotAllowedException.class);
        assertThat(retrospectiveService.getActionItems(retrospective.getId(), null, true)).hasSize(1);
    }

    @Test
    void completedActionCanBeDeletedFromOpenRetrospective() {
        // Arrange
        ActionItem item = addAction("LOW");
        retrospectiveService.completeActionItem(item.getId());

        // Act
        retrospectiveService.deleteFeedbackItem(item.getId());

        // Assert
        assertThat(retrospectiveService.getActionItems(retrospective.getId(), null, null)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"add-feedback", "add-action", "update-feedback", "update-action-content",
            "update-action", "delete-feedback", "delete-action", "complete-action",
            "complete-completed-action", "uncomplete-action", "uncomplete-completed-action"})
    void closedRetrospectiveRejectsEveryMutationWithoutChangingStoredItems(String operation) {
        // Arrange
        FeedbackItem feedback = retrospectiveService.addFeedbackItem(
                retrospective.getId(), "Original", "WENT_WELL", "Alice");
        ActionItem action = addAction("LOW");
        ActionItem completed = addAction("HIGH");
        retrospectiveService.completeActionItem(completed.getId());
        retrospectiveService.closeRetrospective(retrospective.getId());

        // Act
        Throwable exception = catchThrowable(() -> {
            switch (operation) {
                case "add-feedback" -> retrospectiveService.addFeedbackItem(
                        retrospective.getId(), "New", "WENT_WELL", "Alice");
                case "add-action" -> addAction("MEDIUM");
                case "update-feedback" -> retrospectiveService.updateFeedbackItem(feedback.getId(), "New");
                case "update-action-content" -> retrospectiveService.updateFeedbackItem(action.getId(), "New");
                case "update-action" -> retrospectiveService.updateActionItem(action.getId(), "New", "HIGH");
                case "delete-feedback" -> retrospectiveService.deleteFeedbackItem(feedback.getId());
                case "delete-action" -> retrospectiveService.deleteFeedbackItem(action.getId());
                case "complete-action" -> retrospectiveService.completeActionItem(action.getId());
                case "complete-completed-action" -> retrospectiveService.completeActionItem(completed.getId());
                case "uncomplete-action" -> retrospectiveService.uncompleteActionItem(action.getId());
                case "uncomplete-completed-action" -> retrospectiveService.uncompleteActionItem(completed.getId());
                default -> throw new AssertionError("Unknown operation: " + operation);
            }
        });

        // Assert
        assertThat(exception).isExactlyInstanceOf(RetrospectiveClosedException.class);
        assertThat(retrospectiveService.getFeedbackItemsForRetrospective(retrospective.getId()))
                .extracting(FeedbackItem::getContent).containsExactly("Original", "Fix", "Fix");
        assertThat(retrospectiveService.getActionItems(retrospective.getId(), "LOW", false))
                .extracting(ActionItem::getId).containsExactly(action.getId());
        assertThat(retrospectiveService.getActionItems(retrospective.getId(), "HIGH", true))
                .extracting(ActionItem::getId).containsExactly(completed.getId());
    }

    @ParameterizedTest
    @EnumSource(ActionPriority.class)
    void filteringByPriorityReturnsOnlyMatchingActionItems(ActionPriority priority) {
        // Arrange: include other priorities and both completion states for the requested priority.
        ActionItem matchingIncomplete = addAction(priority.name());
        ActionItem matchingCompleted = addAction(priority.name());
        retrospectiveService.completeActionItem(matchingCompleted.getId());
        for (ActionPriority otherPriority : ActionPriority.values()) {
            if (otherPriority != priority) {
                addAction(otherPriority.name());
            }
        }

        // Act
        List<ActionItem> filtered = retrospectiveService.getActionItems(
                retrospective.getId(), priority.name(), null);

        // Assert: exact IDs also prevent an empty or incomplete result from passing.
        assertThat(filtered).allSatisfy(item -> assertThat(item.getPriority()).isEqualTo(priority));
        assertThat(filtered).extracting(ActionItem::getId)
                .containsExactlyInAnyOrder(matchingIncomplete.getId(), matchingCompleted.getId());
    }

    @Test
    void filteringByCompletedTrueReturnsOnlyCompletedActionItems() {
        // Arrange: mix completed and incomplete items across different priorities.
        ActionItem completedLow = addAction("LOW");
        ActionItem completedHigh = addAction("HIGH");
        retrospectiveService.completeActionItem(completedLow.getId());
        retrospectiveService.completeActionItem(completedHigh.getId());
        addAction("LOW");
        addAction("MEDIUM");

        // Act
        List<ActionItem> filtered = retrospectiveService.getActionItems(retrospective.getId(), null, true);

        // Assert
        assertThat(filtered).allMatch(ActionItem::isCompleted);
        assertThat(filtered).extracting(ActionItem::getId)
                .containsExactlyInAnyOrder(completedLow.getId(), completedHigh.getId());
    }

    @ParameterizedTest
    @MethodSource("actionFilters")
    void filtersAreOptionalCombinedWithAndAndScopedToRetrospective(String priority, Boolean completed) {
        // Arrange
        List<ActionItem> actions = new ArrayList<>();
        for (ActionPriority value : ActionPriority.values()) {
            actions.add(addAction(value.name()));
            ActionItem done = addAction(value.name());
            actions.add(retrospectiveService.completeActionItem(done.getId()));
        }
        retrospectiveService.addFeedbackItem(retrospective.getId(), "Not an action", "WENT_WELL", "Alice");
        Team other = teamService.createTeam("Other", List.of("Alice"));
        Retrospective otherRetro = retrospectiveService.createRetrospective(other.getId(), "Other");
        retrospectiveService.addActionItem(otherRetro.getId(), "Excluded", "HIGH", "Alice");
        retrospectiveService.closeRetrospective(retrospective.getId());

        List<Long> expectedIds = actions.stream()
                .filter(action -> priority == null || action.getPriority().name().equals(priority))
                .filter(action -> completed == null || action.isCompleted() == completed)
                .map(ActionItem::getId)
                .toList();

        // Act
        List<ActionItem> filtered = retrospectiveService.getActionItems(retrospective.getId(), priority, completed);

        // Assert
        assertThat(filtered)
                .extracting(ActionItem::getId).containsExactlyElementsOf(expectedIds);
    }

    static Stream<Arguments> actionFilters() {
        return Stream.of(null, "LOW", "MEDIUM", "HIGH")
                .flatMap(priority -> Stream.of(null, Boolean.FALSE, Boolean.TRUE)
                        .map(completed -> Arguments.of(priority, completed)));
    }

    @Test
    void existingRetrospectiveWithNoItemsReturnsEmptyLists() {
        // Arrange: setUp creates the retrospective without feedback.
        Long retrospectiveId = retrospective.getId();

        // Act
        List<FeedbackItem> feedback = retrospectiveService.getFeedbackItemsForRetrospective(retrospectiveId);
        List<ActionItem> actions = retrospectiveService.getActionItems(retrospectiveId, null, null);

        // Assert
        assertThat(feedback).isEmpty();
        assertThat(actions).isEmpty();
    }

    @Test
    void filtersWithNoMatchingActionItemsReturnEmptyList() {
        // Arrange
        addAction("LOW");

        // Act
        List<ActionItem> filtered = retrospectiveService.getActionItems(retrospective.getId(), "HIGH", true);

        // Assert
        assertThat(filtered).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Carol", "alice", " Alice", "Alice "})
    void bothKindsOfFeedbackRequireExactTeamMembership(String submitter) {
        // Arrange: the parameter is not an exact match for Alice or Bob in the fixture team.
        Long retrospectiveId = retrospective.getId();

        // Act
        Throwable feedbackFailure = catchThrowable(() -> retrospectiveService.addFeedbackItem(
                retrospectiveId, "Feedback", "WENT_WELL", submitter));
        Throwable actionFailure = catchThrowable(() -> retrospectiveService.addActionItem(
                retrospectiveId, "Fix", "HIGH", submitter));

        // Assert
        assertThat(feedbackFailure).isExactlyInstanceOf(SubmitterNotTeamMemberException.class);
        assertThat(actionFailure).isExactlyInstanceOf(SubmitterNotTeamMemberException.class);
        assertThat(retrospectiveService.getFeedbackItemsForRetrospective(retrospective.getId())).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"ACTION_ITEM", "went_well", "INVALID"})
    void invalidFeedbackTypesCannotCreateMalformedItems(String type) {
        // Arrange
        Long retrospectiveId = retrospective.getId();

        // Act
        Throwable exception = catchThrowable(() -> retrospectiveService.addFeedbackItem(
                retrospectiveId, "Feedback", type, "Alice"));

        // Assert
        assertThat(exception).isExactlyInstanceOf(InvalidFeedbackTypeException.class);
        assertThat(retrospectiveService.getFeedbackItemsForRetrospective(retrospective.getId())).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"high", " HIGH ", "URGENT"})
    void priorityIsRequiredAndMustBeAnExactEnumValue(String priority) {
        // Arrange
        ActionItem item = addAction("LOW");

        // Act
        Throwable creationFailure = catchThrowable(() -> addAction(priority));
        Throwable updateFailure = catchThrowable(() ->
                retrospectiveService.updateActionItem(item.getId(), "New", priority));

        // Assert
        assertThat(creationFailure).isExactlyInstanceOf(InvalidActionPriorityException.class);
        assertThat(updateFailure).isExactlyInstanceOf(InvalidActionPriorityException.class);
        assertThat(retrospectiveService.getActionItems(retrospective.getId(), null, null))
                .extracting(ActionItem::getPriority).containsExactly(ActionPriority.LOW);
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {"high", " HIGH ", "URGENT"})
    void invalidPriorityFilterIsRejected(String priority) {
        // Arrange
        Long retrospectiveId = retrospective.getId();

        // Act
        Throwable exception = catchThrowable(() -> retrospectiveService.getActionItems(retrospectiveId, priority, null));

        // Assert
        assertThat(exception).isExactlyInstanceOf(InvalidActionPriorityException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\n"})
    void textIsValidatedOnEveryWrite(String text) {
        // Arrange
        ActionItem item = addAction("HIGH");

        // Act
        Throwable titleFailure = catchThrowable(() -> retrospectiveService.createRetrospective(team.getId(), text));
        Throwable feedbackContentFailure = catchThrowable(() -> retrospectiveService.addFeedbackItem(
                retrospective.getId(), text, "WENT_WELL", "Alice"));
        Throwable feedbackSubmitterFailure = catchThrowable(() -> retrospectiveService.addFeedbackItem(
                retrospective.getId(), "Feedback", "WENT_WELL", text));
        Throwable actionContentFailure = catchThrowable(() -> retrospectiveService.addActionItem(
                retrospective.getId(), text, "HIGH", "Alice"));
        Throwable actionSubmitterFailure = catchThrowable(() -> retrospectiveService.addActionItem(
                retrospective.getId(), "Fix", "HIGH", text));
        Throwable feedbackUpdateFailure = catchThrowable(() -> retrospectiveService.updateFeedbackItem(item.getId(), text));
        Throwable actionUpdateFailure = catchThrowable(() -> retrospectiveService.updateActionItem(item.getId(), text, "LOW"));

        // Assert
        assertThat(titleFailure).isExactlyInstanceOf(InvalidInputException.class);
        assertThat(feedbackContentFailure).isExactlyInstanceOf(InvalidInputException.class);
        assertThat(feedbackSubmitterFailure).isExactlyInstanceOf(InvalidInputException.class);
        assertThat(actionContentFailure).isExactlyInstanceOf(InvalidInputException.class);
        assertThat(actionSubmitterFailure).isExactlyInstanceOf(InvalidInputException.class);
        assertThat(feedbackUpdateFailure).isExactlyInstanceOf(InvalidInputException.class);
        assertThat(actionUpdateFailure).isExactlyInstanceOf(InvalidInputException.class);
    }

    @Test
    void regularFeedbackCannotBeUsedAsAnActionItem() {
        // Arrange
        FeedbackItem item = retrospectiveService.addFeedbackItem(
                retrospective.getId(), "Feedback", "WENT_WELL", "Alice");

        // Act
        Throwable completionFailure = catchThrowable(() -> retrospectiveService.completeActionItem(item.getId()));
        Throwable uncompletionFailure = catchThrowable(() -> retrospectiveService.uncompleteActionItem(item.getId()));
        Throwable updateFailure = catchThrowable(() -> retrospectiveService.updateActionItem(item.getId(), "New", "LOW"));

        // Assert
        assertThat(completionFailure)
                .isExactlyInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Action item " + item.getId() + " not found");
        assertThat(uncompletionFailure)
                .isExactlyInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Action item " + item.getId() + " not found");
        assertThat(updateFailure)
                .isExactlyInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Action item " + item.getId() + " not found");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0, -1, MISSING_ID})
    void missingAndInvalidIdsAreExplicitErrors(Long id) {
        // Arrange
        Class<? extends RuntimeException> error = id != null && id > 0
                ? ResourceNotFoundException.class : InvalidInputException.class;

        // Act
        Throwable teamFailure = catchThrowable(() -> teamService.getTeamById(id));
        Throwable creationFailure = catchThrowable(() -> retrospectiveService.createRetrospective(id, "Sprint"));
        Throwable retrospectiveFailure = catchThrowable(() -> retrospectiveService.getRetrospectiveById(id));
        Throwable historyFailure = catchThrowable(() -> retrospectiveService.getRetrospectivesForTeam(id));
        Throwable closureFailure = catchThrowable(() -> retrospectiveService.closeRetrospective(id));
        Throwable reopeningFailure = catchThrowable(() -> retrospectiveService.reopenRetrospective(id));
        Throwable feedbackCreationFailure = catchThrowable(() ->
                retrospectiveService.addFeedbackItem(id, "Feedback", "WENT_WELL", "Alice"));
        Throwable actionCreationFailure = catchThrowable(() ->
                retrospectiveService.addActionItem(id, "Fix", "HIGH", "Alice"));
        Throwable feedbackListFailure = catchThrowable(() -> retrospectiveService.getFeedbackItemsForRetrospective(id));
        Throwable actionListFailure = catchThrowable(() -> retrospectiveService.getActionItems(id, null, null));
        Throwable feedbackUpdateFailure = catchThrowable(() -> retrospectiveService.updateFeedbackItem(id, "New"));
        Throwable actionUpdateFailure = catchThrowable(() -> retrospectiveService.updateActionItem(id, "New", "LOW"));
        Throwable deletionFailure = catchThrowable(() -> retrospectiveService.deleteFeedbackItem(id));
        Throwable completionFailure = catchThrowable(() -> retrospectiveService.completeActionItem(id));
        Throwable uncompletionFailure = catchThrowable(() -> retrospectiveService.uncompleteActionItem(id));

        // Assert
        assertThat(teamFailure).isExactlyInstanceOf(error);
        assertThat(creationFailure).isExactlyInstanceOf(error);
        assertThat(retrospectiveFailure).isExactlyInstanceOf(error);
        assertThat(historyFailure).isExactlyInstanceOf(error);
        assertThat(closureFailure).isExactlyInstanceOf(error);
        assertThat(reopeningFailure).isExactlyInstanceOf(error);
        assertThat(feedbackCreationFailure).isExactlyInstanceOf(error);
        assertThat(actionCreationFailure).isExactlyInstanceOf(error);
        assertThat(feedbackListFailure).isExactlyInstanceOf(error);
        assertThat(actionListFailure).isExactlyInstanceOf(error);
        assertThat(feedbackUpdateFailure).isExactlyInstanceOf(error);
        assertThat(actionUpdateFailure).isExactlyInstanceOf(error);
        assertThat(deletionFailure).isExactlyInstanceOf(error);
        assertThat(completionFailure).isExactlyInstanceOf(error);
        assertThat(uncompletionFailure).isExactlyInstanceOf(error);
    }

    private ActionItem addAction(String priority) {
        return retrospectiveService.addActionItem(retrospective.getId(), "Fix", priority, "Alice");
    }
}
