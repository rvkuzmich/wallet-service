package ru.kuzmich.walletservice.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.TestcontainersConfiguration;
import ru.kuzmich.walletservice.dto.WalletOperationRequest;
import ru.kuzmich.walletservice.dto.WalletResponse;
import ru.kuzmich.walletservice.model.OperationType;
import ru.kuzmich.walletservice.model.Wallet;
import ru.kuzmich.walletservice.repository.WalletRepository;
import ru.kuzmich.walletservice.service.WalletService;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@EnableRetry
@TestPropertySource(properties = {
    "spring.retry.max-attempts=3",
    "spring.retry.backoff.delay=100"
})
class WalletIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
      .withDatabaseName("testdb")
      .withUsername("test")
      .withPassword("test");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.liquibase.enabled", () -> "true");
  }

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private WalletRepository walletRepository;

  @Autowired
  private WalletService walletService;

  private final UUID testWalletId = UUID.randomUUID();

  @Test
  void testCompleteWalletFlow() throws Exception {
    UUID walletId = UUID.randomUUID();

    mockMvc.perform(post("/api/v1/wallets/{walletId}/create", walletId))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.balance").value(0));

    mockMvc.perform(get("/api/v1/wallets/{walletId}", walletId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.balance").value(0));

    String depositRequest = """
            {
                "walletId": "%s",
                "operationType": "DEPOSIT",
                "amount": 1500.50
            }
            """.formatted(walletId);

    mockMvc.perform(post("/api/v1/wallets")
            .contentType(MediaType.APPLICATION_JSON)
            .content(depositRequest))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.balance").value(1500.50));

    String withdrawRequest = """
            {
                "walletId": "%s",
                "operationType": "WITHDRAW",
                "amount": 500.25
            }
            """.formatted(walletId);

    mockMvc.perform(post("/api/v1/wallets")
            .contentType(MediaType.APPLICATION_JSON)
            .content(withdrawRequest))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.balance").value(1000.25));

    String insufficientFundsRequest = """
            {
                "walletId": "%s",
                "operationType": "WITHDRAW",
                "amount": 2000.00
            }
            """.formatted(walletId);

    mockMvc.perform(post("/api/v1/wallets")
            .contentType(MediaType.APPLICATION_JSON)
            .content(insufficientFundsRequest))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Bad Request"));

    mockMvc.perform(get("/api/v1/wallets/{walletId}", walletId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.balance").value(1000.25));
  }

  @Test
  void testNonExistentWalletOperations() throws Exception {
    UUID nonExistentWalletId = UUID.randomUUID();

    mockMvc.perform(get("/api/v1/wallets/{walletId}", nonExistentWalletId))
        .andExpect(status().isNotFound());

    String request = """
            {
                "walletId": "%s",
                "operationType": "DEPOSIT",
                "amount": 1000.00
            }
            """.formatted(nonExistentWalletId);

    mockMvc.perform(post("/api/v1/wallets")
            .contentType(MediaType.APPLICATION_JSON)
            .content(request))
        .andExpect(status().isNotFound());
  }

  @Test
  void testRetryOnOptimisticLockingFailure() {
    WalletOperationRequest request = new WalletOperationRequest();
    request.setWalletId(testWalletId);
    request.setOperationType(OperationType.DEPOSIT);
    request.setAmount(new BigDecimal("1000.00"));

    Wallet wallet = Wallet.builder()
        .id(testWalletId)
        .balance(BigDecimal.ZERO)
        .version(0L)
        .build();

    Wallet savedWallet = Wallet.builder()
        .id(testWalletId)
        .balance(new BigDecimal("1000.00"))
        .version(1L)
        .build();

    when(walletRepository.findByIdForUpdate(testWalletId))
        .thenThrow(new OptimisticLockingFailureException("Version conflict"))
        .thenReturn(Optional.of(wallet));

    when(walletRepository.save(any(Wallet.class))).thenReturn(savedWallet);

    WalletResponse response = walletService.processOperation(request);

    assertNotNull(response);
    assertEquals(testWalletId, response.getWalletId());
    assertEquals(new BigDecimal("1000.00"), response.getBalance());

    verify(walletRepository, times(2)).findByIdForUpdate(testWalletId);
    verify(walletRepository, times(1)).save(any(Wallet.class));
  }

  @Test
  void testMaxRetriesExceeded() {
    WalletOperationRequest request = new WalletOperationRequest();
    request.setWalletId(testWalletId);
    request.setOperationType(OperationType.DEPOSIT);
    request.setAmount(new BigDecimal("1000.00"));

    when(walletRepository.findByIdForUpdate(testWalletId))
        .thenThrow(new OptimisticLockingFailureException("Version conflict"));

    OptimisticLockingFailureException exception = assertThrows(
        OptimisticLockingFailureException.class,
        () -> walletService.processOperation(request)
    );

    assertEquals("Version conflict", exception.getMessage());

    verify(walletRepository, times(3)).findByIdForUpdate(testWalletId);
    verify(walletRepository, never()).save(any(Wallet.class));
  }
}
