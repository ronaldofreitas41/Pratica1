package co.inter.piggies.service;

import co.inter.piggies.entity.Task;
import co.inter.piggies.repository.TaskRepository;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class TaskService {

    @Inject
    TaskRepository taskRepository;

    public Task createtask() {
        return null;
    }
}
