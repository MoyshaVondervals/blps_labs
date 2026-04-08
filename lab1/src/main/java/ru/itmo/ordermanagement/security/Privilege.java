package ru.itmo.ordermanagement.security;


public final class Privilege {
    private Privilege() {}
    public static final String VIEW_CATALOG = "VIEW_CATALOG";
    public static final String CREATE_ORDER = "CREATE_ORDER";
    public static final String VIEW_OWN_ORDERS = "VIEW_OWN_ORDERS";
    public static final String CREATE_PRODUCT = "CREATE_PRODUCT";
    public static final String EDIT_PRODUCT = "EDIT_PRODUCT";
    public static final String DELETE_PRODUCT = "DELETE_PRODUCT";
    public static final String VIEW_ALL_ORDERS = "VIEW_ALL_ORDERS";
    public static final String PROCESS_ORDER = "PROCESS_ORDER";
    public static final String VIEW_DELIVERY_ORDERS = "VIEW_DELIVERY_ORDERS";
    public static final String CHANGE_DELIVERY_STATUS = "CHANGE_DELIVERY_STATUS";
    public static final String DELIVER_ORDER = "DELIVER_ORDER";
    public static final String ACCEPT_DELIVERY = "ACCEPT_DELIVERY";
    public static final String CHANGE_ORDER_STATUS = "CHANGE_ORDER_STATUS";
    public static final String CANCEL_ORDER = "CANCEL_ORDER";
    public static final String SEARCH_COURIER = "SEARCH_COURIER";
    public static final String GET_ORDER_TO_DELIVERY = "GET_ORDER_TO_DELIVERY";

}
