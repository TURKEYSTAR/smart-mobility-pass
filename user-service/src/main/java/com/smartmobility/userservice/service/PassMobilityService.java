package com.smartmobility.userservice.service;

import com.smartmobility.userservice.dto.PassResponse;
import com.smartmobility.userservice.dto.UpdateSoldeRequest;
import com.smartmobility.userservice.entity.MobilityPass;
import com.smartmobility.userservice.entity.PassStatus;
import com.smartmobility.userservice.entity.User;
import com.smartmobility.userservice.exception.PassSuspenduException;
import com.smartmobility.userservice.exception.SoldeInsuffisantException;
import com.smartmobility.userservice.exception.UserNotFoundException;
import com.smartmobility.userservice.messaging.PassEventPublisher;
import com.smartmobility.userservice.repository.MobilityPassRepository;
import com.smartmobility.userservice.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
@Slf4j
public class PassMobilityService {

    private static final int DUREE_VALIDITE_MOIS = 12;

    private final MobilityPassRepository passRepository;
    private final UserRepository userRepository;
    private final PassEventPublisher passEventPublisher;

    public PassMobilityService(MobilityPassRepository passRepository,
                               UserRepository userRepository,
                               PassEventPublisher passEventPublisher) {
        this.passRepository = passRepository;
        this.userRepository = userRepository;
        this.passEventPublisher = passEventPublisher;
    }

