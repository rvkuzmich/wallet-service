package ru.kuzmich.walletservice.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.kuzmich.walletservice.repository.WalletRepository;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@TestPropertySource(properties = {
    "spring.liquibase.enabled=true",
    "spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yaml",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.jpa.show-sql=true",
    "logging.level.ru.kuzmich=DEBUG"
})
class WalletIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
      .withDatabaseName("wallet_test_db")
      .withUsername("wallet_user")
      .withPassword("wallet_password");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
  }

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private WalletRepository walletRepository;

  @BeforeEach
  void setUp() {
    walletRepository.deleteAll();
  }

  @Test
  @DisplayName("Создание кошелька с существующим ID должно возвращать ошибку")
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  @Commit
  void testCreateWalletWithExistingId() throws Exception {
    UUID walletId = UUID.randomUUID();

    mockMvc.perform(post("/api/v1/wallets/{walletId}/create", walletId))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/api/v1/wallets/{walletId}/create", walletId))
        .andDo(print())
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Получение несуществующего кошелька должно возвращать 404")
  void testGetNonExistentWallet() throws Exception {
    UUID nonExistentWalletId = UUID.randomUUID();
    mockMvc.perform(get("/api/v1/wallets/{walletId}", nonExistentWalletId))
        .andDo(print())
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Операции с несуществующим кошельком должны возвращать 404")
  void testOperationsOnNonExistentWallet() throws Exception {
    UUID nonExistentWalletId = UUID.randomUUID();
    String depositRequest = """
            {
                "walletId": "%s",
                "operationType": "DEPOSIT",
                "amount": 1000.00
            }
            """.formatted(nonExistentWalletId);

    mockMvc.perform(post("/api/v1/wallet")
            .contentType(MediaType.APPLICATION_JSON)
            .content(depositRequest))
        .andDo(print())
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Операция с некорректным телом запроса должна возвращать 400")
  @Transactional
  @Commit
  void testInvalidRequest() throws Exception {
    UUID walletId = UUID.randomUUID();
    mockMvc.perform(post("/api/v1/wallets/{walletId}/create", walletId))
        .andExpect(status().isCreated());

    String invalidRequest = """
            {
                "walletId": "%s",
                "operationType": "DEPOSIT",
                "amount": -100.00
            }
            """.formatted(walletId);

    mockMvc.perform(post("/api/v1/wallet")
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidRequest))
        .andDo(print())
        .andExpect(status().isBadRequest());
  }
}