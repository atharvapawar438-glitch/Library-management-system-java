package com.libraflow.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Immutable audit log entry recording administrative and circulation events.
 */
public class AuditLog implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final LocalDateTime timestamp;
    private final String actorUsername;
    private final String actorRole;
    private final String actionType;
    private final String details;

    public AuditLog(String id, String actorUsername, String actorRole, String actionType, String details) {
        this.id = id;
        this.timestamp = LocalDateTime.now();
        this.actorUsername = actorUsername;
        this.actorRole = actorRole;
        this.actionType = actionType;
        this.details = details;
    }

    public String getId() { return id; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getFormattedTimestamp() {
        return timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    public String getActorUsername() { return actorUsername; }
    public String getActorRole() { return actorRole; }
    public String getActionType() { return actionType; }
    public String getDetails() { return details; }
}
