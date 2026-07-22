package com.poultry.backend.dto;

import com.poultry.backend.entity.CustomerStatus;
import com.poultry.backend.entity.CustomerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponse {
    private Long id;
    private String customerCode;
    private String customerName;
    private String phoneNumber;
    private String email;
    private String address;
    private CustomerType customerType;
    private CustomerStatus status;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
