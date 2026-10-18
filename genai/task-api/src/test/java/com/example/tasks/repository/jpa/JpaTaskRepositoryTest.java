package com.example.tasks.repository.jpa;

import com.example.tasks.entity.Task;
import com.example.tasks.entity.TaskDescription;
import com.example.tasks.entity.TaskStatus;
import com.example.tasks.entity.TaskTitle;
import com.example.tasks.entity.UserId;
import com.example.tasks.repository.PageQuery;
import com.example.tasks.repository.TaskFilter;
import com.example.tasks.repository.TaskSort;
import com.example.tasks.repository.TaskVersionConflictException;
import com.example.tasks.support.PostgresTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/** Against real PostgreSQL with the real migrations; the demo users own the tasks. */
@DataJpaTest
@Import({PostgresTestConfiguration.class, JpaTaskRepository.class})
class JpaTaskRepositoryTest {

    private static final UserId ALICE = new UserId(UUID.fromString("6b1f8a2e-1c3d-4e5f-8a9b-0c1d2e3f4a5b"));
    private static final UserId BOB = new UserId(UUID.fromString("7c2a9b3f-2d4e-4f6a-9b0c-1d2e3f4a5b6c"));
    private static final Instant NOW = Instant.parse("2026-10-18T10:00:00.123456Z");
    private static final LocalDate TODAY = LocalDate.parse("2026-10-18");

    @Autowired
    private JpaTaskRepository repository;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void storesATaskAndReadsBackEveryField() {
        var task = Task.create(ALICE, TaskTitle.of("Buy milk"), TaskDescription.of("2 liters\nskimmed").orElseThrow(),
                TODAY.plusDays(2), NOW, TODAY);

        var stored = repository.add(task);

        assertThat(stored).isEqualTo(task);
        assertThat(repository.findByIdAndOwner(task.id(), ALICE)).contains(task);
    }

    @Test
    void doesNotFindAnotherUsersTask() {
        var task = repository.add(Task.create(ALICE, TaskTitle.of("Private"), null, null, NOW, TODAY));

        assertThat(repository.findByIdAndOwner(task.id(), BOB)).isEmpty();
    }

    @Test
    void theDatabaseRefusesATaskWithoutAnExistingOwner() {
        var orphan = Task.create(new UserId(UUID.randomUUID()), TaskTitle.of("Orphan"), null, null, NOW, TODAY);

        assertThatExceptionOfType(DataIntegrityViolationException.class).isThrownBy(() -> {
            repository.add(orphan);
            jdbc.queryForList("SELECT 1");
        });
    }

    @Test
    void anUpdateIsStoredAndBumpsTheVersion() {
        var task = repository.add(Task.create(ALICE, TaskTitle.of("Buy milk"), null, null, NOW, TODAY));

        var updated = repository.update(task.edit(TaskTitle.of("Buy oat milk"), null, TODAY, NOW.plusSeconds(60), TODAY));

        assertThat(updated.version()).isEqualTo(1);
        assertThat(repository.findByIdAndOwner(task.id(), ALICE)).contains(updated);
    }

    @Test
    void refusesAnUpdateFromAStaleCopyEvenAfterTheServiceCheckedIt() {
        // Two requests read version 0; the first one stores version 1; the second still holds version 0.
        var task = repository.add(Task.create(ALICE, TaskTitle.of("Buy milk"), null, null, NOW, TODAY));
        repository.update(task.edit(TaskTitle.of("First"), null, null, NOW, TODAY));

        assertThatExceptionOfType(TaskVersionConflictException.class)
                .isThrownBy(() -> repository.update(task.edit(TaskTitle.of("Second, from a stale copy"), null, null, NOW, TODAY)));
    }

    @Test
    void deletesATaskAtTheGivenVersion() {
        var task = repository.add(Task.create(ALICE, TaskTitle.of("Temporary"), null, null, NOW, TODAY));

        repository.delete(task.id(), 0);

        assertThat(repository.findByIdAndOwner(task.id(), ALICE)).isEmpty();
    }

    @Test
    void refusesToDeleteAtAStaleVersionAndKeepsTheRow() {
        var task = repository.add(Task.create(ALICE, TaskTitle.of("Changed meanwhile"), null, null, NOW, TODAY));
        var updated = repository.update(task.edit(TaskTitle.of("Changed"), null, null, NOW, TODAY));

        assertThatExceptionOfType(TaskVersionConflictException.class).isThrownBy(() -> repository.delete(task.id(), 0));
        assertThat(repository.findByIdAndOwner(task.id(), ALICE)).contains(updated);
    }

