package com.smartmobility.billingservice.service;

import com.smartmobility.billingservice.client.UserServiceClient;
import com.smartmobility.billingservice.dto.*;
import com.smartmobility.billingservice.entity.Transaction;
import com.smartmobility.billingservice.entity.TransactionStatus;
import com.smartmobility.billingservice.entity.TransactionType;
import com.smartmobility.billingservice.exception.TransactionNotFoundException;
import com.smartmobility.billingservice.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    // Le billing-service se présente comme ADMIN pour tous ses appels vers user-service
    private static final String INTERNAL_ROLE = "ADMIN";

    private final TransactionRepository transactionRepository;
    private final UserServiceClient userServiceClient;

    public BillingService(TransactionRepository transactionRepository,
                          UserServiceClient userServiceClient) {
        this.transactionRepository = transactionRepository;
        this.userServiceClient = userServiceClient;
    }

    // ── Débiter après un trajet ───────────────────────────────────────────────

    public TransactionResponse debiter(DebitRequest request) {

        Transaction transaction = new Transaction();
        transaction.setUserId(request.getUserId());
        transaction.setTripId(request.getTripId());
        transaction.setMontant(request.getMontant());
        transaction.setType(TransactionType.DEBIT);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setDescription(request.getDescription() != null
                ? request.getDescription()
                : "Paiement trajet #" + request.getTripId());

        try {
            // Récupère d'abord le pass pour avoir le passId
            PassResponse passInfo = userServiceClient.getPass(
                    request.getUserId(),
                    request.getUserId().toString(),
                    INTERNAL_ROLE
            );

            UUID passId = passInfo.getId();
            transaction.setPassId(passId);

            // Débite le solde
            PassResponse passResponse = userServiceClient.debiterSolde(
                    request.getUserId(),
                    new UpdateSoldeRequest(request.getMontant()),
                    request.getUserId().toString(),
                    INTERNAL_ROLE
            );

            transaction.setSoldeApres(passResponse.getSolde());
            transaction.setStatus(TransactionStatus.SUCCESS);
            Transaction saved = transactionRepository.save(transaction);

            TransactionResponse response = mapToResponse(saved);
            response.setSoldeApresOperation(passResponse.getSolde());
            return response;

        } catch (Exception e) {
            log.error("Débit échoué pour userId={} : {}", request.getUserId(), e.getMessage());
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setDescription("ÉCHEC - " + e.getMessage());
            transaction.setSoldeApres(BigDecimal.ZERO);
            transactionRepository.save(transaction);
            throw new IllegalArgumentException("Débit impossible : " + e.getMessage());
        }
    }

    // ── Recharger le solde ────────────────────────────────────────────────────

    public TransactionResponse recharger(RechargeRequest request) {

        Transaction transaction = new Transaction();
        transaction.setUserId(request.getUserId());
        transaction.setMontant(request.getMontant());
        transaction.setType(TransactionType.CREDIT);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setDescription("Recharge du solde : +" + request.getMontant() + " FCFA");

        try {
            // Récupère le pass pour avoir le passId
            PassResponse passInfo = userServiceClient.getPass(
                    request.getUserId(),
                    request.getUserId().toString(),
                    INTERNAL_ROLE
            );

            UUID passId = passInfo.getId();
            transaction.setPassId(passId);

            // Recharge le solde
            PassResponse passResponse = userServiceClient.rechargerSolde(
                    request.getUserId(),
                    new UpdateSoldeRequest(request.getMontant()),
                    request.getUserId().toString(),
                    INTERNAL_ROLE
            );

            transaction.setSoldeApres(passResponse.getSolde());
            transaction.setStatus(TransactionStatus.SUCCESS);
            Transaction saved = transactionRepository.save(transaction);

            TransactionResponse response = mapToResponse(saved);
            response.setSoldeApresOperation(passResponse.getSolde());
            return response;

        } catch (Exception e) {
            log.error("Recharge échouée pour userId={} : {}", request.getUserId(), e.getMessage());
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setDescription("ÉCHEC RECHARGE - " + e.getMessage());
            transaction.setSoldeApres(BigDecimal.ZERO);
            transactionRepository.save(transaction);
            throw new IllegalArgumentException("Recharge impossible : " + e.getMessage());
        }
    }

    // ── Total des débits du jour pour un pass (appelé par pricing-service) ────

    @Transactional(readOnly = true)
    public BigDecimal getDailyTotal(UUID passId) {
        LocalDateTime debutJour = LocalDate.now().atStartOfDay();
        BigDecimal total = transactionRepository.sumDebitsJourByPassId(passId, debutJour);
        return total != null ? total : BigDecimal.ZERO;
    }

    // ── Historique complet d'un utilisateur ───────────────────────────────────

    @Transactional(readOnly = true)
    public List<TransactionResponse> obtenirHistorique(UUID userId) {
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ── Détail d'une transaction ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TransactionResponse obtenirTransaction(UUID transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));
        return mapToResponse(transaction);
    }

    // ── Stats du jour (ADMIN) ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> obtenirStatsJour() {
        List<Transaction> toutes = transactionRepository.findAllByOrderByCreatedAtDesc();

        LocalDateTime debutJour = LocalDate.now().atStartOfDay();
        List<Transaction> duJour = toutes.stream()
                .filter(t -> t.getCreatedAt().isAfter(debutJour))
                .collect(Collectors.toList());

        BigDecimal totalDebits = duJour.stream()
                .filter(t -> t.getType() == TransactionType.DEBIT
                        && t.getStatus() == TransactionStatus.SUCCESS)
                .map(Transaction::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredits = duJour.stream()
                .filter(t -> t.getType() == TransactionType.CREDIT
                        && t.getStatus() == TransactionStatus.SUCCESS)
                .map(Transaction::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Map.of(
                "date",           LocalDate.now().toString(),
                "totalDebits",    totalDebits,
                "totalCredits",   totalCredits,
                "nbTransactions", duJour.size(),
                "nbEchecs",       duJour.stream()
                        .filter(t -> t.getStatus() == TransactionStatus.FAILED)
                        .count()
        );
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private TransactionResponse mapToResponse(Transaction t) {
        TransactionResponse response = new TransactionResponse();
        response.setId(t.getId());
        response.setUserId(t.getUserId());
        response.setTripId(t.getTripId());
        response.setMontant(t.getMontant());
        response.setType(t.getType());
        response.setStatus(t.getStatus());
        response.setDescription(t.getDescription());
        response.setCreatedAt(t.getCreatedAt());
        return response;
    }
}