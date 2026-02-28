package com.smartmobility.userservice.service;

import com.smartmobility.userservice.dto.PassResponse;
import com.smartmobility.userservice.dto.UpdateSoldeRequest;
import com.smartmobility.userservice.entity.MobilityPass;
import com.smartmobility.userservice.entity.PassStatus;
import com.smartmobility.userservice.entity.User;
import com.smartmobility.userservice.exception.PassSuspenduException;
import com.smartmobility.userservice.exception.SoldeInsuffisantException;
import com.smartmobility.userservice.exception.UserNotFoundException;
import com.smartmobility.userservice.repository.MobilityPassRepository;
import com.smartmobility.userservice.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class PassMobilityService {

    private static final int DUREE_VALIDITE_MOIS = 12;

    private final MobilityPassRepository passRepository;
    private final UserRepository userRepository;

    public PassMobilityService(MobilityPassRepository passRepository, UserRepository userRepository) {
        this.passRepository = passRepository;
        this.userRepository = userRepository;
    }

    public void creerPassAutomatique(User user) {
        // 1. Créer le nouveau Pass
        MobilityPass pass = new MobilityPass();
        pass.setSolde(0.0);
        pass.setStatus(PassStatus.ACTIVE);
        pass.setCreatedAt(LocalDateTime.now());
        // Optionnel : ajouter une date d'expiration (ex: dans 1 an)
        pass.setExpirationDate(LocalDateTime.now().plusYears(1));

        // 2. Sauvegarder le Pass
        MobilityPass savedPass = passRepository.save(pass);

        // 3. Lier le Pass à l'utilisateur
        user.setMobilityPass(savedPass);
        userRepository.save(user);
    }

    // ── Consulter le pass ─────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public PassResponse obtenirPass(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        verifierEtMettreAJourExpiration(user.getMobilityPass());
        return mapToPassResponse(user.getMobilityPass(), userId);
    }

    // ── Suspendre le pass ─────────────────────────────────────────────────────
    public PassResponse suspendrePass(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        MobilityPass pass = user.getMobilityPass();
        verifierEtMettreAJourExpiration(pass);

        if (pass.getStatus() == PassStatus.EXPIRE) {
            throw new IllegalArgumentException(
                    "Impossible de suspendre un pass expiré. Veuillez le renouveler.");
        }

        pass.setStatus(PassStatus.SUSPENDU);
        userRepository.save(user);
        return mapToPassResponse(pass, userId);
    }

    // ── Réactiver le pass ─────────────────────────────────────────────────────
    public PassResponse activerPass(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        MobilityPass pass = user.getMobilityPass();
        verifierEtMettreAJourExpiration(pass);

        if (pass.getStatus() == PassStatus.EXPIRE) {
            throw new IllegalArgumentException(
                    "Impossible de réactiver un pass expiré. Veuillez le renouveler.");
        }

        pass.setStatus(PassStatus.ACTIVE);
        userRepository.save(user);
        return mapToPassResponse(pass, userId);
    }

    // ── Renouveler le pass ────────────────────────────────────────────────────
    public PassResponse renouvellerPass(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        MobilityPass pass = user.getMobilityPass();
        pass.setExpirationDate(LocalDateTime.now().plusMonths(DUREE_VALIDITE_MOIS));
        pass.setStatus(PassStatus.ACTIVE);
        userRepository.save(user);
        return mapToPassResponse(pass, userId);
    }

    // ── Débiter le solde ──────────────────────────────────────────────────────
    public PassResponse debiterSolde(Long userId, UpdateSoldeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        MobilityPass pass = user.getMobilityPass();
        verifierEtMettreAJourExpiration(pass);

        if (pass.getStatus() == PassStatus.SUSPENDU) {
            throw new PassSuspenduException(pass.getPassNumber());
        }
        if (pass.getStatus() == PassStatus.EXPIRE) {
            throw new IllegalArgumentException(
                    "Le Mobility Pass " + pass.getPassNumber() + " est expiré.");
        }

        Double soldeActuel = pass.getSolde();
        Double montant     = request.getMontant();

        if (soldeActuel.compareTo(montant) < 0) {
            throw new SoldeInsuffisantException(soldeActuel, montant);
        }

        pass.setSolde(soldeActuel - montant);
        userRepository.save(user);
        return mapToPassResponse(pass, userId);
    }

    // ── Recharger le solde ────────────────────────────────────────────────────
    public PassResponse rechargerSolde(Long userId, UpdateSoldeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        MobilityPass pass = user.getMobilityPass();
        pass.setSolde(pass.getSolde() + request.getMontant());
        userRepository.save(user);
        return mapToPassResponse(pass, userId);
    }

    // ── Utilitaires internes ──────────────────────────────────────────────────

    private void verifierEtMettreAJourExpiration(MobilityPass pass) {
        if (pass.getStatus() != PassStatus.EXPIRE
                && pass.getExpirationDate() != null
                && LocalDateTime.now().isAfter(pass.getExpirationDate())) {
            pass.setStatus(PassStatus.EXPIRE);
        }
    }

    private PassResponse mapToPassResponse(MobilityPass pass, Long userId) {
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
}