    @Test
    void listsOnlyTheOwnersTasks() {
        var mine = add(ALICE, "Mine", null, TaskStatus.TODO);
        add(BOB, "Bob's", null, TaskStatus.TODO);

        var page = repository.findPage(ALICE, TaskFilter.NONE, TaskSort.DEFAULT, new PageQuery(0, 20));

        assertThat(page.tasks()).containsExactly(mine);
        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    void filtersByStatus() {
        add(ALICE, "To do", null, TaskStatus.TODO);
        var done = add(ALICE, "Done", null, TaskStatus.DONE);

        var page = repository.findPage(ALICE, new TaskFilter(TaskStatus.DONE, null, null), TaskSort.DEFAULT, new PageQuery(0, 20));

        assertThat(page.tasks()).containsExactly(done);
    }

    @Test
    void filtersByAnInclusiveDueDateRangeAndLeavesOutTasksWithoutADate() {
        add(ALICE, "Before", TODAY.plusDays(1), TaskStatus.TODO);
        var first = add(ALICE, "First day", TODAY.plusDays(2), TaskStatus.TODO);
        var last = add(ALICE, "Last day", TODAY.plusDays(4), TaskStatus.TODO);
        add(ALICE, "After", TODAY.plusDays(5), TaskStatus.TODO);
        add(ALICE, "No date", null, TaskStatus.TODO);

        var page = repository.findPage(ALICE, new TaskFilter(null, TODAY.plusDays(2), TODAY.plusDays(4)), TaskSort.DEFAULT,
                new PageQuery(0, 20));

        assertThat(page.tasks()).containsExactly(first, last);
    }

    @Test
    void sortsByDueDateWithTasksWithoutADateLastInBothDirections() {
        var noDate = add(ALICE, "No date", null, TaskStatus.TODO);
        var later = add(ALICE, "Later", TODAY.plusDays(3), TaskStatus.TODO);
        var sooner = add(ALICE, "Sooner", TODAY.plusDays(1), TaskStatus.TODO);

        assertThat(titles(TaskSort.DEFAULT)).containsExactly(sooner, later, noDate);
        assertThat(titles(new TaskSort(TaskSort.Field.DUE_DATE, TaskSort.Direction.DESC))).containsExactly(later, sooner, noDate);
    }

    @Test
    void sortsByTitle() {
        var banana = add(ALICE, "Banana", null, TaskStatus.TODO);
        var apple = add(ALICE, "Apple", null, TaskStatus.TODO);

        assertThat(titles(new TaskSort(TaskSort.Field.TITLE, TaskSort.Direction.ASC))).containsExactly(apple, banana);
        assertThat(titles(new TaskSort(TaskSort.Field.TITLE, TaskSort.Direction.DESC))).containsExactly(banana, apple);
    }

    @Test
    void pagesThroughTheResultsWithTheTotal() {
        for (var day = 1; day <= 5; day++) {
            add(ALICE, "Task " + day, TODAY.plusDays(day), TaskStatus.TODO);
        }

        var second = repository.findPage(ALICE, TaskFilter.NONE, TaskSort.DEFAULT, new PageQuery(1, 2));

        assertThat(second.tasks()).extracting(task -> task.title().value()).containsExactly("Task 3", "Task 4");
        assertThat(second.totalElements()).isEqualTo(5);
    }

    private Task add(UserId owner, String title, LocalDate dueDate, TaskStatus status) {
        var task = Task.create(owner, TaskTitle.of(title), null, dueDate, NOW, TODAY);
        var withStatus = new Task(task.id(), task.owner(), task.title(), null, status, task.dueDate(), NOW, NOW, 0);
        return repository.add(withStatus);
    }

    private List<Task> titles(TaskSort sort) {
        return repository.findPage(ALICE, TaskFilter.NONE, sort, new PageQuery(0, 20)).tasks();
    }

    // One constraint per test: after the first error PostgreSQL aborts the whole transaction the test runs in.
    @Test
    void theDatabaseRefusesAnUnknownStatus() {
        assertThatExceptionOfType(DataIntegrityViolationException.class)
                .isThrownBy(() -> insertRow("Valid", "ARCHIVED"));
    }

    @Test
    void theDatabaseRefusesABlankTitle() {
        assertThatExceptionOfType(DataIntegrityViolationException.class)
                .isThrownBy(() -> insertRow("   ", "TODO"));
    }

    private void insertRow(String title, String status) {
        jdbc.update("INSERT INTO tasks (id, owner_id, title, status, created_at, updated_at, version) "
                + "VALUES (?, ?, ?, ?, now(), now(), 0)", UUID.randomUUID(), ALICE.value(), title, status);
    }
}
