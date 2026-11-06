package co.inter.piggies.repository;

import co.inter.piggies.entity.Task;
import io.micronaut.data.jpa.repository.JpaRepository;
import jakarta.data.repository.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
}
