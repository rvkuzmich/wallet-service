package ru.kuzmich.walletservice.controller;

import static ru.kuzmich.walletservice.util.ApplicationConstants.API_VERSION;
import static ru.kuzmich.walletservice.util.ApplicationConstants.HEALTH_PATH;
import static ru.kuzmich.walletservice.util.ApplicationConstants.WALLETS_ID_PATH;
import static ru.kuzmich.walletservice.util.ApplicationConstants.WALLETS_PATH;
import static ru.kuzmich.walletservice.util.ApplicationConstants.WALLET_ID;
import static ru.kuzmich.walletservice.util.ApplicationConstants.WALLET_PATH;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.kuzmich.walletservice.dto.WalletOperationRequest;
import ru.kuzmich.walletservice.dto.WalletResponse;
import ru.kuzmich.walletservice.service.WalletService;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping(API_VERSION)
public class WalletController {

  private final WalletService walletService;

  @PostMapping(WALLET_PATH)
  public ResponseEntity<WalletResponse> processWalletOperation(@Valid @RequestBody
      WalletOperationRequest walletOperationRequest) {
    log.info("Received wallet operation request: {}", walletOperationRequest);

    WalletResponse walletResponse = walletService.processOperation(walletOperationRequest);
    return ResponseEntity.ok(walletResponse);
  }

  @GetMapping(WALLETS_ID_PATH)
  public ResponseEntity<WalletResponse> getWalletBalance(@PathVariable(WALLET_ID) UUID walletId) {
    log.info("Received wallet balance request: {}", walletId);

    WalletResponse walletResponse = walletService.getWalletBalance(walletId);
    return ResponseEntity.ok(walletResponse);
  }

  @PostMapping(WALLETS_ID_PATH + "/create")
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseEntity<WalletResponse> createWallet(@PathVariable(WALLET_ID) UUID walletId) {
    log.info("Creating wallet: {}", walletId);

    WalletResponse walletResponse = walletService.createWallet(walletId);
    return ResponseEntity.status(HttpStatus.CREATED).body(walletResponse);
  }

  @GetMapping(HEALTH_PATH)
  public ResponseEntity<String> health() {
    return ResponseEntity.ok("OK");
  }
}
