package co.inter.piggies.service;

import co.inter.piggies.entity.Task;
import co.inter.piggies.events.TaskCreatedEvent;
import co.inter.piggies.repository.TaskRepository;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.List;

@Singleton
public class TaskService {

    @Inject
    TaskRepository taskRepository;

    public List<Task> getAllTasks() {
        return  taskRepository.findAll();
    }

    public Task getTaskById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new HttpStatusException(HttpStatus.NOT_FOUND,
                "Tarefa " + id + " não encontrada"));
    }

    public Task createtask(Task task) {
        Task task1 = new Task();

        task1.setTitle(task.getTitle());
        task1.setDescription(task.getDescription());

        return task1;
    }
}