    public void creerPassAutomatique(User user) {
        MobilityPass pass = new MobilityPass();
        pass.setPassNumber(genererPassNumber());
        pass.setSolde(BigDecimal.ZERO);
        pass.setStatus(PassStatus.ACTIVE);
        pass.setCreatedAt(LocalDateTime.now());
        pass.setExpirationDate(LocalDateTime.now().plusMonths(DUREE_VALIDITE_MOIS));
        MobilityPass savedPass = passRepository.save(pass);
        user.setMobilityPass(savedPass);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public PassResponse obtenirPass(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        MobilityPass pass = user.getMobilityPass();
        verifierEtMettreAJourExpiration(pass);
        return mapToPassResponse(pass, userId);
    }

    // ── Suspendre — ADMIN ─────────────────────────────────────────────────────
    public PassResponse suspendrePass(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        MobilityPass pass = user.getMobilityPass();
        verifierEtMettreAJourExpiration(pass);

        if (pass.getStatus() == PassStatus.EXPIRE)
            throw new IllegalArgumentException("Impossible de suspendre un pass expiré.");
        if (pass.getStatus() == PassStatus.SUSPENDU)
            throw new IllegalArgumentException("Le pass est déjà suspendu.");

        pass.setStatus(PassStatus.SUSPENDU);
        passRepository.save(pass);

        try {
            passEventPublisher.publishPassSuspended(
                    userId, pass.getId(), pass.getPassNumber(), pass.getSolde());
        } catch (Exception e) {
            log.warn("[PassMobilityService] RabbitMQ indisponible, PASS_SUSPENDED non publié : {}", e.getMessage());
        }

        log.warn("[PassMobilityService] ⛔ Pass suspendu - userId={}, pass={}", userId, pass.getPassNumber());
        return mapToPassResponse(pass, userId);
    }

    // ── Activer — ADMIN ───────────────────────────────────────────────────────
    public PassResponse activerPass(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        MobilityPass pass = user.getMobilityPass();
        verifierEtMettreAJourExpiration(pass);

        if (pass.getStatus() == PassStatus.EXPIRE)
            throw new IllegalArgumentException("Impossible de réactiver un pass expiré.");
        if (pass.getStatus() == PassStatus.ACTIVE)
            throw new IllegalArgumentException("Le pass est déjà actif.");

        pass.setStatus(PassStatus.ACTIVE);
        passRepository.save(pass);

        // ✅ Notifier l'utilisateur que son pass est réactivé
        try {
            passEventPublisher.publishPassActivated(
                    userId, pass.getId(), pass.getPassNumber(), pass.getSolde());
        } catch (Exception e) {
            log.warn("[PassMobilityService] RabbitMQ indisponible, PASS_ACTIVATED non publié : {}", e.getMessage());
        }

        log.info("[PassMobilityService] ✅ Pass activé - userId={}, pass={}", userId, pass.getPassNumber());
        return mapToPassResponse(pass, userId);
    }

    // ── Renouveler ────────────────────────────────────────────────────────────
    public PassResponse renouvellerPass(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        MobilityPass pass = user.getMobilityPass();
        pass.setExpirationDate(LocalDateTime.now().plusMonths(DUREE_VALIDITE_MOIS));
        pass.setStatus(PassStatus.ACTIVE);
        passRepository.save(pass);
        return mapToPassResponse(pass, userId);
    }

    // ── Débiter ───────────────────────────────────────────────────────────────
    public PassResponse debiterSolde(UUID userId, UpdateSoldeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        MobilityPass pass = user.getMobilityPass();
        verifierEtMettreAJourExpiration(pass);

        if (pass.getStatus() == PassStatus.SUSPENDU)
            throw new PassSuspenduException(pass.getPassNumber());
        if (pass.getStatus() == PassStatus.EXPIRE)
            throw new IllegalArgumentException("Le Mobility Pass " + pass.getPassNumber() + " est expiré.");

        BigDecimal soldeActuel = pass.getSolde();
        BigDecimal montant = request.getMontant();
        if (soldeActuel.compareTo(montant) < 0)
            throw new SoldeInsuffisantException(soldeActuel, montant);

        pass.setSolde(soldeActuel.subtract(montant));
        passRepository.save(pass);
        return mapToPassResponse(pass, userId);
    }

    // ── Recharger ─────────────────────────────────────────────────────────────
    public PassResponse rechargerSolde(UUID userId, UpdateSoldeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        MobilityPass pass = user.getMobilityPass();
        verifierEtMettreAJourExpiration(pass);

        // ✅ Bloquer + notifier si pass suspendu
        if (pass.getStatus() == PassStatus.SUSPENDU) {
            try {
                passEventPublisher.publishRechargeRefused(
                        userId, pass.getId(), request.getMontant(), "PASS_SUSPENDED");
            } catch (Exception e) {
                log.warn("[PassMobilityService] RabbitMQ indisponible, RECHARGE_REFUSED non publié : {}", e.getMessage());
            }
            throw new IllegalArgumentException(
                    "Impossible de recharger un pass suspendu. Contactez un administrateur.");
        }

        // ✅ Bloquer + notifier si pass expiré
        if (pass.getStatus() == PassStatus.EXPIRE) {
            try {
                passEventPublisher.publishRechargeRefused(
                        userId, pass.getId(), request.getMontant(), "PASS_EXPIRED");
            } catch (Exception e) {
                log.warn("[PassMobilityService] RabbitMQ indisponible, RECHARGE_REFUSED non publié : {}", e.getMessage());
            }
            throw new IllegalArgumentException(
                    "Impossible de recharger un pass expiré. Veuillez d'abord le renouveler.");
        }

        pass.setSolde(pass.getSolde().add(request.getMontant()));
        passRepository.save(pass);
        log.info("[PassMobilityService] ✅ Recharge effectuée - userId={}, montant={}, nouveau solde={}",
                userId, request.getMontant(), pass.getSolde());
        return mapToPassResponse(pass, userId);
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    private void verifierEtMettreAJourExpiration(MobilityPass pass) {
        if (pass.getStatus() != PassStatus.EXPIRE
                && pass.getExpirationDate() != null
                && LocalDateTime.now().isAfter(pass.getExpirationDate())) {
            pass.setStatus(PassStatus.EXPIRE);
            passRepository.save(pass);
        }
    }

    private PassResponse mapToPassResponse(MobilityPass pass, UUID userId) {
        PassResponse response = new PassResponse();
        response.setId(pass.getId());
        response.setPassNumber(pass.getPassNumber());
        response.setStatus(pass.getStatus());
        response.setSolde(pass.getSolde());
        response.setCreatedAt(pass.getCreatedAt());
        response.setExpirationDate(pass.getExpirationDate());
        response.setUserId(userId);
        return response;
    }

    private String genererPassNumber() {
        return "SMP-" + UUID.randomUUID().toString().toUpperCase().replace("-", "").substring(0, 8);
    }
}