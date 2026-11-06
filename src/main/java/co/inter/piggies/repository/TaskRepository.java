package co.inter.piggies.repository;

import co.inter.piggies.entity.Task;
import io.micronaut.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {
}
