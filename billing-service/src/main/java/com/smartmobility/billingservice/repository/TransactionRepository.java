package com.smartmobility.billingservice.repository;

import com.smartmobility.billingservice.entity.Transaction;
import com.smartmobility.billingservice.entity.TransactionStatus;
import com.smartmobility.billingservice.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Transaction> findByUserIdAndTypeOrderByCreatedAtDesc(UUID userId, TransactionType type);

    List<Transaction> findByTripId(UUID tripId);

    List<Transaction> findAllByOrderByCreatedAtDesc();

    List<Transaction> findByStatusOrderByCreatedAtDesc(TransactionStatus status);

    /**
     * Total des débits SUCCESS du jour pour un pass.
     * Appelé par pricing-service pour le plafond journalier.
     * ⚠️ Hibernate 7 : enums en HQL doivent être qualifiés avec le FQCN.
     */
    @Query("SELECT COALESCE(SUM(t.montant), 0) FROM Transaction t " +
            "WHERE t.passId = :passId " +
            "AND t.type = com.smartmobility.billingservice.entity.TransactionType.DEBIT " +
            "AND t.status = com.smartmobility.billingservice.entity.TransactionStatus.SUCCESS " +
            "AND t.createdAt >= :debutJour")
    BigDecimal sumDebitsJourByPassId(@Param("passId") UUID passId,
                                     @Param("debutJour") LocalDateTime debutJour);
}