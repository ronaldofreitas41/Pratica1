package co.inter.piggies.events;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record TaskCreatedEvent(
        Long id,
        String title,
        String description,
        String status
)
{}
