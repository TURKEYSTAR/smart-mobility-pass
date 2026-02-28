package com.smartmobility.billingservice.service;

import com.smartmobility.billingservice.client.UserServiceClient;
import com.smartmobility.billingservice.dto.*;
import com.smartmobility.billingservice.entity.Transaction;
import com.smartmobility.billingservice.entity.TransactionStatus;
import com.smartmobility.billingservice.entity.TransactionType;
import com.smartmobility.billingservice.exception.TransactionNotFoundException;
import com.smartmobility.billingservice.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class BillingService {

    private final TransactionRepository transactionRepository;
    private final UserServiceClient userServiceClient;

    public BillingService(TransactionRepository transactionRepository,
                          UserServiceClient userServiceClient) {
        this.transactionRepository = transactionRepository;
        this.userServiceClient = userServiceClient;
    }

    // ───────────────────────────────────────────────
    // Débiter le compte d'un utilisateur après un trajet
    // Appelé par trip-service
    // ───────────────────────────────────────────────
    public TransactionResponse debiter(DebitRequest request) {

        Transaction transaction = new Transaction();
        transaction.setUserId(request.getUserId());
        transaction.setTripId(request.getTripId());
        transaction.setMontant(request.getMontant());
        transaction.setType(TransactionType.DEBIT);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setDescription(
                request.getDescription() != null
                        ? request.getDescription()
                        : "Paiement trajet #" + request.getTripId());

        try {
            // Appel Feign → user-service débite le solde
            UpdateSoldeRequest soldeRequest = new UpdateSoldeRequest(request.getMontant());
            ApiResponse<PassResponse> passResponse =
                    userServiceClient.debiterSolde(request.getUserId(), soldeRequest);

            transaction.setStatus(TransactionStatus.SUCCESS);
            Transaction saved = transactionRepository.save(transaction);

            TransactionResponse response = mapToResponse(saved);
            // On retourne le solde restant après opération
            response.setSoldeApresOperation(passResponse.getData().getSolde());
            return response;

        } catch (Exception e) {
            // Si user-service échoue (solde insuffisant, pass suspendu/expiré)
            // on enregistre quand même la transaction en FAILED pour traçabilité
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setDescription("ÉCHEC - " + e.getMessage());
            transactionRepository.save(transaction);
            throw new IllegalArgumentException("Débit impossible : " + e.getMessage());
        }
    }

    // ───────────────────────────────────────────────
    // Recharger le solde d'un utilisateur
    // ───────────────────────────────────────────────
    public TransactionResponse recharger(RechargeRequest request) {

        Transaction transaction = new Transaction();
        transaction.setUserId(request.getUserId());
        transaction.setMontant(request.getMontant());
        transaction.setType(TransactionType.CREDIT);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setDescription("Recharge du solde : +" + request.getMontant() + " FCFA");

        // Appel Feign → user-service crédite le solde
        UpdateSoldeRequest soldeRequest = new UpdateSoldeRequest(request.getMontant());
        ApiResponse<PassResponse> passResponse =
                userServiceClient.rechargerSolde(request.getUserId(), soldeRequest);

        transaction.setStatus(TransactionStatus.SUCCESS);
        Transaction saved = transactionRepository.save(transaction);

        TransactionResponse response = mapToResponse(saved);
        response.setSoldeApresOperation(passResponse.getData().getSolde());
        return response;
    }

    // ───────────────────────────────────────────────
    // Historique complet des transactions d'un utilisateur
    // ───────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<TransactionResponse> obtenirHistorique(Long userId) {
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ───────────────────────────────────────────────
    // Historique uniquement des débits (trajets payés)
    // ───────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<TransactionResponse> obtenirHistoriqueDebits(Long userId) {
        return transactionRepository
                .findByUserIdAndTypeOrderByCreatedAtDesc(userId, TransactionType.DEBIT)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ───────────────────────────────────────────────
    // Détail d'une transaction par ID
    // ───────────────────────────────────────────────
    @Transactional(readOnly = true)
    public TransactionResponse obtenirTransaction(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));
        return mapToResponse(transaction);
    }

    // ───────────────────────────────────────────────
    // Mapper
    // ───────────────────────────────────────────────
    private TransactionResponse mapToResponse(Transaction transaction) {
        TransactionResponse response = new TransactionResponse();
        response.setId(transaction.getId());
        response.setUserId(transaction.getUserId());
        response.setTripId(transaction.getTripId());
        response.setMontant(transaction.getMontant());
        response.setType(transaction.getType());
        response.setStatus(transaction.getStatus());
        response.setDescription(transaction.getDescription());
        response.setCreatedAt(transaction.getCreatedAt());
        return response;
    }
}