package io.github.stonley890.eyeofonyx.files;

public enum NotificationType {

    /**
     * Generic notification.
     * Deletes when read.
     */
    GENERIC,

    /**
     * Challenge sent notification.
     * Includes buttons to accept or deny.
     * Deletes when acknowledged.
     */
    CHALLENGE_REQUESTED,

    /**
     * Challenge accepted notification.
     * Includes buttons with available times.
     * Deletes when acknowledged.
     */
    CHALLENGE_ACCEPTED

}
