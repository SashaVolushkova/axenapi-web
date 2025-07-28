package pro.axenix_innovation.axenapi.web.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.axenix_innovation.axenapi.web.model.EventDTO;
import pro.axenix_innovation.axenapi.web.model.EventGraphDTO;
import pro.axenix_innovation.axenapi.web.model.LinkDTO;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.WARN_FOUND_UNUSED_EVENTS;

public class EventService {

    private static final Logger log = LoggerFactory.getLogger(EventService.class);

    static public List<EventDTO> findUnusedEvents(EventGraphDTO graph) {
        Set<UUID> usedEventIds = graph.getLinks().stream()
                .map(LinkDTO::getEventId)
                .collect(Collectors.toSet());

        List<EventDTO> unusedEvents = graph.getEvents().stream()
                .filter(event -> !usedEventIds.contains(event.getId()))
                .collect(Collectors.toList());

        log.warn(MessageHelper.getStaticMessage(WARN_FOUND_UNUSED_EVENTS,unusedEvents.size(), unusedEvents.stream()
                .map(EventDTO::getId)
                .map(UUID::toString)
                .collect(Collectors.joining(", "))));

        return unusedEvents;
    }

}
