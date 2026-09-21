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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ServiceLayerTest {

    private static final long MISSING_ID = Long.MAX_VALUE;

    private final TeamService teamService;
    private final RetrospectiveService retrospectiveService;
    private Team team;
    private Retrospective retrospective;

    @Autowired
    ServiceLayerTest(TeamService teamService, RetrospectiveService retrospectiveService) {
        this.teamService = teamService;
        this.retrospectiveService = retrospectiveService;
    }

    @BeforeEach
    void setUp() {
        team = teamService.createTeam("Team", List.of("Alice", "Bob"));
        retrospective = retrospectiveService.createRetrospective(team.getId(), "Sprint");
    }

    @Test
    void teamsArePersistedWithCopiedDistinctMembersAndMayShareNames() {
        List<String> members = new ArrayList<>(List.of("Alice", "Alice", "Bob"));
        Team created = teamService.createTeam("Team", members);
        members.clear();

        Team stored = teamService.getTeamById(created.getId());
        assertThat(stored.getId()).isNotEqualTo(team.getId());
        assertThat(stored.getName()).isEqualTo("Team");
        assertThat(stored.getMembers()).containsExactlyInAnyOrder("Alice", "Bob");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void teamsRequireMembers(List<String> members) {
        assertThatThrownBy(() -> teamService.createTeam("Team", members))
                .isExactlyInstanceOf(TeamMustHaveMembersException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void namesAndMemberNamesMustBeNonblank(String name) {
        assertThatThrownBy(() -> teamService.createTeam(name, List.of("Alice")))
                .isExactlyInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> teamService.createTeam("Team", Arrays.asList(name)))
                .isExactlyInstanceOf(InvalidInputException.class);
    }

    @Test
    void serviceEnforcesTextLengthsWithoutRelyingOnPersistenceValidation() {
        assertThatThrownBy(() -> teamService.createTeam("x".repeat(101), List.of("Alice")))
                .isExactlyInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> teamService.createTeam("Team", List.of("x".repeat(101))))
                .isExactlyInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> retrospectiveService.createRetrospective(team.getId(), "x".repeat(201)))
                .isExactlyInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> retrospectiveService.addFeedbackItem(
                retrospective.getId(), "x".repeat(5001), "WENT_WELL", "Alice"))
                .isExactlyInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> retrospectiveService.addActionItem(
                retrospective.getId(), "Fix", "HIGH", "x".repeat(101)))
                .isExactlyInstanceOf(InvalidInputException.class);
    }

    @Test
    void retrospectiveIsCreatedOpenWithTodaysLocalDateAndTeam() {
        LocalDate before = LocalDate.now();
        Team otherTeam = teamService.createTeam("Other", List.of("Carol"));
        Retrospective created = retrospectiveService.createRetrospective(otherTeam.getId(), "Sprint 2");
        LocalDate after = LocalDate.now();

        Retrospective stored = retrospectiveService.getRetrospectiveById(created.getId());
        assertThat(stored.getTitle()).isEqualTo("Sprint 2");
        assertThat(stored.getDate()).isBetween(before, after);
        assertThat(stored.getStatus()).isEqualTo(RetrospectiveStatus.OPEN);
        assertThat(stored.getTeam().getId()).isEqualTo(otherTeam.getId());
        assertThat(retrospectiveService.getRetrospectivesForTeam(otherTeam.getId()))
                .extracting(Retrospective::getId).containsExactly(created.getId());
    }

    @Test
    void emptyTeamHistoryIsDistinctFromMissingTeam() {
        Team newTeam = teamService.createTeam("New", List.of("Alice"));
        assertThat(retrospectiveService.getRetrospectivesForTeam(newTeam.getId())).isEmpty();
        assertThatThrownBy(() -> retrospectiveService.getRetrospectivesForTeam(MISSING_ID))
                .isExactlyInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void secondOpenRetrospectiveIsRejectedButNewOneAfterClosureIsAllowed() {
        assertThatThrownBy(() -> retrospectiveService.createRetrospective(team.getId(), "Duplicate"))
                .isExactlyInstanceOf(OpenRetrospectiveExistsException.class);
        assertThat(retrospectiveService.closeRetrospective(retrospective.getId()).getStatus())
                .isEqualTo(RetrospectiveStatus.CLOSED);
        Retrospective next = retrospectiveService.createRetrospective(team.getId(), "Next");
        assertThat(next.getStatus()).isEqualTo(RetrospectiveStatus.OPEN);
        assertThat(retrospectiveService.getRetrospectivesForTeam(team.getId()))
                .extracting(Retrospective::getId).containsExactly(retrospective.getId(), next.getId());
    }

    @Test
    void reopeningClosedRetrospectiveAndClosingTwiceHaveSpecificErrors() {
        assertThat(retrospectiveService.reopenRetrospective(retrospective.getId()).getStatus())
                .isEqualTo(RetrospectiveStatus.OPEN);
        retrospectiveService.closeRetrospective(retrospective.getId());

        assertThatThrownBy(() -> retrospectiveService.closeRetrospective(retrospective.getId()))
                .isExactlyInstanceOf(RetrospectiveClosedException.class);
        assertThatThrownBy(() -> retrospectiveService.reopenRetrospective(retrospective.getId()))
                .isExactlyInstanceOf(RetrospectiveReopeningNotAllowedException.class);
        assertThat(retrospectiveService.getRetrospectiveById(retrospective.getId()).getStatus())
                .isEqualTo(RetrospectiveStatus.CLOSED);
    }

    @ParameterizedTest
    @EnumSource(value = FeedbackType.class, names = {"WENT_WELL", "NEEDS_IMPROVEMENT"})
    void regularFeedbackCanBeCreatedReadUpdatedAndDeleted(FeedbackType type) {
        FeedbackItem item = retrospectiveService.addFeedbackItem(
                retrospective.getId(), "Original", type.name(), "Alice");
        FeedbackItem updated = retrospectiveService.updateFeedbackItem(item.getId(), "Updated");

        assertThat(updated.getId()).isEqualTo(item.getId());
        assertThat(updated.getType()).isEqualTo(type);
        assertThat(updated.getSubmittedBy()).isEqualTo("Alice");
        assertThat(updated.getRetrospective().getId()).isEqualTo(retrospective.getId());
        assertThat(retrospectiveService.getFeedbackItemsForRetrospective(retrospective.getId()))
                .extracting(FeedbackItem::getContent).containsExactly("Updated");

        retrospectiveService.deleteFeedbackItem(item.getId());
        assertThat(retrospectiveService.getFeedbackItemsForRetrospective(retrospective.getId())).isEmpty();
        assertThatThrownBy(() -> retrospectiveService.deleteFeedbackItem(item.getId()))
                .isExactlyInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void actionUpdatesPreserveSubtypeSubmitterAndCompletionAndCanBeDeleted() {
        ActionItem item = addAction("LOW");
        assertThat(item.isCompleted()).isFalse();
        assertThat(item.getType()).isEqualTo(FeedbackType.ACTION_ITEM);
        assertThat(retrospectiveService.uncompleteActionItem(item.getId()).isCompleted()).isFalse();
        assertThat(retrospectiveService.completeActionItem(item.getId()).isCompleted()).isTrue();
        assertThat(retrospectiveService.completeActionItem(item.getId()).isCompleted()).isTrue();

        ActionItem updated = retrospectiveService.updateActionItem(item.getId(), "Changed", "HIGH");
        assertThat(updated.isCompleted()).isTrue();
        assertThat(updated.getPriority()).isEqualTo(ActionPriority.HIGH);
        assertThat(updated.getSubmittedBy()).isEqualTo("Alice");
        assertThat(retrospectiveService.updateFeedbackItem(item.getId(), "Changed again"))
                .isInstanceOf(ActionItem.class);
        ActionItem stored = retrospectiveService.getActionItems(retrospective.getId(), null, null).getFirst();
        assertThat(stored.getContent()).isEqualTo("Changed again");
        assertThat(stored.getType()).isEqualTo(FeedbackType.ACTION_ITEM);
        assertThat(stored.isCompleted()).isTrue();
        assertThat(stored.getPriority()).isEqualTo(ActionPriority.HIGH);

        assertThatThrownBy(() -> retrospectiveService.uncompleteActionItem(item.getId()))
                .isExactlyInstanceOf(ActionItemUncompletionNotAllowedException.class);
        assertThat(retrospectiveService.getActionItems(retrospective.getId(), null, true)).hasSize(1);
        retrospectiveService.deleteFeedbackItem(item.getId());
        assertThat(retrospectiveService.getActionItems(retrospective.getId(), null, null)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"add-feedback", "add-action", "update-feedback", "update-action-content",
            "update-action", "delete-feedback", "delete-action", "complete-action",
            "complete-completed-action", "uncomplete-action", "uncomplete-completed-action"})
    void closedRetrospectiveRejectsEveryMutationWithoutChangingStoredItems(String operation) {
        FeedbackItem feedback = retrospectiveService.addFeedbackItem(
                retrospective.getId(), "Original", "WENT_WELL", "Alice");
        ActionItem action = addAction("LOW");
        ActionItem completed = addAction("HIGH");
        retrospectiveService.completeActionItem(completed.getId());
        retrospectiveService.closeRetrospective(retrospective.getId());

        assertThatThrownBy(() -> {
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
        }).isExactlyInstanceOf(RetrospectiveClosedException.class);

        assertThat(retrospectiveService.getFeedbackItemsForRetrospective(retrospective.getId()))
                .extracting(FeedbackItem::getContent).containsExactly("Original", "Fix", "Fix");
        assertThat(retrospectiveService.getActionItems(retrospective.getId(), "LOW", false))
                .extracting(ActionItem::getId).containsExactly(action.getId());
        assertThat(retrospectiveService.getActionItems(retrospective.getId(), "HIGH", true))
                .extracting(ActionItem::getId).containsExactly(completed.getId());
    }

    @ParameterizedTest
    @MethodSource("actionFilters")
    void filtersAreOptionalCombinedWithAndAndScopedToRetrospective(String priority, Boolean completed) {
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
        assertThat(retrospectiveService.getActionItems(retrospective.getId(), priority, completed))
                .extracting(ActionItem::getId).containsExactlyElementsOf(expectedIds);
    }

    static Stream<Arguments> actionFilters() {
        return Stream.of(null, "LOW", "MEDIUM", "HIGH")
                .flatMap(priority -> Stream.of(null, Boolean.FALSE, Boolean.TRUE)
                        .map(completed -> Arguments.of(priority, completed)));
    }

    @Test
    void existingRetrospectiveWithNoItemsReturnsEmptyLists() {
        assertThat(retrospectiveService.getFeedbackItemsForRetrospective(retrospective.getId())).isEmpty();
        assertThat(retrospectiveService.getActionItems(retrospective.getId(), null, null)).isEmpty();
        addAction("LOW");
        assertThat(retrospectiveService.getActionItems(retrospective.getId(), "HIGH", true)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Carol", "alice", " Alice", "Alice "})
    void bothKindsOfFeedbackRequireExactTeamMembership(String submitter) {
        assertThatThrownBy(() -> retrospectiveService.addFeedbackItem(
                retrospective.getId(), "Feedback", "WENT_WELL", submitter))
                .isExactlyInstanceOf(SubmitterNotTeamMemberException.class);
        assertThatThrownBy(() -> retrospectiveService.addActionItem(
                retrospective.getId(), "Fix", "HIGH", submitter))
                .isExactlyInstanceOf(SubmitterNotTeamMemberException.class);
        assertThat(retrospectiveService.getFeedbackItemsForRetrospective(retrospective.getId())).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"ACTION_ITEM", "went_well", "INVALID"})
    void invalidFeedbackTypesCannotCreateMalformedItems(String type) {
        assertThatThrownBy(() -> retrospectiveService.addFeedbackItem(
                retrospective.getId(), "Feedback", type, "Alice"))
                .isExactlyInstanceOf(InvalidFeedbackTypeException.class);
        assertThat(retrospectiveService.getFeedbackItemsForRetrospective(retrospective.getId())).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"high", " HIGH ", "URGENT"})
    void priorityIsRequiredAndMustBeAnExactEnumValue(String priority) {
        ActionItem item = addAction("LOW");
        assertThatThrownBy(() -> addAction(priority)).isExactlyInstanceOf(InvalidActionPriorityException.class);
        assertThatThrownBy(() -> retrospectiveService.updateActionItem(item.getId(), "New", priority))
                .isExactlyInstanceOf(InvalidActionPriorityException.class);
        if (priority != null) {
            assertThatThrownBy(() -> retrospectiveService.getActionItems(retrospective.getId(), priority, null))
                    .isExactlyInstanceOf(InvalidActionPriorityException.class);
        }
        assertThat(retrospectiveService.getActionItems(retrospective.getId(), null, null))
                .extracting(ActionItem::getPriority).containsExactly(ActionPriority.LOW);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\n"})
    void textIsValidatedOnEveryWrite(String text) {
        ActionItem item = addAction("HIGH");
        assertThatThrownBy(() -> retrospectiveService.createRetrospective(team.getId(), text))
                .isExactlyInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> retrospectiveService.addFeedbackItem(
                retrospective.getId(), text, "WENT_WELL", "Alice"))
                .isExactlyInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> retrospectiveService.addFeedbackItem(
                retrospective.getId(), "Feedback", "WENT_WELL", text))
                .isExactlyInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> retrospectiveService.addActionItem(
                retrospective.getId(), text, "HIGH", "Alice"))
                .isExactlyInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> retrospectiveService.addActionItem(
                retrospective.getId(), "Fix", "HIGH", text))
                .isExactlyInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> retrospectiveService.updateFeedbackItem(item.getId(), text))
                .isExactlyInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> retrospectiveService.updateActionItem(item.getId(), text, "LOW"))
                .isExactlyInstanceOf(InvalidInputException.class);
    }

    @Test
    void regularFeedbackCannotBeUsedAsAnActionItem() {
        FeedbackItem item = retrospectiveService.addFeedbackItem(
                retrospective.getId(), "Feedback", "WENT_WELL", "Alice");
        assertThatThrownBy(() -> retrospectiveService.completeActionItem(item.getId()))
                .isExactlyInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Action item " + item.getId() + " not found");
        assertThatThrownBy(() -> retrospectiveService.uncompleteActionItem(item.getId()))
                .isExactlyInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Action item " + item.getId() + " not found");
        assertThatThrownBy(() -> retrospectiveService.updateActionItem(item.getId(), "New", "LOW"))
                .isExactlyInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Action item " + item.getId() + " not found");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0, -1, MISSING_ID})
    void missingAndInvalidIdsAreExplicitErrors(Long id) {
        Class<? extends RuntimeException> error = id != null && id > 0
                ? ResourceNotFoundException.class : InvalidInputException.class;
        assertThatThrownBy(() -> teamService.getTeamById(id)).isExactlyInstanceOf(error);
        assertThatThrownBy(() -> retrospectiveService.createRetrospective(id, "Sprint")).isExactlyInstanceOf(error);
        assertThatThrownBy(() -> retrospectiveService.getRetrospectiveById(id)).isExactlyInstanceOf(error);
        assertThatThrownBy(() -> retrospectiveService.getRetrospectivesForTeam(id)).isExactlyInstanceOf(error);
        assertThatThrownBy(() -> retrospectiveService.closeRetrospective(id)).isExactlyInstanceOf(error);
        assertThatThrownBy(() -> retrospectiveService.reopenRetrospective(id)).isExactlyInstanceOf(error);
        assertThatThrownBy(() -> retrospectiveService.addFeedbackItem(id, "Feedback", "WENT_WELL", "Alice"))
                .isExactlyInstanceOf(error);
        assertThatThrownBy(() -> retrospectiveService.addActionItem(id, "Fix", "HIGH", "Alice"))
                .isExactlyInstanceOf(error);
        assertThatThrownBy(() -> retrospectiveService.getFeedbackItemsForRetrospective(id)).isExactlyInstanceOf(error);
        assertThatThrownBy(() -> retrospectiveService.getActionItems(id, null, null)).isExactlyInstanceOf(error);
        assertThatThrownBy(() -> retrospectiveService.updateFeedbackItem(id, "New")).isExactlyInstanceOf(error);
        assertThatThrownBy(() -> retrospectiveService.updateActionItem(id, "New", "LOW")).isExactlyInstanceOf(error);
        assertThatThrownBy(() -> retrospectiveService.deleteFeedbackItem(id)).isExactlyInstanceOf(error);
        assertThatThrownBy(() -> retrospectiveService.completeActionItem(id)).isExactlyInstanceOf(error);
        assertThatThrownBy(() -> retrospectiveService.uncompleteActionItem(id)).isExactlyInstanceOf(error);
    }

    @Test
    void concurrentCreatesProduceExactlyOneOpenRetrospective() throws Exception {
        Team newTeam = teamService.createTeam("Concurrent", List.of("Alice"));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> create = () -> {
            ready.countDown();
            assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
            try {
                retrospectiveService.createRetrospective(newTeam.getId(), "Concurrent");
                return true;
            } catch (OpenRetrospectiveExistsException ex) {
                return false;
            }
        };

        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(create);
            var second = executor.submit(create);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
        assertThat(retrospectiveService.getRetrospectivesForTeam(newTeam.getId()))
                .extracting(Retrospective::getStatus).containsExactly(RetrospectiveStatus.OPEN);
    }

    private ActionItem addAction(String priority) {
        return retrospectiveService.addActionItem(retrospective.getId(), "Fix", priority, "Alice");
    }
}
