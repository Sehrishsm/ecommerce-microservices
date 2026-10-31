package com.ecommerce.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ecommerce.product.dto.InventoryReservationItem;
import com.ecommerce.product.entity.Inventory;
import com.ecommerce.product.entity.InventoryReservation;
import com.ecommerce.product.repository.InventoryRepository;
import com.ecommerce.product.repository.InventoryReservationRepository;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

        @Mock
        private InventoryRepository inventoryRepository;

        @Mock
        private ProductService productService;

        @Mock
        private InventoryReservationRepository reservationRepository;

        private InventoryService inventoryService;

        @BeforeEach
        void setUp() {
                inventoryService = new InventoryService(
                                inventoryRepository,
                                reservationRepository,
                                productService);
        }

        @Test
        void getInventoryShouldReturnExistingInventory() {
                Inventory inventory = new Inventory(1L, 50);

                when(inventoryRepository.findByProductId(1L))
                                .thenReturn(Optional.of(inventory));

                Inventory result = inventoryService.getInventory(1L);

                assertEquals(50, result.getQuantity());
                assertEquals(1L, result.getProductId());
        }

        @Test
        void getInventoryShouldRejectMissingInventory() {
                when(inventoryRepository.findByProductId(999L))
                                .thenReturn(Optional.empty());

                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> inventoryService.getInventory(999L));

                assertEquals(
                                "Inventory not found for product: 999",
                                exception.getMessage());
        }

        @Test
        void createInventoryShouldSaveInventoryForExistingProduct() {
                when(inventoryRepository.findByProductId(1L))
                                .thenReturn(Optional.empty());

                when(inventoryRepository.save(any(Inventory.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                Inventory result = inventoryService.createInventory(1L, 50);

                assertEquals(1L, result.getProductId());
                assertEquals(50, result.getQuantity());

                verify(productService).getProductById(1L);
                verify(inventoryRepository).save(any(Inventory.class));
        }

        @Test
        void createInventoryShouldAllowZeroQuantity() {
                when(inventoryRepository.findByProductId(1L))
                                .thenReturn(Optional.empty());

                when(inventoryRepository.save(any(Inventory.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                Inventory result = inventoryService.createInventory(1L, 0);

                assertEquals(0, result.getQuantity());
                verify(inventoryRepository).save(any(Inventory.class));
        }

        @Test
        void createInventoryShouldRejectNullOrNegativeQuantity() {
                assertThrows(
                                IllegalArgumentException.class,
                                () -> inventoryService.createInventory(1L, null));

                assertThrows(
                                IllegalArgumentException.class,
                                () -> inventoryService.createInventory(1L, -1));

                verify(productService, never()).getProductById(1L);
                verify(inventoryRepository, never()).save(any(Inventory.class));
        }

        @Test
        void createInventoryShouldRejectDuplicateInventory() {
                when(inventoryRepository.findByProductId(1L))
                                .thenReturn(Optional.of(new Inventory(1L, 50)));

                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> inventoryService.createInventory(1L, 10));

                assertEquals(
                                "Inventory already exists for product: 1",
                                exception.getMessage());

                verify(productService).getProductById(1L);
                verify(inventoryRepository, never()).save(any(Inventory.class));
        }

        @Test
        void createInventoryShouldRejectMissingProduct() {
                when(productService.getProductById(999L))
                                .thenThrow(new IllegalArgumentException("Product not found: 999"));

                assertThrows(
                                IllegalArgumentException.class,
                                () -> inventoryService.createInventory(999L, 10));

                verify(inventoryRepository, never()).save(any(Inventory.class));
        }

        @Test
        void addStockShouldIncreaseQuantity() {
                Inventory inventory = new Inventory(1L, 50);

                when(inventoryRepository.findByProductId(1L))
                                .thenReturn(Optional.of(inventory));

                when(inventoryRepository.save(any(Inventory.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                Inventory result = inventoryService.addStock(1L, 10);

                assertEquals(60, result.getQuantity());
                verify(inventoryRepository).save(inventory);
        }

        @Test
        void addStockShouldRejectNullOrNonPositiveQuantity() {
                assertThrows(
                                IllegalArgumentException.class,
                                () -> inventoryService.addStock(1L, null));

                assertThrows(
                                IllegalArgumentException.class,
                                () -> inventoryService.addStock(1L, 0));

                assertThrows(
                                IllegalArgumentException.class,
                                () -> inventoryService.addStock(1L, -5));

                verify(inventoryRepository, never()).save(any(Inventory.class));
        }

        @Test
        void addStockShouldRejectIntegerOverflow() {
                Inventory inventory = new Inventory(1L, Integer.MAX_VALUE);

                when(inventoryRepository.findByProductId(1L))
                                .thenReturn(Optional.of(inventory));

                assertThrows(
                                ArithmeticException.class,
                                () -> inventoryService.addStock(1L, 1));

                assertEquals(Integer.MAX_VALUE, inventory.getQuantity());
                verify(inventoryRepository, never()).save(any(Inventory.class));
        }

        @Test
        void removeStockShouldDecreaseQuantity() {
                Inventory inventory = new Inventory(1L, 50);

                when(inventoryRepository.findByProductId(1L))
                                .thenReturn(Optional.of(inventory));

                when(inventoryRepository.save(any(Inventory.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                Inventory result = inventoryService.removeStock(1L, 15);

                assertEquals(35, result.getQuantity());
                verify(inventoryRepository).save(inventory);
        }

        @Test
        void removeStockShouldAllowRemovingAllRemainingStock() {
                Inventory inventory = new Inventory(1L, 10);

                when(inventoryRepository.findByProductId(1L))
                                .thenReturn(Optional.of(inventory));

                when(inventoryRepository.save(any(Inventory.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                Inventory result = inventoryService.removeStock(1L, 10);

                assertEquals(0, result.getQuantity());
                verify(inventoryRepository).save(inventory);
        }

        @Test
        void removeStockShouldRejectInsufficientStock() {
                Inventory inventory = new Inventory(1L, 5);

                when(inventoryRepository.findByProductId(1L))
                                .thenReturn(Optional.of(inventory));

                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> inventoryService.removeStock(1L, 6));

                assertEquals("Insufficient stock", exception.getMessage());
                assertEquals(5, inventory.getQuantity());
                verify(inventoryRepository, never()).save(any(Inventory.class));
        }

        @Test
        void removeStockShouldRejectNullOrNonPositiveQuantity() {
                assertThrows(
                                IllegalArgumentException.class,
                                () -> inventoryService.removeStock(1L, null));

                assertThrows(
                                IllegalArgumentException.class,
                                () -> inventoryService.removeStock(1L, 0));

                assertThrows(
                                IllegalArgumentException.class,
                                () -> inventoryService.removeStock(1L, -1));

                verify(inventoryRepository, never()).save(any(Inventory.class));
        }

        @Test
        void reserveStockForOrderShouldCombineDuplicateProductLines() {
                Inventory inventory = new Inventory(10L, 20);

                when(inventoryRepository.findByProductIdForUpdate(10L))
                                .thenReturn(Optional.of(inventory));

                when(reservationRepository.findByOrderIdAndProductIdForUpdate(100L, 10L))
                                .thenReturn(Optional.empty());

                when(inventoryRepository.save(any(Inventory.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                when(reservationRepository.save(any(InventoryReservation.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                List<InventoryReservation> result = inventoryService.reserveStockForOrder(
                                100L,
                                List.of(
                                                new InventoryReservationItem(10L, 2),
                                                new InventoryReservationItem(10L, 3)));

                assertEquals(1, result.size());
                assertEquals(5, result.get(0).getQuantity());
                assertEquals(15, inventory.getQuantity());

                verify(inventoryRepository, times(1))
                                .findByProductIdForUpdate(10L);
                verify(reservationRepository, times(1))
                                .save(any(InventoryReservation.class));
        }

        @Test
        void reserveStockShouldReturnExistingReservationWithoutDeductingStockAgain() {
                Inventory inventory = new Inventory(10L, 15);
                InventoryReservation existing = new InventoryReservation(100L, 10L, 5);

                when(inventoryRepository.findByProductIdForUpdate(10L))
                                .thenReturn(Optional.of(inventory));

                when(reservationRepository.findByOrderIdAndProductIdForUpdate(100L, 10L))
                                .thenReturn(Optional.of(existing));

                InventoryReservation result = inventoryService.reserveStock(100L, 10L, 5);

                assertSame(existing, result);
                assertEquals(15, inventory.getQuantity());

                verify(inventoryRepository, never()).save(any(Inventory.class));
                verify(reservationRepository, never())
                                .save(any(InventoryReservation.class));
        }

        @Test
        void reserveStockShouldRejectInsufficientStock() {
                Inventory inventory = new Inventory(10L, 4);

                when(inventoryRepository.findByProductIdForUpdate(10L))
                                .thenReturn(Optional.of(inventory));

                when(reservationRepository.findByOrderIdAndProductIdForUpdate(100L, 10L))
                                .thenReturn(Optional.empty());

                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> inventoryService.reserveStock(100L, 10L, 5));

                assertEquals("Insufficient stock", exception.getMessage());
                assertEquals(4, inventory.getQuantity());

                verify(inventoryRepository, never()).save(any(Inventory.class));
                verify(reservationRepository, never())
                                .save(any(InventoryReservation.class));
        }

        @Test
        void reserveStockForOrderShouldRejectEmptyItems() {
                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> inventoryService.reserveStockForOrder(100L, List.of()));

                assertEquals(
                                "At least one inventory item is required",
                                exception.getMessage());

                verify(inventoryRepository, never())
                                .findByProductIdForUpdate(any());
        }
}