package org.moysha.createorderservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.moysha.createorderservice.model.entity.Courier;

import java.util.Optional;

@Repository
public interface CourierRepository extends JpaRepository<Courier, Long> {

    Optional<Courier> findFirstByAvailableTrue();
}
