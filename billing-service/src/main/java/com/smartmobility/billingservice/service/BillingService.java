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

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    // Le billing-service se comporte comme un MANAGER pour tous ses appels internes
    private static final String INTERNAL_ROLE = "MANAGER";

    private final TransactionRepository transactionRepository;
    private final UserServiceClient userServiceClient;

    public BillingService(TransactionRepository transactionRepository,
                          UserServiceClient userServiceClient) {
        this.transactionRepository = transactionRepository;
        this.userServiceClient = userServiceClient;
    }

    // ── Débiter le compte après un trajet ─────────────────────────────────────
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
            // ✅ Appel Feign — billing envoie MANAGER comme rôle
            // debiterSolde attend : userId, request, X-User-Role
            ApiResponse<PassResponse> passResponse = userServiceClient.debiterSolde(
                    request.getUserId(),
                    new UpdateSoldeRequest(request.getMontant()),
                    INTERNAL_ROLE   // ← "MANAGER" — pas besoin de le recevoir du client
            );

            transaction.setStatus(TransactionStatus.SUCCESS);
            Transaction saved = transactionRepository.save(transaction);

            TransactionResponse response = mapToResponse(saved);
            response.setSoldeApresOperation(passResponse.getData().getSolde());
            return response;

        } catch (Exception e) {
            // Enregistre la transaction FAILED pour traçabilité
            log.error("Débit échoué pour userId={} : {}", request.getUserId(), e.getMessage());
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setDescription("ÉCHEC - " + e.getMessage());
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

        // ✅ Appel Feign — rechargerSolde attend :
        //    userId, request, X-User-Id (userId cible en String), X-User-Role
        ApiResponse<PassResponse> passResponse = userServiceClient.rechargerSolde(
                request.getUserId(),
                new UpdateSoldeRequest(request.getMontant()),
                String.valueOf(request.getUserId()),  // ← X-User-Id = userId cible
                INTERNAL_ROLE                         // ← X-User-Role = "MANAGER"
        );

        transaction.setStatus(TransactionStatus.SUCCESS);
        Transaction saved = transactionRepository.save(transaction);

        TransactionResponse response = mapToResponse(saved);
        response.setSoldeApresOperation(passResponse.getData().getSolde());
        return response;
    }

    // ── Historique complet ────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<TransactionResponse> obtenirHistorique(Long userId) {
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ── Historique débits uniquement ──────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<TransactionResponse> obtenirHistoriqueDebits(Long userId) {
        return transactionRepository
                .findByUserIdAndTypeOrderByCreatedAtDesc(userId, TransactionType.DEBIT)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ── Détail d'une transaction ──────────────────────────────────────────────
    @Transactional(readOnly = true)
    public TransactionResponse obtenirTransaction(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));
        return mapToResponse(transaction);
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