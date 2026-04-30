package org.moysha.createorderservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.moysha.createorderservice.model.entity.Seller;

@Repository
public interface SellerRepository extends JpaRepository<Seller, Long> {
}
