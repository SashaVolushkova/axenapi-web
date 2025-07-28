package pro.axenix_innovation.axenapi.web.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class NotServiceNode extends Exception {
    private final UUID nodeId;
    public NotServiceNode(@NotNull @Valid UUID nodeId) {
        this.nodeId = nodeId;
    }

    public UUID getNodeId() {
        return nodeId;
    }
}
