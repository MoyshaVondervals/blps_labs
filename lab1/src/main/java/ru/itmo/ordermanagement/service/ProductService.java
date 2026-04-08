package ru.itmo.ordermanagement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.ordermanagement.dto.CreateProductRequest;
import ru.itmo.ordermanagement.dto.ProductResponse;
import ru.itmo.ordermanagement.dto.UpdateProductRequest;
import ru.itmo.ordermanagement.exception.ResourceNotFoundException;
import ru.itmo.ordermanagement.model.entity.Product;
import ru.itmo.ordermanagement.model.entity.Seller;
import ru.itmo.ordermanagement.repository.ProductRepository;
import ru.itmo.ordermanagement.repository.SellerRepository;

import static ru.itmo.ordermanagement.security.Privilege.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final SellerRepository sellerRepository;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @PreAuthorize("hasAuthority(" + CREATE_PRODUCT + ")")
    public ProductResponse createProduct(CreateProductRequest request) {
        Seller seller = sellerRepository.findById(request.getSellerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Seller not found: " + request.getSellerId()));

        Product product = Product.builder()
                .seller(seller)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .available(true)
                .build();

        product = productRepository.save(product);
        log.info("Product '{}' created for seller #{}", product.getName(), seller.getId());
        return toResponse(product);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    @PreAuthorize("hasAuthority(" + EDIT_PRODUCT + ")")
    public ProductResponse updateProduct(Long productId, UpdateProductRequest request) {
        Product product = findProductOrThrow(productId);

        if (request.getName() != null) {
            product.setName(request.getName());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
        }
        if (request.getAvailable() != null) {
            product.setAvailable(request.getAvailable());
        }

        product = productRepository.save(product);
        log.info("Product #{} updated", productId);
        return toResponse(product);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    @PreAuthorize("hasAuthority(" + DELETE_PRODUCT + ")")
    public void deleteProduct(Long productId) {
        Product product = findProductOrThrow(productId);
        productRepository.delete(product);
        log.info("Product #{} deleted", productId);
    }


    public ProductResponse getProduct(Long productId) {
        return toResponse(findProductOrThrow(productId));
    }

    public Page<ProductResponse> getProductsBySeller(Long sellerId, Pageable pageable) {
        return productRepository.findBySellerId(sellerId, pageable)
                .map(this::toResponse);
    }

    public Page<ProductResponse> getAvailableProductsBySeller(Long sellerId, Pageable pageable) {
        return productRepository.findBySellerIdAndAvailableTrue(sellerId, pageable)
                .map(this::toResponse);
    }

    private Product findProductOrThrow(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
    }

    public ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .sellerId(product.getSeller().getId())
                .sellerName(product.getSeller().getName())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .available(product.getAvailable())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}

