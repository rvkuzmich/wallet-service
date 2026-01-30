package ru.kuzmich.walletservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.kuzmich.walletservice.dto.WalletOperationRequest;
import ru.kuzmich.walletservice.dto.WalletResponse;
import ru.kuzmich.walletservice.exception.InsufficientFundsException;
import ru.kuzmich.walletservice.exception.WalletNotFoundException;
import ru.kuzmich.walletservice.model.OperationType;
import ru.kuzmich.walletservice.service.WalletService;

@WebMvcTest(WalletController.class)
class WalletControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private WalletService walletService;

  private final UUID testWalletId = UUID.randomUUID();

  @Test
  void testProcessDepositOperation() throws Exception {
    WalletOperationRequest request = new WalletOperationRequest();
    request.setWalletId(testWalletId);
    request.setOperationType(OperationType.DEPOSIT);
    request.setAmount(new BigDecimal("1000.00"));

    WalletResponse response = WalletResponse.builder()
        .walletId(testWalletId)
        .balance(new BigDecimal("1000.00"))
        .build();

    when(walletService.processOperation(any())).thenReturn(response);

    mockMvc.perform(post("/api/v1/wallet")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.walletId").value(testWalletId.toString()))
        .andExpect(jsonPath("$.balance").value(1000.00));
  }

  @Test
  void testGetWalletBalance() throws Exception {
    WalletResponse response = WalletResponse.builder()
        .walletId(testWalletId)
        .balance(new BigDecimal("500.00"))
        .build();

    when(walletService.getWalletBalance(testWalletId)).thenReturn(response);

    mockMvc.perform(get("/api/v1/wallets/{walletId}", testWalletId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.walletId").value(testWalletId.toString()))
        .andExpect(jsonPath("$.balance").value(500.00));
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "{ invalid json }",
      "{ walletId: invalid-uuid, operationType: DEPOSIT, amount: 1000 }",
      "{\"walletId\": \"not-a-uuid\", \"operationType\": \"INVALID\", \"amount\": -100}",
      "malformed",
      "{ missing closing brace",
      "[not an object]",
      "{\"extra\": \"field\", \"walletId\": \"123e4567-e89b-12d3-a456-426614174000\", \"operationType\": \"DEPOSIT\", \"amount\": 1000}"
  })
  void testInvalidJson(String invalidJson) throws Exception {
    mockMvc.perform(post("/api/v1/wallet")
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").exists())
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "{\"walletId\": null, \"operationType\": null, \"amount\": null}"
  })
  void testFailedJsonValidation(String invalidJson) throws Exception {
    mockMvc.perform(post("/api/v1/wallet")
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Validation Failed"))
        .andExpect(jsonPath("$.message").exists())
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  void testMissingRequiredFields() throws Exception {
    String requestWithoutRequiredFields = "{}";

    mockMvc.perform(post("/api/v1/wallet")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestWithoutRequiredFields))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.validationErrors").exists())
        .andExpect(jsonPath("$.validationErrors.walletId").value("walletId is required"))
        .andExpect(jsonPath("$.validationErrors.operationType").value("operationType is required"))
        .andExpect(jsonPath("$.validationErrors.amount").value("amount is required"));
  }

  @Test
  void testInvalidUUIDFormat() throws Exception {
    String requestWithInvalidUUID = """
            {
                "walletId": "not-a-valid-uuid",
                "operationType": "DEPOSIT",
                "amount": 1000
            }
            """;

    mockMvc.perform(post("/api/v1/wallet")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestWithInvalidUUID))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Bad Request"));
  }

  @Test
  void testInvalidOperationType() throws Exception {
    String requestWithInvalidOperation = """
            {
                "walletId": "123e4567-e89b-12d3-a456-426614174000",
                "operationType": "INVALID_OPERATION",
                "amount": 1000
            }
            """;

    mockMvc.perform(post("/api/v1/wallet")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestWithInvalidOperation))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Bad Request"));
  }

  @Test
  void testNegativeAmount() throws Exception {
    WalletOperationRequest request = new WalletOperationRequest();
    request.setWalletId(testWalletId);
    request.setOperationType(OperationType.DEPOSIT);
    request.setAmount(new BigDecimal("-100.00"));

    String jsonRequest = """
            {
                "walletId": "%s",
                "operationType": "DEPOSIT",
                "amount": -100.00
            }
            """.formatted(testWalletId);

    mockMvc.perform(post("/api/v1/wallet")
            .contentType(MediaType.APPLICATION_JSON)
            .content(jsonRequest))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.validationErrors").exists())
        .andExpect(jsonPath("$.validationErrors.amount")
            .value("Amount must be greater than 0"));
  }

  @Test
  void testZeroAmount() throws Exception {
    String jsonRequest = """
            {
                "walletId": "%s",
                "operationType": "DEPOSIT",
                "amount": 0
            }
            """.formatted(testWalletId);

    mockMvc.perform(post("/api/v1/wallet")
            .contentType(MediaType.APPLICATION_JSON)
            .content(jsonRequest))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.validationErrors.amount")
            .value("Amount must be greater than 0"));
  }

  @Test
  void testValidDepositOperation() throws Exception {
    WalletOperationRequest request = new WalletOperationRequest();
    request.setWalletId(testWalletId);
    request.setOperationType(OperationType.DEPOSIT);
    request.setAmount(new BigDecimal("1000.00"));

    WalletResponse response = WalletResponse.builder()
        .walletId(testWalletId)
        .balance(new BigDecimal("1000.00"))
        .build();

    when(walletService.processOperation(any(WalletOperationRequest.class)))
        .thenReturn(response);

    String jsonRequest = """
            {
                "walletId": "%s",
                "operationType": "DEPOSIT",
                "amount": 1000.00
            }
            """.formatted(testWalletId);

    mockMvc.perform(post("/api/v1/wallet")
            .contentType(MediaType.APPLICATION_JSON)
            .content(jsonRequest))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.walletId").value(testWalletId.toString()))
        .andExpect(jsonPath("$.balance").value(1000.00));

    verify(walletService, times(1)).processOperation(any(WalletOperationRequest.class));
  }

  @Test
  void testWalletNotFound() throws Exception {
    WalletOperationRequest request = new WalletOperationRequest();
    request.setWalletId(testWalletId);
    request.setOperationType(OperationType.DEPOSIT);
    request.setAmount(new BigDecimal("1000.00"));

    when(walletService.processOperation(any(WalletOperationRequest.class)))
        .thenThrow(new WalletNotFoundException("Wallet not found: " + testWalletId));

    String jsonRequest = """
            {
                "walletId": "%s",
                "operationType": "DEPOSIT",
                "amount": 1000.00
            }
            """.formatted(testWalletId);

    mockMvc.perform(post("/api/v1/wallet")
            .contentType(MediaType.APPLICATION_JSON)
            .content(jsonRequest))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("Not Found"))
        .andExpect(jsonPath("$.message").value("Wallet not found: " + testWalletId));
  }

  @Test
  void testInsufficientFunds() throws Exception {
    WalletOperationRequest request = new WalletOperationRequest();
    request.setWalletId(testWalletId);
    request.setOperationType(OperationType.WITHDRAW);
    request.setAmount(new BigDecimal("1000.00"));

    when(walletService.processOperation(any(WalletOperationRequest.class)))
        .thenThrow(new InsufficientFundsException("Insufficient funds"));

    String jsonRequest = """
            {
                "walletId": "%s",
                "operationType": "WITHDRAW",
                "amount": 1000.00
            }
            """.formatted(testWalletId);

    mockMvc.perform(post("/api/v1/wallet")
            .contentType(MediaType.APPLICATION_JSON)
            .content(jsonRequest))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Insufficient funds"));
  }

  @Test
  void testGetWalletBalanceSuccess() throws Exception {
    WalletResponse response = WalletResponse.builder()
        .walletId(testWalletId)
        .balance(new BigDecimal("500.00"))
        .build();

    when(walletService.getWalletBalance(testWalletId)).thenReturn(response);

    mockMvc.perform(get("/api/v1/wallets/{walletId}", testWalletId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.walletId").value(testWalletId.toString()))
        .andExpect(jsonPath("$.balance").value(500.00));

    verify(walletService, times(1)).getWalletBalance(testWalletId);
  }

  @Test
  void testGetWalletBalanceNotFound() throws Exception {
    when(walletService.getWalletBalance(testWalletId))
        .thenThrow(new WalletNotFoundException("Wallet not found"));

    mockMvc.perform(get("/api/v1/wallets/{walletId}", testWalletId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("Not Found"));
  }

  @Test
  void testInvalidWalletIdInPath() throws Exception {
    mockMvc.perform(get("/api/v1/wallets/{walletId}", "invalid-uuid"))
        .andExpect(status().isInternalServerError());
  }

  @Test
  void testConcurrentModificationRetry() throws Exception {
    String jsonRequest = """
            {
                "walletId": "%s",
                "operationType": "DEPOSIT",
                "amount": 1000.00
            }
            """.formatted(testWalletId);

    when(walletService.processOperation(any(WalletOperationRequest.class)))
        .thenThrow(new OptimisticLockingFailureException("Version conflict"));

    mockMvc.perform(post("/api/v1/wallet")
            .contentType(MediaType.APPLICATION_JSON)
            .content(jsonRequest))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value("Conflict"))
        .andExpect(jsonPath("$.message").value("Concurrent modification detected. Please retry."));

    verify(walletService, times(1)).processOperation(any(WalletOperationRequest.class));
  }

  @Test
  void testContentTypeValidation() throws Exception {
    String jsonRequest = """
            {
                "walletId": "%s",
                "operationType": "DEPOSIT",
                "amount": 1000.00
            }
            """.formatted(testWalletId);

    mockMvc.perform(post("/api/v1/wallet")
            .content(jsonRequest))
        .andExpect(status().isUnsupportedMediaType());

    mockMvc.perform(post("/api/v1/wallet")
            .contentType(MediaType.TEXT_PLAIN)
            .content(jsonRequest))
        .andExpect(status().isUnsupportedMediaType());
  }

  @Test
  void testCreateWallet() throws Exception {
    WalletResponse response = WalletResponse.builder()
        .walletId(testWalletId)
        .balance(BigDecimal.ZERO)
        .build();

    when(walletService.createWallet(testWalletId)).thenReturn(response);

    mockMvc.perform(post("/api/v1/wallets/{walletId}/create", testWalletId))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.walletId").value(testWalletId.toString()))
        .andExpect(jsonPath("$.balance").value(0));

    verify(walletService, times(1)).createWallet(testWalletId);
  }

  @Test
  void testHealthEndpoint() throws Exception {
    mockMvc.perform(get("/api/v1/health"))
        .andExpect(status().isOk())
        .andExpect(content().string("OK"));
  }
}