package ru.kuzmich.walletservice.service;

import java.util.UUID;
import ru.kuzmich.walletservice.dto.WalletOperationRequest;
import ru.kuzmich.walletservice.dto.WalletResponse;

public interface WalletService {

  WalletResponse processOperation(WalletOperationRequest request);

  WalletResponse getWalletBalance(UUID walletId);

  WalletResponse createWallet(UUID walletId);
}
