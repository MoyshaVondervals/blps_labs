package ru.itmo.ordermanagement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itmo.ordermanagement.dto.CreateCustomerRequest;
import ru.itmo.ordermanagement.exception.ResourceNotFoundException;
import ru.itmo.ordermanagement.model.entity.Customer;
import ru.itmo.ordermanagement.repository.CustomerRepository;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Tag(name = "Customers", description = "Управление заказчиками")
public class CustomerController {

    private final CustomerRepository customerRepository;

    @PostMapping
    @Operation(summary = "Создать заказчика")
    public ResponseEntity<Customer> create(@Valid @RequestBody CreateCustomerRequest request) {
        Customer customer = Customer.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(customerRepository.save(customer));
    }

    @GetMapping
    @Operation(summary = "Получить всех заказчиков")
    public ResponseEntity<Page<Customer>> getAll(@PageableDefault(sort = "id") Pageable pageable) {
        return ResponseEntity.ok(customerRepository.findAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить заказчика по ID")
    public ResponseEntity<Customer> getById(@PathVariable Long id) {
        return ResponseEntity.ok(customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id)));
    }
}
