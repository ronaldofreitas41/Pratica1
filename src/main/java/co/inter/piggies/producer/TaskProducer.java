package co.inter.piggies.producer;

import co.inter.piggies.events.TaskCreatedEvent;
import co.inter.piggies.kafka.Topics;
import io.micronaut.configuration.kafka.annotation.KafkaClient;
import io.micronaut.configuration.kafka.annotation.Topic;


/*
 * @KafkaClient: Informa que essa classe sera um producer
 */

@KafkaClient
public interface TaskProducer {

    /*
     * @Topic: Define qual topico a mensagem será publicada
     */
    @Topic(Topics.TASKS)
    void publish(TaskCreatedEvent event);
}
