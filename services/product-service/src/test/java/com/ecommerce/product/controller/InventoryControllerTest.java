package com.ecommerce.product.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;


import com.ecommerce.product.entity.Inventory;
import com.ecommerce.product.entity.InventoryReservation;
import com.ecommerce.product.dto.InventoryReservationItem;
import com.ecommerce.product.exception.GlobalExceptionHandler;
import com.ecommerce.product.service.InventoryService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class InventoryControllerTest {

    @Mock
    private InventoryService inventoryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new InventoryController(inventoryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getInventoryShouldReturnInventory() throws Exception {
        when(inventoryService.getInventory(1L))
                .thenReturn(new Inventory(1L, 10));

        mockMvc.perform(get("/inventory/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.quantity").value(10));

        verify(inventoryService).getInventory(1L);
    }

    @Test
    void createInventoryShouldReturnCreatedInventory() throws Exception {
        when(inventoryService.createInventory(1L, 10))
                .thenReturn(new Inventory(1L, 10));

        mockMvc.perform(post("/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": 1,
                                  "quantity": 10
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.quantity").value(10));

        verify(inventoryService).createInventory(1L, 10);
    }

    @Test
    void addStockShouldReturnUpdatedInventory() throws Exception {
        when(inventoryService.addStock(1L, 5))
                .thenReturn(new Inventory(1L, 15));

        mockMvc.perform(post("/inventory/1/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quantity": 5
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.quantity").value(15));

        verify(inventoryService).addStock(1L, 5);
    }

    @Test
    void removeStockShouldReturnUpdatedInventory() throws Exception {
        when(inventoryService.removeStock(1L, 3))
                .thenReturn(new Inventory(1L, 7));

        mockMvc.perform(post("/inventory/1/remove")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quantity": 3
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.quantity").value(7));

        verify(inventoryService).removeStock(1L, 3);
    }

    @Test
    void createInventoryWithNegativeQuantityShouldReturnBadRequest()
            throws Exception {
        mockMvc.perform(post("/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": 1,
                                  "quantity": -1
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inventoryService);
    }

    @Test
    void addStockWithZeroQuantityShouldReturnBadRequest()
            throws Exception {
        mockMvc.perform(post("/inventory/1/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quantity": 0
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inventoryService);
    }

    @Test
    void getMissingInventoryShouldReturnNotFound() throws Exception {
        when(inventoryService.getInventory(999L))
                .thenThrow(new IllegalArgumentException(
                        "Inventory not found for product: 999"));

        mockMvc.perform(get("/inventory/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("Inventory not found for product: 999"));
    }

    @Test
    void reserveStockForOrderShouldReturnReservationDetails() throws Exception {
        InventoryReservation reservation =
                new InventoryReservation(10L, 1L, 2);

        when(inventoryService.reserveStockForOrder(
                eq(10L), anyList()))
                .thenReturn(List.of(reservation));

        mockMvc.perform(post("/inventory/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": 10,
                                  "items": [
                                    {
                                      "productId": 1,
                                      "quantity": 2
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value(10))
                .andExpect(jsonPath("$[0].productId").value(1))
                .andExpect(jsonPath("$[0].quantity").value(2))
                .andExpect(jsonPath("$[0].status").value("RESERVED"));

        verify(inventoryService).reserveStockForOrder(
                eq(10L), anyList());
    }

    @Test
    void releaseReservationShouldReturnReleasedReservation()
            throws Exception {
        InventoryReservation reservation =
                new InventoryReservation(10L, 1L, 2);
        reservation.release();

        when(inventoryService.releaseReservation(10L, 1L))
                .thenReturn(reservation);

        mockMvc.perform(post("/inventory/reservations/10/1/release"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(10))
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.status").value("RELEASED"));

        verify(inventoryService).releaseReservation(10L, 1L);
    }

    @Test
    void commitReservationShouldReturnCommittedReservation()
            throws Exception {
        InventoryReservation reservation =
                new InventoryReservation(10L, 1L, 2);
        reservation.commit();

        when(inventoryService.commitReservation(10L, 1L))
                .thenReturn(reservation);

        mockMvc.perform(post("/inventory/reservations/10/1/commit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(10))
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.status").value("COMMITTED"));

        verify(inventoryService).commitReservation(10L, 1L);
    }
}