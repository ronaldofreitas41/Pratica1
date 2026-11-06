package co.inter.piggies.service;

import co.inter.piggies.entity.Task;
import co.inter.piggies.events.TaskCreatedEvent;
import co.inter.piggies.producer.TaskProducer;
import co.inter.piggies.repository.TaskRepository;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

import java.util.List;

@Singleton
public class TaskService {

    @Inject
    TaskRepository taskRepository;

    @Inject
    TaskProducer taskProducer;

    public List<Task> getAllTasks() {
        return  taskRepository.findAll();
    }

    public Task getTaskById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new HttpStatusException(HttpStatus.NOT_FOUND,
                "Tarefa " + id + " não encontrada"));
    }

    @Transactional
    public Task createtask(Task task) {

        task.setStatus("PENDING");

        // 1. Salva a tarefa no PostgreSQL
        Task savedTask = taskRepository.save(task);

        // 2. Cria o evento
        TaskCreatedEvent event = new TaskCreatedEvent(
                savedTask.getId(),
                savedTask.getTitle(),
                savedTask.getDescription(),
                savedTask.getStatus()
        );

        // 3. Publica no Kafka
        taskProducer.publish(event);

        // 4. Retorna a tarefa salva
        return savedTask;
    }
}
