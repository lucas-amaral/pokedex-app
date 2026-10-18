package com.example.tasks.service;

import com.example.tasks.entity.InvalidFieldException;
import com.example.tasks.entity.InvalidStatusChangeException;
import com.example.tasks.entity.TaskId;
import com.example.tasks.entity.TaskStatus;
import com.example.tasks.entity.UserId;
import com.example.tasks.repository.PageQuery;
import com.example.tasks.repository.TaskFilter;
import com.example.tasks.repository.TaskSort;
import com.example.tasks.repository.TaskVersionConflictException;
import com.example.tasks.support.InMemoryTaskRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class TaskServiceTest {

    /** 22:30 UTC: still the 18th in UTC, already the 19th in Tokyo, so "today" must come from the UTC date. */
    private static final Instant NOW = Instant.parse("2026-10-18T22:30:00.123456789Z");
    private static final UserId ALICE = new UserId(UUID.randomUUID());
    private static final UserId BOB = new UserId(UUID.randomUUID());

    private final InMemoryTaskRepository repository = new InMemoryTaskRepository();
    private final TaskService service = new TaskService(repository, Clock.fixed(NOW, ZoneOffset.ofHours(9)));

    @Test
    void createsATaskForTheCaller() {
        var task = service.create(ALICE, new NewTask("Buy milk", "2 liters", LocalDate.parse("2026-10-20")));

        assertThat(task.owner()).isEqualTo(ALICE);
        assertThat(task.status()).isEqualTo(TaskStatus.TODO);
        assertThat(repository.all()).containsExactly(task);
    }

    @Test
    void storesTimesAtTheDatabasePrecision() {
        var task = service.create(ALICE, new NewTask("Buy milk", null, null));

        // PostgreSQL keeps microseconds; anything finer would differ between the response and a later read.
        assertThat(task.createdAt()).isEqualTo(Instant.parse("2026-10-18T22:30:00.123456Z"));
    }

    @Test
    void judgesThePastByTheUtcDate() {
        var dueTodayInUtc = LocalDate.parse("2026-10-18");

        assertThat(service.create(ALICE, new NewTask("Today", null, dueTodayInUtc)).dueDate()).isEqualTo(dueTodayInUtc);
        assertThatExceptionOfType(InvalidFieldException.class)
                .isThrownBy(() -> service.create(ALICE, new NewTask("Yesterday", null, dueTodayInUtc.minusDays(1))));
    }

    @Test
    void readsTheCallersOwnTask() {
        var task = service.create(ALICE, new NewTask("Mine", null, null));

        assertThat(service.get(ALICE, task.id())).isEqualTo(task);
    }

    @Test
    void anotherUsersTaskLooksExactlyLikeAMissingOne() {
        var task = service.create(ALICE, new NewTask("Private", null, null));

        var othersTask = catchNotFound(() -> service.get(BOB, task.id()));
        var missing = catchNotFound(() -> service.get(BOB, new TaskId(UUID.randomUUID())));

        assertThat(othersTask).isInstanceOf(TaskNotFoundException.class);
        assertThat(missing).isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void listsOnlyTheCallersTasks() {
        var mine = service.create(ALICE, new NewTask("Mine", null, null));
        service.create(BOB, new NewTask("Bob's", null, null));

        var page = service.list(ALICE, TaskFilter.NONE, TaskSort.DEFAULT, new PageQuery(0, 20));

        assertThat(page.tasks()).containsExactly(mine);
        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    void updatesTheEditableFieldsAndBumpsTheVersion() {
        var task = service.create(ALICE, new NewTask("Buy milk", null, null));

        var updated = service.update(ALICE, task.id(), new TaskChanges(0, "Buy oat milk", "1 liter", LocalDate.parse("2026-10-25")));

        assertThat(updated.title().value()).isEqualTo("Buy oat milk");
        assertThat(updated.description().value()).isEqualTo("1 liter");
        assertThat(updated.dueDate()).isEqualTo(LocalDate.parse("2026-10-25"));
        assertThat(updated.version()).isEqualTo(1);
        assertThat(service.get(ALICE, task.id())).isEqualTo(updated);
    }

    @Test
    void refusesAnUpdateBasedOnAStaleVersionAndChangesNothing() {
        var task = service.create(ALICE, new NewTask("Buy milk", null, null));
        service.update(ALICE, task.id(), new TaskChanges(0, "First edit", null, null));

        assertThatExceptionOfType(TaskVersionConflictException.class)
                .isThrownBy(() -> service.update(ALICE, task.id(), new TaskChanges(0, "Second edit from an old copy", null, null)))
                .satisfies(conflict -> {
                    assertThat(conflict.expectedVersion()).isEqualTo(0);
                    assertThat(conflict.currentVersion()).contains(1L);
                });
        assertThat(service.get(ALICE, task.id()).title().value()).isEqualTo("First edit");
    }

    @Test
    void cannotUpdateAnotherUsersTask() {
        var task = service.create(ALICE, new NewTask("Private", null, null));

        assertThatExceptionOfType(TaskNotFoundException.class)
                .isThrownBy(() -> service.update(BOB, task.id(), new TaskChanges(0, "Mine now", null, null)));
        assertThat(service.get(ALICE, task.id()).title().value()).isEqualTo("Private");
    }

    @Test
    void changesTheStatusAndBumpsTheVersion() {
        var task = service.create(ALICE, new NewTask("Report", null, null));

        var started = service.changeStatus(ALICE, task.id(), new StatusChange(0, TaskStatus.IN_PROGRESS));

        assertThat(started.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(started.version()).isEqualTo(1);
    }

    @Test
    void settingTheCurrentStatusAgainIsAHarmlessRetry() {
        var task = service.create(ALICE, new NewTask("Report", null, null));

        var unchanged = service.changeStatus(ALICE, task.id(), new StatusChange(0, TaskStatus.TODO));

        assertThat(unchanged).isEqualTo(task);
        assertThat(unchanged.version()).isZero();
    }

    @Test
    void refusesAForbiddenTransitionAndChangesNothing() {
        var task = service.create(ALICE, new NewTask("Report", null, null));
        var done = service.changeStatus(ALICE, task.id(), new StatusChange(0, TaskStatus.DONE));

        assertThatExceptionOfType(InvalidStatusChangeException.class)
                .isThrownBy(() -> service.changeStatus(ALICE, task.id(), new StatusChange(done.version(), TaskStatus.TODO)));
        assertThat(service.get(ALICE, task.id())).isEqualTo(done);
    }

    @Test
    void refusesAStatusChangeFromAStaleVersion() {
        var task = service.create(ALICE, new NewTask("Report", null, null));
        service.changeStatus(ALICE, task.id(), new StatusChange(0, TaskStatus.IN_PROGRESS));

        assertThatExceptionOfType(TaskVersionConflictException.class)
                .isThrownBy(() -> service.changeStatus(ALICE, task.id(), new StatusChange(0, TaskStatus.DONE)));
    }

    @Test
    void cannotChangeTheStatusOfAnotherUsersTask() {
        var task = service.create(ALICE, new NewTask("Private", null, null));

        assertThatExceptionOfType(TaskNotFoundException.class)
                .isThrownBy(() -> service.changeStatus(BOB, task.id(), new StatusChange(0, TaskStatus.DONE)));
    }

    @Test
    void deletesATaskForGoodSoASecondDeleteIsNotFound() {
        var task = service.create(ALICE, new NewTask("Temporary", null, null));

        service.delete(ALICE, task.id(), 0);

        assertThatExceptionOfType(TaskNotFoundException.class).isThrownBy(() -> service.get(ALICE, task.id()));
        assertThatExceptionOfType(TaskNotFoundException.class).isThrownBy(() -> service.delete(ALICE, task.id(), 0));
    }

    @Test
    void refusesToDeleteFromAStaleVersionAndKeepsTheTask() {
        var task = service.create(ALICE, new NewTask("Changed meanwhile", null, null));
        service.update(ALICE, task.id(), new TaskChanges(0, "Changed", null, null));

        assertThatExceptionOfType(TaskVersionConflictException.class).isThrownBy(() -> service.delete(ALICE, task.id(), 0));
        assertThat(service.get(ALICE, task.id()).title().value()).isEqualTo("Changed");
    }

    @Test
    void cannotDeleteAnotherUsersTask() {
        var task = service.create(ALICE, new NewTask("Private", null, null));

        assertThatExceptionOfType(TaskNotFoundException.class).isThrownBy(() -> service.delete(BOB, task.id(), 0));
        assertThat(repository.all()).containsExactly(task);
    }

    private static Throwable catchNotFound(Runnable call) {
        try {
            call.run();
            return null;
        } catch (TaskNotFoundException notFound) {
            return notFound;
        }
    }
}
