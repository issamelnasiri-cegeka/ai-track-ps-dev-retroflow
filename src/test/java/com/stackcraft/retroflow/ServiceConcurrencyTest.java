package com.stackcraft.retroflow;

import com.stackcraft.retroflow.entity.ActionItem;
import com.stackcraft.retroflow.entity.FeedbackItem;
import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.entity.RetrospectiveStatus;
import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.exception.OpenRetrospectiveExistsException;
import com.stackcraft.retroflow.exception.RetrospectiveClosedException;
import com.stackcraft.retroflow.service.RetrospectiveService;
import com.stackcraft.retroflow.service.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@SpringBootTest
class ServiceConcurrencyTest {

    private final TeamService teamService;
    private final RetrospectiveService retrospectiveService;
    private final TransactionTemplate transaction;

    @Autowired
    ServiceConcurrencyTest(TeamService teamService, RetrospectiveService retrospectiveService,
                           PlatformTransactionManager transactionManager) {
        this.teamService = teamService;
        this.retrospectiveService = retrospectiveService;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    @ParameterizedTest
    @ValueSource(strings = {"add-feedback", "add-action", "update", "delete", "complete", "uncomplete"})
    void mutationsWaitForConcurrentClosureAndThenRejectIt(String operation) throws Exception {
        // Arrange
        Team team = teamService.createTeam("Concurrent closure", List.of("Alice"));
        Retrospective retro = retrospectiveService.createRetrospective(team.getId(), "Sprint");
        ActionItem action = retrospectiveService.addActionItem(retro.getId(), "Fix", "LOW", "Alice");
        CountDownLatch closedInTransaction = new CountDownLatch(1);
        CountDownLatch commitClosure = new CountDownLatch(1);
        CountDownLatch mutationStarted = new CountDownLatch(1);

        var executor = Executors.newFixedThreadPool(2);
        Throwable waitingFailure;
        Throwable mutationFailure;

        // Act: observe the blocked mutation, then release the closure transaction.
        try {
            var closure = executor.submit(() -> transaction.executeWithoutResult(status -> {
                retrospectiveService.closeRetrospective(retro.getId());
                closedInTransaction.countDown();
                await(commitClosure);
            }));
            await(closedInTransaction);
            var mutation = executor.submit(() -> {
                mutationStarted.countDown();
                switch (operation) {
                    case "add-feedback" -> retrospectiveService.addFeedbackItem(
                            retro.getId(), "Feedback", "WENT_WELL", "Alice");
                    case "add-action" -> retrospectiveService.addActionItem(
                            retro.getId(), "New", "HIGH", "Alice");
                    case "update" -> retrospectiveService.updateActionItem(action.getId(), "Changed", "HIGH");
                    case "delete" -> retrospectiveService.deleteFeedbackItem(action.getId());
                    case "complete" -> retrospectiveService.completeActionItem(action.getId());
                    case "uncomplete" -> retrospectiveService.uncompleteActionItem(action.getId());
                    default -> throw new AssertionError("Unknown operation: " + operation);
                }
            });
            await(mutationStarted);
            waitingFailure = catchThrowable(() -> mutation.get(200, TimeUnit.MILLISECONDS));
            commitClosure.countDown();
            closure.get(5, TimeUnit.SECONDS);
            mutationFailure = catchThrowable(() -> mutation.get(5, TimeUnit.SECONDS));
        } finally {
            commitClosure.countDown();
            executor.shutdownNow();
        }

        // Assert
        assertThat(waitingFailure).isExactlyInstanceOf(TimeoutException.class);
        assertThat(mutationFailure).hasCauseInstanceOf(RetrospectiveClosedException.class);
        assertThat(retrospectiveService.getActionItems(retro.getId(), "LOW", false))
                .extracting(ActionItem::getId).containsExactly(action.getId());
        assertThat(retrospectiveService.getFeedbackItemsForRetrospective(retro.getId()))
                .extracting(FeedbackItem::getContent).containsExactly("Fix");
    }

    @Test
    void concurrentCreatesProduceExactlyOneOpenRetrospective() throws Exception {
        // Arrange
        Team team = teamService.createTeam("Concurrent", List.of("Alice"));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> create = () -> {
            ready.countDown();
            await(start);
            try {
                retrospectiveService.createRetrospective(team.getId(), "Concurrent");
                return true;
            } catch (OpenRetrospectiveExistsException ex) {
                return false;
            }
        };
        var executor = Executors.newFixedThreadPool(2);
        List<Boolean> outcomes;

        // Act
        try {
            var first = executor.submit(create);
            var second = executor.submit(create);
            await(ready);
            start.countDown();
            outcomes = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
        } finally {
            start.countDown();
            executor.shutdownNow();
        }

        // Assert
        assertThat(outcomes).containsExactlyInAnyOrder(true, false);
        assertThat(retrospectiveService.getRetrospectivesForTeam(team.getId()))
                .extracting(Retrospective::getStatus).containsExactly(RetrospectiveStatus.OPEN);
    }

    private static void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while coordinating concurrent requests", ex);
        }
    }
}
