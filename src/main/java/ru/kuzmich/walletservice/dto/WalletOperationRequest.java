package ru.kuzmich.walletservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Data;
import ru.kuzmich.walletservice.model.OperationType;

@Data
public class WalletOperationRequest {
  @NotNull(message = "walletId is required")
  private UUID walletId;

  @NotNull(message = "operationType is required")
  private OperationType operationType;

  @NotNull(message = "amount is required")
  @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
  private BigDecimal amount;
}
