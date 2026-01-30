package ru.kuzmich.walletservice.service.impl;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import ru.kuzmich.walletservice.dto.WalletOperationRequest;
import ru.kuzmich.walletservice.dto.WalletResponse;
import ru.kuzmich.walletservice.exception.InsufficientFundsException;
import ru.kuzmich.walletservice.exception.WalletNotFoundException;
import ru.kuzmich.walletservice.model.OperationType;
import ru.kuzmich.walletservice.model.Wallet;
import ru.kuzmich.walletservice.repository.WalletRepository;
import ru.kuzmich.walletservice.service.WalletService;

@Slf4j
@Setter
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

  private final WalletRepository walletRepository;

  @Override
  @Transactional(isolation = Isolation.READ_COMMITTED)
  @Retryable(
      retryFor = OptimisticLockingFailureException.class,
      maxAttempts = 3,
      backoff = @Backoff(delay = 100)
  )
  public WalletResponse processOperation(WalletOperationRequest walletOperationRequest) {
    log.debug("Processing operation request for wallet: {}", walletOperationRequest.getWalletId());

    Wallet wallet = walletRepository.findByIdForUpdate(walletOperationRequest.getWalletId())
        .orElseThrow(() -> new WalletNotFoundException(
            "Wallet not found: " + walletOperationRequest.getWalletId()));

    if (walletOperationRequest.getOperationType() == OperationType.DEPOSIT) {
      wallet.deposit(walletOperationRequest.getAmount());
      log.info("Deposited {} to wallet {}", walletOperationRequest.getAmount(), walletOperationRequest.getWalletId());
    } else {
      if (wallet.getBalance().compareTo(walletOperationRequest.getAmount()) < 0) {
        throw new InsufficientFundsException(
            "Insufficient funds in wallet: " + walletOperationRequest.getWalletId());
      }
      wallet.withdraw(walletOperationRequest.getAmount());
      log.info("Withdrawn {} from wallet {}", walletOperationRequest.getAmount(), walletOperationRequest.getWalletId());
    }

    Wallet savedWallet = walletRepository.save(wallet);
    log.debug("Wallet {} updated. New balance: {}",
        walletOperationRequest.getWalletId(), savedWallet.getBalance());

    return mapToResponse(savedWallet);
  }

  @Override
  @Transactional(readOnly = true)
  public WalletResponse getWalletBalance(UUID walletId) {
    log.debug("Getting balance for wallet: {}", walletId);

    Wallet wallet = walletRepository.findById(walletId)
        .orElseThrow(() -> new WalletNotFoundException(
            "Wallet not found: " + walletId));

    return mapToResponse(wallet);
  }

  @Override
  @Transactional
  public WalletResponse createWallet(UUID walletId) {
    log.debug("Creating wallet: {}", walletId);

    if (walletRepository.existsById(walletId)) {
      throw new IllegalArgumentException("Wallet already exists: " + walletId);
    }

    Wallet wallet = Wallet.builder()
        .id(walletId)
        .balance(BigDecimal.ZERO)
        .version(0L)
        .build();

    Wallet savedWallet = walletRepository.save(wallet);
    log.info("Created wallet: {}", walletId);

    return mapToResponse(savedWallet);
  }

  private WalletResponse mapToResponse(Wallet wallet) {
    return WalletResponse.builder()
        .walletId(wallet.getId())
        .balance(wallet.getBalance())
        .createdAt(wallet.getCreatedAt())
        .updatedAt(wallet.getUpdatedAt())
        .build();
  }
}
