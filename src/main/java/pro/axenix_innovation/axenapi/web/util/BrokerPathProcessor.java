package pro.axenix_innovation.axenapi.web.util;

import io.swagger.v3.oas.models.PathItem;
import lombok.extern.slf4j.Slf4j;
import pro.axenix_innovation.axenapi.web.model.NodeDTO;

import java.util.*;

@Slf4j
public class BrokerPathProcessor {

    public static class BrokerPathInfo {
        private final String topic;
        private final String group;
        private final NodeDTO.BrokerTypeEnum brokerType;
        private final String eventName;
        private final Set<String> tags;
        private final boolean isIncoming;

        public BrokerPathInfo(String topic, String group, NodeDTO.BrokerTypeEnum brokerType, String eventName, Set<String> tags, boolean isIncoming) {
            this.topic = topic;
            this.group = group;
            this.brokerType = brokerType;
            this.eventName = eventName;
            this.tags = tags;
            this.isIncoming = isIncoming;
        }

        public String getTopic() { return topic; }
        public String getGroup() { return group; }
        public NodeDTO.BrokerTypeEnum getBrokerType() { return brokerType; }
        public String getEventName() { return eventName; }
        public Set<String> getTags() { return tags; }
        public boolean isIncoming() { return isIncoming; }
    }

    public static BrokerPathInfo processBrokerPath(String pathKey, PathItem path) {
        String[] parts = pathKey.split("/");
        if (parts.length < 2) {
            return null;
        }

        String brokerTypeString = parts[1];

        // Handle special case for undefined_broker
        if ("undefined_broker".equals(brokerTypeString)) {
            brokerTypeString = "UNDEFINED";
        }

        final String finalBrokerTypeString = brokerTypeString;
        boolean isTopic = Arrays.stream(NodeDTO.BrokerTypeEnum.values())
                .anyMatch(v -> v.getValue().equals(finalBrokerTypeString.toUpperCase()));

        if (!isTopic) {
            return null;
        }

        NodeDTO.BrokerTypeEnum brokerType = NodeDTO.BrokerTypeEnum.fromValue(finalBrokerTypeString.toUpperCase());
        String topic = parts[parts.length - 2];
        String group = null;

        if (brokerType == NodeDTO.BrokerTypeEnum.KAFKA && parts.length > 4) {
            group = parts[2];
        }

        String eventName = parts[parts.length - 1];
        Set<String> tags = TagExtractor.getTagsFromPath(path);

        // Handle undefined_event case - return null eventName to indicate no event should be created
        if ("undefined_event".equals(eventName)) {
            eventName = null;
        }

        boolean isIncoming = path.getPost() != null &&
                path.getPost().getExtensions() != null &&
                path.getPost().getExtensions().containsKey("x-incoming") &&
                (Boolean) path.getPost().getExtensions().get("x-incoming");


        return new BrokerPathInfo(topic, group, brokerType, eventName, tags, isIncoming);
    }
}