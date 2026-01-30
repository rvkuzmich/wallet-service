package ru.kuzmich.walletservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "balance", nullable = false, precision = 15, scale = 2)
  private BigDecimal balance;

  @Version
  @Column(name = "version", nullable = false)
  private Long version;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public void deposit(BigDecimal amount) {
    this.balance = this.balance.add(amount);
  }

  public void withdraw(BigDecimal amount) {
    if (this.balance.compareTo(amount) < 0) {
      throw new IllegalArgumentException("Insufficient funds");
    }
    this.balance = this.balance.subtract(amount);
  }
}
