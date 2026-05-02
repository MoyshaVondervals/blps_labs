package ru.itmo.searchcourierservice.repository;

import ru.itmo.searchcourierservice.model.entity.Courier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourierRepository extends JpaRepository<Courier, Long> {
    Optional<Courier> findFirstByAvailableTrue();
}

