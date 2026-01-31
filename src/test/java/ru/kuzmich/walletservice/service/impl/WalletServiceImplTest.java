package ru.kuzmich.walletservice.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import ru.kuzmich.walletservice.dto.WalletOperationRequest;
import ru.kuzmich.walletservice.dto.WalletResponse;
import ru.kuzmich.walletservice.exception.InsufficientFundsException;
import ru.kuzmich.walletservice.exception.WalletAlreadyExistsException;
import ru.kuzmich.walletservice.model.OperationType;
import ru.kuzmich.walletservice.model.Wallet;
import ru.kuzmich.walletservice.repository.WalletRepository;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

  @Mock
  private WalletRepository walletRepository;

  @InjectMocks
  private WalletServiceImpl walletService;

  private final UUID testWalletId = UUID.randomUUID();

  @Test
  void testOptimisticLockingFailureIsThrown() {
    WalletOperationRequest request = new WalletOperationRequest();
    request.setWalletId(testWalletId);
    request.setOperationType(OperationType.DEPOSIT);
    request.setAmount(new BigDecimal("1000.00"));

    when(walletRepository.findWithLockingById(testWalletId)).thenThrow(
        new OptimisticLockingFailureException("Version conflict"));

    OptimisticLockingFailureException exception = assertThrows(
        OptimisticLockingFailureException.class, () -> walletService.processOperation(request));

    assertEquals("Version conflict", exception.getMessage());
    verify(walletRepository, times(1)).findWithLockingById(testWalletId);
    verify(walletRepository, never()).save(any(Wallet.class));
  }

  @Test
  void testProcessDepositSuccess() {
    WalletOperationRequest request = new WalletOperationRequest();
    request.setWalletId(testWalletId);
    request.setOperationType(OperationType.DEPOSIT);
    request.setAmount(new BigDecimal("1000.00"));

    Wallet wallet = Wallet.builder().id(testWalletId).balance(BigDecimal.ZERO).version(0L).build();

    Wallet savedWallet = Wallet.builder().id(testWalletId).balance(new BigDecimal("1000.00"))
        .version(1L).build();

    when(walletRepository.findWithLockingById(testWalletId)).thenReturn(Optional.of(wallet));

    when(walletRepository.save(any(Wallet.class))).thenReturn(savedWallet);

    WalletResponse response = walletService.processOperation(request);

    assertNotNull(response);
    assertEquals(testWalletId, response.getWalletId());
    assertEquals(new BigDecimal("1000.00"), response.getBalance());

    verify(walletRepository, times(1)).findWithLockingById(testWalletId);
    verify(walletRepository, times(1)).save(any(Wallet.class));
  }

  @Test
  void testProcessWithdrawSuccess() {
    WalletOperationRequest request = new WalletOperationRequest();
    request.setWalletId(testWalletId);
    request.setOperationType(OperationType.WITHDRAW);
    request.setAmount(new BigDecimal("500.00"));

    Wallet wallet = Wallet.builder().id(testWalletId)
        .balance(new BigDecimal("1000.00"))
        .version(0L).build();

    Wallet savedWallet = Wallet.builder().id(testWalletId)
        .balance(new BigDecimal("500.00"))
        .version(1L).build();

    when(walletRepository.findWithLockingById(testWalletId)).thenReturn(Optional.of(wallet));

    when(walletRepository.save(any(Wallet.class))).thenReturn(savedWallet);

    WalletResponse response = walletService.processOperation(request);

    assertNotNull(response);
    assertEquals(testWalletId, response.getWalletId());
    assertEquals(new BigDecimal("500.00"), response.getBalance());

    verify(walletRepository, times(1)).findWithLockingById(testWalletId);
    verify(walletRepository, times(1)).save(any(Wallet.class));
  }

  @Test
  void testProcessWithdrawInsufficientFunds() {
    WalletOperationRequest request = new WalletOperationRequest();
    request.setWalletId(testWalletId);
    request.setOperationType(OperationType.WITHDRAW);
    request.setAmount(new BigDecimal("1500.00"));

    Wallet wallet = Wallet.builder().id(testWalletId)
        .balance(new BigDecimal("1000.00"))
        .version(0L).build();

    when(walletRepository.findWithLockingById(testWalletId)).thenReturn(Optional.of(wallet));

    InsufficientFundsException exception = assertThrows(InsufficientFundsException.class,
        () -> walletService.processOperation(request));

    assertTrue(exception.getMessage().contains("Insufficient funds"));
    assertTrue(exception.getMessage().contains(testWalletId.toString()));

    verify(walletRepository, times(1)).findWithLockingById(testWalletId);
    verify(walletRepository, never()).save(any(Wallet.class));
  }

  @Test
  void testWalletNotFound() {
    WalletOperationRequest request = new WalletOperationRequest();
    request.setWalletId(testWalletId);
    request.setOperationType(OperationType.DEPOSIT);
    request.setAmount(new BigDecimal("1000.00"));

    when(walletRepository.findWithLockingById(testWalletId)).thenReturn(Optional.empty());

    assertThrows(ru.kuzmich.walletservice.exception.WalletNotFoundException.class,
        () -> walletService.processOperation(request));

    verify(walletRepository, times(1)).findWithLockingById(testWalletId);
    verify(walletRepository, never()).save(any(Wallet.class));
  }

  @Test
  void testGetWalletBalanceSuccess() {
    Wallet wallet = Wallet.builder().id(testWalletId).balance(new BigDecimal("500.00")).version(0L)
        .build();

    when(walletRepository.findById(testWalletId)).thenReturn(Optional.of(wallet));

    WalletResponse response = walletService.getWalletBalance(testWalletId);

    assertNotNull(response);
    assertEquals(testWalletId, response.getWalletId());
    assertEquals(new BigDecimal("500.00"), response.getBalance());

    verify(walletRepository, times(1)).findById(testWalletId);
  }

  @Test
  void testGetWalletBalanceNotFound() {
    when(walletRepository.findById(testWalletId)).thenReturn(Optional.empty());

    assertThrows(ru.kuzmich.walletservice.exception.WalletNotFoundException.class,
        () -> walletService.getWalletBalance(testWalletId));

    verify(walletRepository, times(1)).findById(testWalletId);
  }

  @Test
  void testCreateWalletSuccess() {
    when(walletRepository.existsById(testWalletId)).thenReturn(false);

    Wallet savedWallet = Wallet.builder().id(testWalletId).balance(BigDecimal.ZERO).version(0L)
        .build();

    when(walletRepository.save(any(Wallet.class))).thenReturn(savedWallet);

    WalletResponse response = walletService.createWallet(testWalletId);

    assertNotNull(response);
    assertEquals(testWalletId, response.getWalletId());
    assertEquals(BigDecimal.ZERO, response.getBalance());

    verify(walletRepository, times(1)).existsById(testWalletId);
    verify(walletRepository, times(1)).save(any(Wallet.class));
  }

  @Test
  void testCreateWalletAlreadyExists() {
    when(walletRepository.existsById(testWalletId)).thenReturn(true);

    WalletAlreadyExistsException exception = assertThrows(WalletAlreadyExistsException.class,
        () -> walletService.createWallet(testWalletId));

    assertTrue(exception.getMessage().contains("Wallet already exists"));

    verify(walletRepository, times(1)).existsById(testWalletId);
    verify(walletRepository, never()).save(any(Wallet.class));
  }
}