package ru.itmo.ordermanagement.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.itmo.ordermanagement.model.entity.Order;
import ru.itmo.ordermanagement.model.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    Page<Order> findByCustomerId(Long customerId, Pageable pageable);

    Page<Order> findBySellerId(Long sellerId, Pageable pageable);

    Page<Order> findByCourierId(Long courierId, Pageable pageable);

    Page<Order> findByStatusAndSellerNotifiedAtBefore(OrderStatus status, LocalDateTime deadline);

    Page<Order> findByStatusAndCourierAssignedAtBefore(OrderStatus status, LocalDateTime deadline);
}
