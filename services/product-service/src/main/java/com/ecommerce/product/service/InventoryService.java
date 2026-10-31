package com.ecommerce.product.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.product.dto.InventoryReservationItem;
import com.ecommerce.product.entity.Inventory;
import com.ecommerce.product.entity.InventoryReservation;
import com.ecommerce.product.repository.InventoryRepository;
import com.ecommerce.product.repository.InventoryReservationRepository;

@Service
@Transactional(readOnly = true)
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository reservationRepository;
    private final ProductService productService;

    public InventoryService(
            InventoryRepository inventoryRepository,
            InventoryReservationRepository reservationRepository,
            ProductService productService) {
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
        this.productService = productService;
    }

    public Inventory getInventory(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Inventory not found for product: " + productId));
    }

    public InventoryReservation getReservation(
            Long orderId, Long productId) {

        validatePositiveId(orderId, "Order ID");
        validatePositiveId(productId, "Product ID");

        return reservationRepository
                .findByOrderIdAndProductId(orderId, productId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Reservation not found for order " + orderId
                                + " and product " + productId));
    }

    @Transactional
    public Inventory createInventory(Long productId, Integer quantity) {
        if (quantity == null || quantity < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative");
        }
        productService.getProductById(productId);
        if (inventoryRepository.findByProductId(productId).isPresent()) {
            throw new IllegalArgumentException(
                    "Inventory already exists for product: " + productId);
        }
        return inventoryRepository.save(new Inventory(productId, quantity));
    }

    @Transactional
    public Inventory addStock(Long productId, Integer quantity) {
        validatePositiveQuantity(quantity);
        Inventory inventory = getInventory(productId);
        inventory.setQuantity(Math.addExact(inventory.getQuantity(), quantity));
        return inventoryRepository.save(inventory);
    }

    @Transactional
    public Inventory removeStock(Long productId, Integer quantity) {
        validatePositiveQuantity(quantity);
        Inventory inventory = getInventory(productId);
        if (inventory.getQuantity() < quantity) {
            throw new IllegalArgumentException("Insufficient stock");
        }
        inventory.setQuantity(inventory.getQuantity() - quantity);
        return inventoryRepository.save(inventory);
    }

    @Transactional
    public InventoryReservation reserveStock(
            Long orderId, Long productId, Integer quantity) {
        validatePositiveId(orderId, "Order ID");
        validatePositiveId(productId, "Product ID");
        validatePositiveQuantity(quantity);

        Inventory inventory = inventoryRepository
                .findByProductIdForUpdate(productId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Inventory not found for product: " + productId));

        var existing = reservationRepository
                .findByOrderIdAndProductIdForUpdate(orderId, productId);

        if (existing.isPresent()) {
            InventoryReservation reservation = existing.get();

            if (!reservation.getQuantity().equals(quantity)) {
                throw new IllegalArgumentException(
                        "Reservation retry quantity does not match the original request");
            }

            if (reservation.getStatus() == InventoryReservation.Status.RELEASED) {
                throw new IllegalStateException(
                        "This order's inventory reservation has already been released");
            }

            // A retry returns the existing reservation without deducting stock again.
            return reservation;
        }

        if (inventory.getQuantity() < quantity) {
            throw new IllegalArgumentException("Insufficient stock");
        }

        inventory.setQuantity(inventory.getQuantity() - quantity);
        inventoryRepository.save(inventory);

        return reservationRepository.save(
                new InventoryReservation(orderId, productId, quantity));
    }

    @Transactional
    public InventoryReservation releaseReservation(Long orderId, Long productId) {
        validatePositiveId(orderId, "Order ID");
        validatePositiveId(productId, "Product ID");

        InventoryReservation initial = reservationRepository
                .findByOrderIdAndProductId(orderId, productId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Reservation not found for order " + orderId
                                + " and product " + productId));

        Inventory inventory = inventoryRepository
                .findByProductIdForUpdate(initial.getProductId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Inventory not found for product: " + productId));

        InventoryReservation reservation = reservationRepository
                .findByOrderIdAndProductIdForUpdate(orderId, productId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Reservation not found for order " + orderId
                                + " and product " + productId));

        if (reservation.getStatus() == InventoryReservation.Status.RELEASED) {
            return reservation;
        }

        if (reservation.getStatus() == InventoryReservation.Status.COMMITTED) {
            throw new IllegalStateException(
                    "A committed inventory reservation cannot be released");
        }

        inventory.setQuantity(Math.addExact(
                inventory.getQuantity(), reservation.getQuantity()));
        reservation.release();

        inventoryRepository.save(inventory);
        return reservationRepository.save(reservation);
    }

    @Transactional
    public InventoryReservation commitReservation(Long orderId, Long productId) {
        validatePositiveId(orderId, "Order ID");
        validatePositiveId(productId, "Product ID");

        InventoryReservation initial = reservationRepository
                .findByOrderIdAndProductId(orderId, productId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Reservation not found for order " + orderId
                                + " and product " + productId));

        // Use the same inventory-then-reservation lock order as reserve/release.
        inventoryRepository.findByProductIdForUpdate(initial.getProductId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Inventory not found for product: " + productId));

        InventoryReservation reservation = reservationRepository
                .findByOrderIdAndProductIdForUpdate(orderId, productId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Reservation not found for order " + orderId
                                + " and product " + productId));

        reservation.commit();
        return reservationRepository.save(reservation);
    }

    @Transactional
    public List<InventoryReservation> reserveStockForOrder(
            Long orderId,
            List<InventoryReservationItem> items) {

        validatePositiveId(orderId, "Order ID");

        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one inventory item is required");
        }

        // Combine duplicate product lines into one reservation per product.
        Map<Long, Integer> quantitiesByProduct = new LinkedHashMap<>();

        for (InventoryReservationItem item : items) {
            if (item == null) {
                throw new IllegalArgumentException(
                        "Inventory item must not be null");
            }

            validatePositiveId(item.productId(), "Product ID");
            validatePositiveQuantity(item.quantity());

            quantitiesByProduct.merge(
                    item.productId(),
                    item.quantity(),
                    Math::addExact);
        }

        List<InventoryReservation> reservations = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : quantitiesByProduct.entrySet()) {

            reservations.add(reserveStock(
                    orderId,
                    entry.getKey(),
                    entry.getValue()));
        }

        return List.copyOf(reservations);
    }

    private void validatePositiveQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be greater than zero");
        }
    }

    private void validatePositiveId(Long id, String name) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }
}