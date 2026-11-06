package co.inter.piggies.controller;

import co.inter.piggies.entity.Task;
import co.inter.piggies.service.TaskService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;

import java.util.List;

@Controller("/api/task")
@ExecuteOn(TaskExecutors.BLOCKING)
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @Get
    public HttpResponse<List<Task>> getAllTasks() {
        return HttpResponse.ok(taskService.getAllTasks());
    }

    @Get("/{id}")
    public HttpResponse<Task> getTaskById(@PathVariable Long id) {
        return HttpResponse.ok(taskService.getTaskById(id));
    }

    @Post
    public HttpResponse<Task> createTask(@Body Task task) {
        return HttpResponse.created(taskService.createtask(task));
    }
}