package com.smartmobility.billingservice.repository;

import com.smartmobility.billingservice.entity.Transaction;
import com.smartmobility.billingservice.entity.TransactionStatus;
import com.smartmobility.billingservice.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Transaction> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, TransactionType type);

    List<Transaction> findByTripId(Long tripId);

    List<Transaction> findAllByOrderByCreatedAtDesc();

    // ADMIN — Filtrer par statut (ex: toutes les FAILED pour détecter les anomalies)
    List<Transaction> findByStatusOrderByCreatedAtDesc(TransactionStatus status);
}