package co.inter.piggies.events;

public record TaskCreatedEvent(
        Long id,
        String title,
        String description,
        String status
)
{}
