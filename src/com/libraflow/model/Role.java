package com.libraflow.model;

import java.io.Serializable;

/**
 * Defines role-based access levels within the LibraFlow Pro system.
 */
public enum Role implements Serializable {
    ADMIN("Administrator", "Full system control, user management, catalog & financial configuration"),
    LIBRARIAN("Librarian", "Manage book inventory, issue/return loans, process reservations & fines"),
    MEMBER("Member", "Search catalog, borrow books, place reservations & view personal history");

    private final String displayName;
    private final String permissionsSummary;

    Role(String displayName, String permissionsSummary) {
        this.displayName = displayName;
        this.permissionsSummary = permissionsSummary;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPermissionsSummary() {
        return permissionsSummary;
    }
}
