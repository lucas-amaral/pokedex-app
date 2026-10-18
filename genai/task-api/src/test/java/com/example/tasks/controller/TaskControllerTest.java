package com.example.tasks.controller;

import com.example.tasks.config.JsonConfig;
import com.example.tasks.config.SecurityConfig;
import com.example.tasks.entity.InvalidFieldException;
import com.example.tasks.entity.InvalidStatusChangeException;
import com.example.tasks.entity.Task;
import com.example.tasks.entity.TaskDescription;
import com.example.tasks.entity.TaskId;
import com.example.tasks.entity.TaskStatus;
import com.example.tasks.entity.TaskTitle;
import com.example.tasks.entity.UserId;
import com.example.tasks.repository.PageQuery;
import com.example.tasks.repository.TaskFilter;
import com.example.tasks.repository.TaskPage;
import com.example.tasks.repository.TaskSort;
import com.example.tasks.repository.TaskVersionConflictException;
import com.example.tasks.repository.UserRepository;
import com.example.tasks.service.ChangeTaskStatusUseCase;
import com.example.tasks.service.CreateTaskUseCase;
import com.example.tasks.service.DeleteTaskUseCase;
import com.example.tasks.service.GetTaskUseCase;
import com.example.tasks.service.ListTasksUseCase;
import com.example.tasks.service.NewTask;
import com.example.tasks.service.StatusChange;
import com.example.tasks.service.TaskChanges;
import com.example.tasks.service.TaskNotFoundException;
import com.example.tasks.service.UpdateTaskUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@WebMvcTest(TaskController.class)
@Import({SecurityConfig.class, JsonConfig.class})
class TaskControllerTest {

    private static final String TASKS = "/api/v1/tasks";
    private static final UserId ALICE = new UserId(UUID.fromString("6b1f8a2e-1c3d-4e5f-8a9b-0c1d2e3f4a5b"));
    private static final Instant NOW = Instant.parse("2026-10-18T10:00:00Z");
    private static final LocalDate TODAY = LocalDate.parse("2026-10-18");

    @Autowired
    private MockMvcTester mvc;

    /** The security chain checks that a token's account still exists; tests here authenticate with jwt() instead. */
    @MockitoBean
    private UserRepository users;

    @MockitoBean
    private CreateTaskUseCase createTask;

    @MockitoBean
    private GetTaskUseCase getTask;

    @MockitoBean
    private ListTasksUseCase listTasks;

    @MockitoBean
    private UpdateTaskUseCase updateTask;

    @MockitoBean
    private ChangeTaskStatusUseCase changeStatus;

    @MockitoBean
    private DeleteTaskUseCase deleteTask;

    private final Task milk = Task.create(ALICE, TaskTitle.of("Buy milk"), TaskDescription.of("2 liters").orElseThrow(),
            LocalDate.parse("2026-10-20"), NOW, TODAY);

    private static RequestPostProcessor alice() {
        return jwt().jwt(token -> token.subject(ALICE.value().toString()));
    }

    @Test
    void createsATaskForTheCallerAndPointsToIt() {
        given(createTask.create(ALICE, new NewTask("Buy milk", "2 liters", LocalDate.parse("2026-10-20")))).willReturn(milk);

        var response = mvc.post().with(alice()).uri(TASKS).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\": \"Buy milk\", \"description\": \"2 liters\", \"dueDate\": \"2026-10-20\"}").exchange();

