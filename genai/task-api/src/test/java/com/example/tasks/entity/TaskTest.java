package com.example.tasks.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class TaskTest {

    private static final Instant NOW = Instant.parse("2026-10-18T22:30:00Z");
    private static final LocalDate TODAY = LocalDate.parse("2026-10-18");
    private static final UserId ALICE = new UserId(UUID.randomUUID());

    @Test
    void aNewTaskIsToDoAtVersionZero() {
        var task = Task.create(ALICE, TaskTitle.of("Buy milk"), TaskDescription.of("2 liters").orElseThrow(), TODAY.plusDays(1), NOW, TODAY);

        assertThat(task.id()).isNotNull();
        assertThat(task.owner()).isEqualTo(ALICE);
        assertThat(task.title().value()).isEqualTo("Buy milk");
        assertThat(task.description()).isEqualTo(new TaskDescription("2 liters"));
        assertThat(task.status()).isEqualTo(TaskStatus.TODO);
        assertThat(task.dueDate()).isEqualTo(TODAY.plusDays(1));
        assertThat(task.createdAt()).isEqualTo(NOW);
        assertThat(task.updatedAt()).isEqualTo(NOW);
        assertThat(task.version()).isZero();
    }

    @Test
    void mayHaveNoDescriptionAndNoDueDate() {
        var task = Task.create(ALICE, TaskTitle.of("Someday"), null, null, NOW, TODAY);

        assertThat(task.description()).isNull();
        assertThat(task.dueDate()).isNull();
    }

    @Test
    void mayBeDueToday() {
        assertThat(Task.create(ALICE, TaskTitle.of("Today"), null, TODAY, NOW, TODAY).dueDate()).isEqualTo(TODAY);
    }

    @Test
    void cannotBeCreatedAlreadyOverdue() {
        assertThatExceptionOfType(InvalidFieldException.class)
                .isThrownBy(() -> Task.create(ALICE, TaskTitle.of("Too late"), null, TODAY.minusDays(1), NOW, TODAY))
                .satisfies(exception -> assertThat(exception.field()).isEqualTo("dueDate"));
    }

    @Test
    void knowsItsOwner() {
        var task = Task.create(ALICE, TaskTitle.of("Mine"), null, null, NOW, TODAY);

        assertThat(task.isOwnedBy(ALICE)).isTrue();
        assertThat(task.isOwnedBy(new UserId(UUID.randomUUID()))).isFalse();
    }

    @Test
    void anEditReplacesTheEditableFieldsAndKeepsTheRest() {
        var task = Task.create(ALICE, TaskTitle.of("Buy milk"), TaskDescription.of("2 liters").orElseThrow(), TODAY, NOW, TODAY);
        var later = NOW.plusSeconds(60);

        var edited = task.edit(TaskTitle.of("Buy oat milk"), null, TODAY.plusDays(3), later, TODAY);

        assertThat(edited.title().value()).isEqualTo("Buy oat milk");
        assertThat(edited.description()).isNull();
        assertThat(edited.dueDate()).isEqualTo(TODAY.plusDays(3));
        assertThat(edited.updatedAt()).isEqualTo(later);
        assertThat(edited).extracting(Task::id, Task::owner, Task::status, Task::createdAt, Task::version)
                .containsExactly(task.id(), ALICE, TaskStatus.TODO, NOW, 0L);
    }

    @Test
    void anEditCannotMoveTheDueDateIntoThePast() {
        var task = Task.create(ALICE, TaskTitle.of("Report"), null, TODAY.plusDays(2), NOW, TODAY);

        assertThatExceptionOfType(InvalidFieldException.class)
                .isThrownBy(() -> task.edit(task.title(), null, TODAY.minusDays(1), NOW, TODAY))
                .satisfies(exception -> assertThat(exception.field()).isEqualTo("dueDate"));
    }

    @Test
    void anOverdueTaskCanStillBeEditedWhenItsDueDateStaysTheSame() {
        var createdLastWeek = Task.create(ALICE, TaskTitle.of("Report"), null, TODAY.minusDays(2), NOW, TODAY.minusDays(7));

        var edited = createdLastWeek.edit(TaskTitle.of("Report, late"), null, TODAY.minusDays(2), NOW, TODAY);

        assertThat(edited.dueDate()).isEqualTo(TODAY.minusDays(2));
    }

    @Test
    void anAllowedStatusChangeIsApplied() {
        var task = Task.create(ALICE, TaskTitle.of("Report"), null, null, NOW, TODAY);
        var later = NOW.plusSeconds(60);

        var started = task.changeStatus(TaskStatus.IN_PROGRESS, later);

        assertThat(started.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(started.updatedAt()).isEqualTo(later);
        assertThat(started.version()).isEqualTo(task.version());
    }

    @Test
    void settingTheCurrentStatusChangesNothing() {
        var task = Task.create(ALICE, TaskTitle.of("Report"), null, null, NOW, TODAY);

        assertThat(task.changeStatus(TaskStatus.TODO, NOW.plusSeconds(60))).isSameAs(task);
    }

    @Test
    void aRefusedStatusChangeSaysFromWhereToWhereAndWhatIsAllowed() {
        var done = Task.create(ALICE, TaskTitle.of("Report"), null, null, NOW, TODAY).changeStatus(TaskStatus.DONE, NOW);

        assertThatExceptionOfType(InvalidStatusChangeException.class)
                .isThrownBy(() -> done.changeStatus(TaskStatus.TODO, NOW))
                .satisfies(refused -> {
                    assertThat(refused.from()).isEqualTo(TaskStatus.DONE);
                    assertThat(refused.to()).isEqualTo(TaskStatus.TODO);
                    assertThat(refused.allowed()).containsExactly(TaskStatus.IN_PROGRESS);
                });
    }
}
