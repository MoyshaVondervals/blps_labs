package ru.itmo.ordermanagement.security;

import lombok.Getter;

import java.util.Set;
import java.util.stream.Collectors;

@Getter
public enum Role {
    CUSTOMER(Set.of(
            Privilege.VIEW_CATALOG,
            Privilege.CREATE_ORDER,
            Privilege.VIEW_OWN_ORDERS
    )),
    SELLER(Set.of(
            Privilege.CREATE_PRODUCT,
            Privilege.EDIT_PRODUCT,
            Privilege.DELETE_PRODUCT,
            Privilege.VIEW_CATALOG,
            Privilege.VIEW_ALL_ORDERS,
            Privilege.CHANGE_DELIVERY_STATUS,
            Privilege.CHANGE_ORDER_STATUS,
            Privilege.PROCESS_ORDER,
            Privilege.SEARCH_COURIER
    )),
    COURIER(Set.of(
            Privilege.VIEW_DELIVERY_ORDERS,
            Privilege.DELIVER_ORDER,
            Privilege.GET_ORDER_TO_DELIVERY
    )),
    ADMIN(Set.of(CUSTOMER.getPrivileges(), SELLER.getPrivileges(), COURIER.getPrivileges())
            .stream().flatMap(Set::stream).collect(Collectors.toUnmodifiableSet()));

    private final Set<String> privileges;

    Role(Set<String> privileges) {
        this.privileges = privileges;
    }
}