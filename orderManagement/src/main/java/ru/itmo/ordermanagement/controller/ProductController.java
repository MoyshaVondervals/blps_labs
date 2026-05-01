package ru.itmo.ordermanagement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itmo.ordermanagement.dto.CreateProductRequest;
import ru.itmo.ordermanagement.dto.ProductResponse;
import ru.itmo.ordermanagement.dto.UpdateProductRequest;
import ru.itmo.ordermanagement.service.ProductService;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "CRUD для товаров продавца")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @Operation(summary = "Создать товар",
            description = "Продавец добавляет новый товар в свой каталог")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }

    @PutMapping("/{productId}")
    @Operation(summary = "Обновить товар",
            description = "Продавец обновляет название, описание, цену или доступность товара")
    public ResponseEntity<ProductResponse> update(
            @PathVariable Long productId,
            @Valid @RequestBody UpdateProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(productId, request));
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Удалить товар")
    public ResponseEntity<Void> delete(@PathVariable Long productId) {
        productService.deleteProduct(productId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Получить товар по ID")
    public ResponseEntity<ProductResponse> getById(@PathVariable Long productId) {
        return ResponseEntity.ok(productService.getProduct(productId));
    }

    @GetMapping("/seller/{sellerId}")
    @Operation(summary = "Получить товары продавца с пагинацией")
    public ResponseEntity<Page<ProductResponse>> getBySeller(
            @PathVariable Long sellerId,
            @PageableDefault(sort = "id") Pageable pageable) {
        return ResponseEntity.ok(productService.getProductsBySeller(sellerId, pageable));
    }

    @GetMapping("/seller/{sellerId}/available")
    @Operation(summary = "Получить доступные товары продавца",
            description = "Только товары с available=true — для отображения покупателю")
    public ResponseEntity<Page<ProductResponse>> getAvailableBySeller(@PathVariable Long sellerId,
                                                                      @PageableDefault Pageable pageable) {
        return ResponseEntity.ok(productService.getAvailableProductsBySeller(sellerId, pageable));
    }
}

