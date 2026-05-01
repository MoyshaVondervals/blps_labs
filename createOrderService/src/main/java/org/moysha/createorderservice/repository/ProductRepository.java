package org.moysha.createorderservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.moysha.createorderservice.model.entity.Product;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Override
    @EntityGraph(attributePaths = {"seller"})
    Optional<Product> findById(Long id);

    @EntityGraph(attributePaths = {"seller"})
    Page<Product> findBySellerId(Long sellerId, Pageable pageable);

    @EntityGraph(attributePaths = {"seller"})
    Page<Product> findBySellerIdAndAvailableTrue(Long sellerId, Pageable pageable);
}
