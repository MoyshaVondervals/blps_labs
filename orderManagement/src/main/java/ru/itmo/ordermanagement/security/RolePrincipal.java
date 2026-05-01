package ru.itmo.ordermanagement.security;

import java.security.Principal;
import java.util.Objects;


public class RolePrincipal implements Principal {

    private final String roleName;

    public RolePrincipal(String roleName) {
        if (roleName == null || roleName.trim().isEmpty()) {
            throw new IllegalArgumentException("Role name cannot be null or empty");
        }
        this.roleName = roleName;
    }

    @Override
    public String getName() {
        return roleName;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RolePrincipal that = (RolePrincipal) o;
        return Objects.equals(roleName, that.roleName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roleName);
    }
}
