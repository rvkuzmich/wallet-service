package ru.kuzmich.walletservice.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.kuzmich.walletservice.model.Wallet;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT w FROM Wallet w WHERE w.id = :id")
  Optional<Wallet> findWithLockingById(@Param("id") UUID id);

  Optional<Wallet> findById(UUID id);
}