        assertThat(response).hasStatus(HttpStatus.CREATED);
        assertThat(response).hasHeader("Location", "http://localhost/api/v1/tasks/" + milk.id().value());
        assertThat(response).bodyJson().isStrictlyEqualTo("""
                {
                  "id": "%s",
                  "title": "Buy milk",
                  "description": "2 liters",
                  "status": "TODO",
                  "dueDate": "2026-10-20",
                  "createdAt": "2026-10-18T10:00:00Z",
                  "updatedAt": "2026-10-18T10:00:00Z",
                  "version": 0
                }
                """.formatted(milk.id().value()));
    }

    @Test
    void reportsARuleTheTaskBreaksAsAFieldError() {
        given(createTask.create(any(), any())).willThrow(new InvalidFieldException("dueDate", "must be today or later"));

        var response = mvc.post().with(alice()).uri(TASKS).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\": \"Too late\", \"dueDate\": \"2000-01-01\"}").exchange();

        assertThat(response).hasStatus(HttpStatus.BAD_REQUEST).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response).bodyJson().extractingPath("$.errors[0].field").isEqualTo("dueDate");
        assertThat(response).bodyJson().extractingPath("$.errors[0].message").isEqualTo("must be today or later");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "{\"title\": \"Buy milk\", \"status\": \"DONE\"}         | status  | is not a known field",
            "{\"title\": \"Buy milk\", \"id\": \"x\"}                | id      | is not a known field",
            "{\"title\": 42}                                      | title   | must be text",
            "{\"title\": \"Buy milk\", \"dueDate\": \"31/01/2030\"}  | dueDate | must be a date like 2030-01-31"})
    void refusesWhatTheClientCannotSetOrGotWrong(String body, String field, String message) {
        var response = mvc.post().with(alice()).uri(TASKS).contentType(MediaType.APPLICATION_JSON).content(body).exchange();

        assertThat(response).hasStatus(HttpStatus.BAD_REQUEST).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response).bodyJson().extractingPath("$.errors[0].field").isEqualTo(field);
        assertThat(response).bodyJson().extractingPath("$.errors[0].message").isEqualTo(message);
        verifyNoInteractions(createTask);
    }

    @Test
    void readsOneOfTheCallersTasks() {
        given(getTask.get(ALICE, milk.id())).willReturn(milk);

        var response = mvc.get().with(alice()).uri(TASKS + "/{id}", milk.id().value()).exchange();

        assertThat(response).hasStatus(HttpStatus.OK);
        assertThat(response).bodyJson().extractingPath("$.title").isEqualTo("Buy milk");
    }

    @Test
    void answersNotFoundForAMissingOrSomeoneElsesTask() {
        var id = new TaskId(UUID.randomUUID());
        given(getTask.get(ALICE, id)).willThrow(new TaskNotFoundException(id));

        var response = mvc.get().with(alice()).uri(TASKS + "/{id}", id.value()).exchange();

        assertThat(response).hasStatus(HttpStatus.NOT_FOUND).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response).bodyJson().extractingPath("$.title").isEqualTo("Task not found");
    }

    @Test
    void refusesAnIdThatIsNotAUuid() {
        var response = mvc.get().with(alice()).uri(TASKS + "/42").exchange();

        assertThat(response).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(response).bodyJson().extractingPath("$.errors[0].parameter").isEqualTo("id");
        assertThat(response).bodyJson().extractingPath("$.errors[0].message").isEqualTo("must be a UUID");
    }

    @Test
    void listsAPageOfTheCallersTasks() {
        given(listTasks.list(ALICE, TaskFilter.NONE, TaskSort.DEFAULT, new PageQuery(0, 20)))
                .willReturn(new TaskPage(List.of(milk), new PageQuery(0, 20), 21));

        var response = mvc.get().with(alice()).uri(TASKS).exchange();

        assertThat(response).hasStatus(HttpStatus.OK);
        assertThat(response).bodyJson().extractingPath("$.content[0].title").isEqualTo("Buy milk");
        assertThat(response).bodyJson().extractingPath("$.page").isEqualTo(0);
        assertThat(response).bodyJson().extractingPath("$.size").isEqualTo(20);
        assertThat(response).bodyJson().extractingPath("$.totalElements").isEqualTo(21);
        assertThat(response).bodyJson().extractingPath("$.totalPages").isEqualTo(2);
    }

    @Test
    void passesFiltersSortAndPageToTheUseCase() {
        var filter = new TaskFilter(TaskStatus.IN_PROGRESS, LocalDate.parse("2026-10-20"), LocalDate.parse("2026-10-31"));
        var sort = new TaskSort(TaskSort.Field.TITLE, TaskSort.Direction.DESC);
        given(listTasks.list(ALICE, filter, sort, new PageQuery(2, 5))).willReturn(new TaskPage(List.of(), new PageQuery(2, 5), 0));

        var response = mvc.get().with(alice())
                .uri(TASKS + "?status=IN_PROGRESS&dueFrom=2026-10-20&dueTo=2026-10-31&sort=title,desc&page=2&size=5").exchange();

        assertThat(response).hasStatus(HttpStatus.OK);
        assertThat(response).bodyJson().extractingPath("$.totalElements").isEqualTo(0);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "size=51              | size    | must be less than or equal to 50",
            "size=0               | size    | must be greater than or equal to 1",
            "page=-1              | page    | must be greater than or equal to 0",
            "status=ARCHIVED      | status  | must be one of [TODO, IN_PROGRESS, DONE]",
            "dueFrom=tomorrow     | dueFrom | must be a date like 2030-01-31",
            "sort=owner           | sort    | must be one of dueDate, createdAt, title, status, optionally followed by ,asc or ,desc",
            "dueFrom=2026-10-21&dueTo=2026-10-20 | dueTo | must not be before dueFrom"})
    void explainsEachInvalidListParameter(String query, String parameter, String message) {
        var response = mvc.get().with(alice()).uri(TASKS + "?" + query).exchange();

        assertThat(response).hasStatus(HttpStatus.BAD_REQUEST).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response).bodyJson().extractingPath("$.errors[0].parameter").isEqualTo(parameter);
        assertThat(response).bodyJson().extractingPath("$.errors[0].message").isEqualTo(message);
        verifyNoInteractions(listTasks);
    }

    @Test
    void updatesATask() {
        var changes = new TaskChanges(0, "Buy oat milk", null, LocalDate.parse("2026-10-25"));
        var updated = new Task(milk.id(), ALICE, TaskTitle.of("Buy oat milk"), null, milk.status(), LocalDate.parse("2026-10-25"),
                NOW, NOW.plusSeconds(60), 1);
        given(updateTask.update(ALICE, milk.id(), changes)).willReturn(updated);

        var response = mvc.put().with(alice()).uri(TASKS + "/{id}", milk.id().value()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\": 0, \"title\": \"Buy oat milk\", \"dueDate\": \"2026-10-25\"}").exchange();

        assertThat(response).hasStatus(HttpStatus.OK);
        assertThat(response).bodyJson().extractingPath("$.title").isEqualTo("Buy oat milk");
        assertThat(response).bodyJson().extractingPath("$.version").isEqualTo(1);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "{\"title\": \"No version\"}                          | version | must not be null",
            "{\"version\": -1, \"title\": \"x\"}                   | version | must be greater than or equal to 0",
            "{\"version\": 0, \"title\": \"x\", \"status\": \"DONE\"} | status  | is not a known field",
            "{\"version\": \"0\", \"title\": \"x\"}                | version | must be a whole number"})
    void refusesAnInvalidUpdate(String body, String field, String message) {
        var response = mvc.put().with(alice()).uri(TASKS + "/{id}", milk.id().value()).contentType(MediaType.APPLICATION_JSON)
                .content(body).exchange();

        assertThat(response).hasStatus(HttpStatus.BAD_REQUEST).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response).bodyJson().extractingPath("$.errors[0].field").isEqualTo(field);
        assertThat(response).bodyJson().extractingPath("$.errors[0].message").isEqualTo(message);
        verifyNoInteractions(updateTask);
    }

    @Test
    void explainsAConflictWithBothVersions() {
        given(updateTask.update(any(), any(), any())).willThrow(new TaskVersionConflictException(milk.id(), 0, 3L));

        var response = mvc.put().with(alice()).uri(TASKS + "/{id}", milk.id().value()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\": 0, \"title\": \"Stale\"}").exchange();

        assertThat(response).hasStatus(HttpStatus.CONFLICT).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response).bodyJson().extractingPath("$.title").isEqualTo("Task changed since you loaded it");
        assertThat(response).bodyJson().extractingPath("$.expectedVersion").isEqualTo(0);
        assertThat(response).bodyJson().extractingPath("$.currentVersion").isEqualTo(3);
    }

    @Test
    void changesTheStatus() {
        var started = milk.changeStatus(TaskStatus.IN_PROGRESS, NOW);
        given(changeStatus.changeStatus(ALICE, milk.id(), new StatusChange(0, TaskStatus.IN_PROGRESS))).willReturn(started);

        var response = mvc.patch().with(alice()).uri(TASKS + "/{id}/status", milk.id().value())
                .contentType(MediaType.APPLICATION_JSON).content("{\"version\": 0, \"status\": \"IN_PROGRESS\"}").exchange();

        assertThat(response).hasStatus(HttpStatus.OK);
        assertThat(response).bodyJson().extractingPath("$.status").isEqualTo("IN_PROGRESS");
    }

    @Test
    void explainsARefusedTransitionWithWhatIsAllowed() {
        given(changeStatus.changeStatus(any(), any(), any()))
                .willThrow(new InvalidStatusChangeException(TaskStatus.DONE, TaskStatus.TODO));

        var response = mvc.patch().with(alice()).uri(TASKS + "/{id}/status", milk.id().value())
                .contentType(MediaType.APPLICATION_JSON).content("{\"version\": 2, \"status\": \"TODO\"}").exchange();

        assertThat(response).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response).bodyJson().extractingPath("$.title").isEqualTo("Status change not allowed");
        assertThat(response).bodyJson().extractingPath("$.detail").isEqualTo("A task cannot go from DONE to TODO.");
        assertThat(response).bodyJson().extractingPath("$.allowed").asArray().containsExactly("IN_PROGRESS");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "{\"version\": 0}                            | status  | must not be null",
            "{\"status\": \"DONE\"}                       | version | must not be null",
            "{\"version\": 0, \"status\": \"ARCHIVED\"}    | status  | must be one of [TODO, IN_PROGRESS, DONE]",
            "{\"version\": 0, \"status\": \"DONE\", \"title\": \"x\"} | title | is not a known field"})
    void refusesAnInvalidStatusChange(String body, String field, String message) {
        var response = mvc.patch().with(alice()).uri(TASKS + "/{id}/status", milk.id().value())
                .contentType(MediaType.APPLICATION_JSON).content(body).exchange();

        assertThat(response).hasStatus(HttpStatus.BAD_REQUEST).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response).bodyJson().extractingPath("$.errors[0].field").isEqualTo(field);
        assertThat(response).bodyJson().extractingPath("$.errors[0].message").isEqualTo(message);
        verifyNoInteractions(changeStatus);
    }

    @Test
    void deletesATask() {
        var response = mvc.delete().with(alice()).uri(TASKS + "/{id}?version=3", milk.id().value()).exchange();

        assertThat(response).hasStatus(HttpStatus.NO_CONTENT);
        verify(deleteTask).delete(ALICE, milk.id(), 3);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "''         | must not be null",
            "?version=x  | must be a whole number",
            "?version=-1 | must be greater than or equal to 0"})
    void requiresTheVersionToDelete(String query, String message) {
        var response = mvc.delete().with(alice()).uri(TASKS + "/" + milk.id().value() + query).exchange();

        assertThat(response).hasStatus(HttpStatus.BAD_REQUEST).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response).bodyJson().extractingPath("$.errors[0].parameter").isEqualTo("version");
        assertThat(response).bodyJson().extractingPath("$.errors[0].message").isEqualTo(message);
        verifyNoInteractions(deleteTask);
    }

    @Test
    void answersNotFoundWhenDeletingAMissingTask() {
        willThrow(new TaskNotFoundException(milk.id())).given(deleteTask).delete(ALICE, milk.id(), 0);

        assertThat(mvc.delete().with(alice()).uri(TASKS + "/{id}?version=0", milk.id().value())).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void answersSpringsOwnErrorsAsProblemsToo() {
        assertThat(mvc.get().with(alice()).uri("/api/v1/nothing-here").exchange())
                .hasStatus(HttpStatus.NOT_FOUND).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(mvc.patch().with(alice()).uri(TASKS + "/{id}", milk.id().value()).exchange())
                .hasStatus(HttpStatus.METHOD_NOT_ALLOWED).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(mvc.post().with(alice()).uri(TASKS).contentType(MediaType.TEXT_PLAIN).content("Buy milk").exchange())
                .hasStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void requiresALogin() {
        assertThat(mvc.post().uri(TASKS).contentType(MediaType.APPLICATION_JSON).content("{\"title\": \"x\"}"))
                .hasStatus(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(createTask);
    }
}
