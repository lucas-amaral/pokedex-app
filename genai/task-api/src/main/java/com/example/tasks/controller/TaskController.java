package com.example.tasks.controller;

import com.example.tasks.entity.TaskId;
import com.example.tasks.entity.TaskStatus;
import com.example.tasks.repository.PageQuery;
import com.example.tasks.repository.TaskFilter;
import com.example.tasks.repository.TaskSort;
import com.example.tasks.service.ChangeTaskStatusUseCase;
import com.example.tasks.service.CreateTaskUseCase;
import com.example.tasks.service.DeleteTaskUseCase;
import com.example.tasks.service.GetTaskUseCase;
import com.example.tasks.service.ListTasksUseCase;
import com.example.tasks.service.UpdateTaskUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
@Tag(name = "Tasks", description = "The caller's own tasks")
@SecurityRequirement(name = "bearerAuth")
class TaskController {

    private final CreateTaskUseCase createTask;
    private final GetTaskUseCase getTask;
    private final ListTasksUseCase listTasks;
    private final UpdateTaskUseCase updateTask;
    private final ChangeTaskStatusUseCase changeStatus;
    private final DeleteTaskUseCase deleteTask;

    TaskController(CreateTaskUseCase createTask, GetTaskUseCase getTask, ListTasksUseCase listTasks,
                   UpdateTaskUseCase updateTask, ChangeTaskStatusUseCase changeStatus, DeleteTaskUseCase deleteTask) {
        this.createTask = createTask;
        this.getTask = getTask;
        this.listTasks = listTasks;
        this.updateTask = updateTask;
        this.changeStatus = changeStatus;
        this.deleteTask = deleteTask;
    }

    @PostMapping
    @Operation(summary = "Create a task", description = "A new task is TODO; its due date cannot be in the past (UTC).")
    @ApiResponse(responseCode = "201", description = "Created; Location points to it")
    @ApiResponse(responseCode = "400", description = "A field is missing, malformed, unknown or breaks a rule")
    ResponseEntity<TaskResponse> create(@AuthenticationPrincipal Jwt token, @RequestBody CreateTaskRequest request) {
        var task = createTask.create(CurrentUser.from(token), request.toNewTask());
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(task.id().value()).toUri();
        return ResponseEntity.created(location).body(TaskResponse.from(task));
    }

    @GetMapping
    @Operation(summary = "List your tasks", description = "Sorted by due date by default; tasks without one come last.")
    @ApiResponse(responseCode = "200", description = "One page of your tasks")
    @ApiResponse(responseCode = "400", description = "An invalid filter, sort or page parameter")
    PageResponse list(
            @AuthenticationPrincipal Jwt token,
            @RequestParam(required = false) TaskStatus status,
            @Parameter(description = "Inclusive") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueFrom,
            @Parameter(description = "Inclusive") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueTo,
            @Parameter(description = "dueDate, createdAt, title or status, optionally followed by ,asc or ,desc")
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(PageQuery.MAX_SIZE) int size) {
        var tasks = listTasks.list(CurrentUser.from(token), new TaskFilter(status, dueFrom, dueTo), TaskSort.parse(sort),
                new PageQuery(page, size));
        return PageResponse.from(tasks);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Read one of your tasks")
    @ApiResponse(responseCode = "200", description = "The task")
    @ApiResponse(responseCode = "404", description = "No such task among yours")
    TaskResponse get(@AuthenticationPrincipal Jwt token, @PathVariable UUID id) {
        return TaskResponse.from(getTask.get(CurrentUser.from(token), new TaskId(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a task's title, description and due date",
            description = "A field left out is cleared. Send the version you read; if the task changed since, nothing is saved.")
    @ApiResponse(responseCode = "200", description = "Saved; the response carries the new version")
    @ApiResponse(responseCode = "400", description = "A field is missing, malformed, unknown or breaks a rule")
    @ApiResponse(responseCode = "404", description = "No such task among yours")
    @ApiResponse(responseCode = "409", description = "The task changed since the version you sent")
    TaskResponse update(@AuthenticationPrincipal Jwt token, @PathVariable UUID id, @Valid @RequestBody UpdateTaskRequest request) {
        return TaskResponse.from(updateTask.update(CurrentUser.from(token), new TaskId(id), request.toChanges()));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change a task's status",
            description = "TODO to IN_PROGRESS or DONE; IN_PROGRESS to TODO or DONE; DONE back to IN_PROGRESS. "
                    + "Asking for the current status again changes nothing.")
    @ApiResponse(responseCode = "200", description = "The task with its status")
    @ApiResponse(responseCode = "400", description = "Missing or unknown status or version")
    @ApiResponse(responseCode = "404", description = "No such task among yours")
    @ApiResponse(responseCode = "409", description = "The task changed since the version you sent")
    @ApiResponse(responseCode = "422", description = "That status change is not allowed; the problem lists the allowed ones")
    TaskResponse changeStatus(@AuthenticationPrincipal Jwt token, @PathVariable UUID id,
                              @Valid @RequestBody ChangeStatusRequest request) {
        return TaskResponse.from(changeStatus.changeStatus(CurrentUser.from(token), new TaskId(id), request.toStatusChange()));
    }

    /** {@code DELETE} has no body, so the version travels as a query parameter (decided before implementation). */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a task for good", description = "Send the version you read; deleting twice gives 404.")
    @ApiResponse(responseCode = "204", description = "Deleted")
    @ApiResponse(responseCode = "400", description = "Missing or malformed version")
    @ApiResponse(responseCode = "404", description = "No such task among yours")
    @ApiResponse(responseCode = "409", description = "The task changed since the version you sent")
    void delete(@AuthenticationPrincipal Jwt token, @PathVariable UUID id,
                @Parameter(description = "The version you read") @RequestParam(required = false) @NotNull @Min(0) Long version) {
        deleteTask.delete(CurrentUser.from(token), new TaskId(id), version);
    }
}
