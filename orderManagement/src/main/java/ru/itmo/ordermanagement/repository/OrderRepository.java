package ru.itmo.ordermanagement.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.itmo.ordermanagement.model.entity.Order;
import ru.itmo.ordermanagement.model.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Override
    @EntityGraph(attributePaths = {"customer", "seller", "courier", "items", "items.product"})
    Optional<Order> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"customer", "seller", "courier", "items", "items.product"})
    Page<Order> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "seller", "courier", "items", "items.product"})
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "seller", "courier", "items", "items.product"})
    Page<Order> findByCustomerId(Long customerId, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "seller", "courier", "items", "items.product"})
    Page<Order> findBySellerId(Long sellerId, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "seller", "courier", "items", "items.product"})
    Page<Order> findByCourierId(Long courierId, Pageable pageable);

    Page<Order> findByStatusAndSellerNotifiedAtBefore(OrderStatus status, LocalDateTime deadline, Pageable pageable);

    Page<Order> findByStatusAndCourierAssignedAtBefore(OrderStatus status, LocalDateTime deadline, Pageable pageable);
}
