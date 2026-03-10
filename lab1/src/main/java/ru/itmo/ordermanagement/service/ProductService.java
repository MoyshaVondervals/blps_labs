package ru.itmo.ordermanagement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.ordermanagement.dto.CreateProductRequest;
import ru.itmo.ordermanagement.dto.ProductResponse;
import ru.itmo.ordermanagement.dto.UpdateProductRequest;
import ru.itmo.ordermanagement.exception.ResourceNotFoundException;
import ru.itmo.ordermanagement.model.entity.Product;
import ru.itmo.ordermanagement.model.entity.Seller;
import ru.itmo.ordermanagement.repository.ProductRepository;
import ru.itmo.ordermanagement.repository.SellerRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final SellerRepository sellerRepository;

    @Transactional
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

    @Transactional
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

    @Transactional
    public void deleteProduct(Long productId) {
        Product product = findProductOrThrow(productId);
        productRepository.delete(product);
        log.info("Product #{} deleted", productId);
    }

    public ProductResponse getProduct(Long productId) {
        return toResponse(findProductOrThrow(productId));
    }

    public List<ProductResponse> getProductsBySeller(Long sellerId) {
        return productRepository.findBySellerId(sellerId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<ProductResponse> getAvailableProductsBySeller(Long sellerId) {
        return productRepository.findBySellerIdAndAvailableTrue(sellerId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
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

