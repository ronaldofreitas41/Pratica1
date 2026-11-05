package co.inter.piggies.repository;

import co.inter.piggies.entity.Task;
import io.micronaut.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {
}
