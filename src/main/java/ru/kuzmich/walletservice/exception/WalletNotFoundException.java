package ru.kuzmich.walletservice.exception;

public class WalletNotFoundException extends RuntimeException {

  public WalletNotFoundException(String message) {
    super(message);
  }
}
