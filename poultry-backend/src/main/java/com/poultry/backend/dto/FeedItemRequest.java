package com.poultry.backend.dto;

import com.poultry.backend.entity.FeedStatus;
import com.poultry.backend.entity.FeedType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedItemRequest {

    @NotNull(message = "Feed code is required")
    @Size(min = 2, max = 50, message = "Feed code length must be between 2 and 50 characters")
    private String feedCode;

    @NotNull(message = "Feed name is required")
    @Size(min = 2, max = 100, message = "Feed name length must be between 2 and 100 characters")
    private String feedName;

    @NotNull(message = "Feed type is required")
    private FeedType feedType;

    private String description;

    @NotNull(message = "Unit is required")
    private String unit;

    @NotNull(message = "Minimum stock is required")
    @PositiveOrZero(message = "Minimum stock must be positive or zero")
    private Double minimumStock;

    @NotNull(message = "Current stock is required")
    @PositiveOrZero(message = "Current stock must be positive or zero")
    private Double currentStock;

    @NotNull(message = "Unit cost is required")
    @PositiveOrZero(message = "Unit cost must be positive or zero")
    private Double unitCost;

    @NotNull(message = "Storage location is required")
    private String storageLocation;

    private LocalDate expiryDate;

    @NotNull(message = "Feed status is required")
    private FeedStatus status;
}
