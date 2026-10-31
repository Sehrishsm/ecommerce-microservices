package com.ecommerce.product.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.product.dto.InventoryCreateRequest;
import com.ecommerce.product.dto.InventoryReservationItem;
import com.ecommerce.product.dto.InventoryReservationRequest;
import com.ecommerce.product.dto.InventoryReservationResponse;
import com.ecommerce.product.dto.InventoryResponse;
import com.ecommerce.product.dto.StockAdjustmentRequest;
import com.ecommerce.product.service.InventoryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

        private final InventoryService inventoryService;

        public InventoryController(InventoryService inventoryService) {
                this.inventoryService = inventoryService;
        }

        @GetMapping("/{productId}")
        public InventoryResponse getInventory(
                        @PathVariable("productId") Long productId) {
                return InventoryResponse.from(
                                inventoryService.getInventory(productId));
        }

        @PostMapping
        public ResponseEntity<InventoryResponse> createInventory(
                        @Valid @RequestBody InventoryCreateRequest request) {
                var inventory = inventoryService.createInventory(
                                request.productId(), request.quantity());

                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(InventoryResponse.from(inventory));
        }

        @PostMapping("/{productId}/add")
        public InventoryResponse addStock(
                        @PathVariable("productId") Long productId,
                        @Valid @RequestBody StockAdjustmentRequest request) {
                return InventoryResponse.from(
                                inventoryService.addStock(productId, request.quantity()));
        }

        @PostMapping("/{productId}/remove")
        public InventoryResponse removeStock(
                        @PathVariable("productId") Long productId,
                        @Valid @RequestBody StockAdjustmentRequest request) {
                return InventoryResponse.from(
                                inventoryService.removeStock(productId, request.quantity()));
        }

        @PostMapping("/reservations")
        public List<InventoryReservationResponse> reserveStockForOrder(
                        @RequestBody InventoryReservationRequest request) {

                List<InventoryReservationItem> items = request.items();

                return inventoryService.reserveStockForOrder(
                                request.orderId(), items)
                                .stream()
                                .map(InventoryReservationResponse::from)
                                .toList();
        }

        @PostMapping("/reservations/{orderId}/{productId}/release")
        public InventoryReservationResponse releaseReservation(
                        @PathVariable("orderId") Long orderId,
                        @PathVariable("productId") Long productId) {

                return InventoryReservationResponse.from(
                                inventoryService.releaseReservation(orderId, productId));
        }

        @PostMapping("/reservations/{orderId}/{productId}/commit")
        public InventoryReservationResponse commitReservation(
                        @PathVariable("orderId") Long orderId,
                        @PathVariable("productId") Long productId) {

                return InventoryReservationResponse.from(
                                inventoryService.commitReservation(orderId, productId));
        }

        @GetMapping("/reservations/{orderId}/{productId}")
        public InventoryReservationResponse getReservation(
                        @PathVariable("orderId") Long orderId,
                        @PathVariable("productId") Long productId) {

                return InventoryReservationResponse.from(
                                inventoryService.getReservation(orderId, productId));
        }

}
