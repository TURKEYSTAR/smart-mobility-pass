package com.smartmobility.userservice.service;

import com.smartmobility.userservice.dto.*;
import com.smartmobility.userservice.entity.MobilityPass;
import com.smartmobility.userservice.entity.PassStatus;
import com.smartmobility.userservice.entity.Role;
import com.smartmobility.userservice.entity.User;
import com.smartmobility.userservice.exception.*;
import com.smartmobility.userservice.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService {

    // Durée de validité d'un pass : 1 an (configurable via Config Server)
    private static final int DUREE_VALIDITE_MOIS = 12;

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // ───────────────────────────────────────────────
    // Créer un utilisateur + générer son MobilityPass
    // ───────────────────────────────────────────────
    public UserResponse creerUtilisateur(CreateUserRequest request) {
        // Vérification username (l'email est déjà vérifié par auth-service)
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException(
                    "Ce nom d'utilisateur est déjà pris : " + request.username());
        }

        LocalDateTime maintenant = LocalDateTime.now();

        // Génération du MobilityPass — inchangé ✅
        MobilityPass pass = new MobilityPass();
        pass.setPassNumber(genererPassNumber());
        pass.setStatus(PassStatus.ACTIVE);
        pass.setSolde(0.0);
        pass.setCreatedAt(maintenant);
        pass.setExpirationDate(maintenant.plusMonths(DUREE_VALIDITE_MOIS));

        // Création du User
        User user = new User();
        user.setNom(request.nom());
        user.setPrenom(request.prenom());
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(request.password());       // déjà encodé par auth-service ✅
        user.setTelephone(request.telephone());
        user.setCreatedAt(maintenant);
        user.setEnabled(request.enabled());
        user.setGoogleId(request.googleId());       // null si inscription classique

        // ✅ AJOUT : on persiste le rôle (vient de auth-service, "USER" par défaut)
        user.setRole(Role.valueOf(request.role()));

        user.setMobilityPass(pass);

        User savedUser = userRepository.save(user);
        return mapToUserResponse(savedUser);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ✅ AJOUT — obtenirTousLesUtilisateurs (pour GET /api/users)
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<UserResponse> obtenirTousLesUtilisateurs() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ✅ AJOUT — mettreAJourUtilisateur (pour PUT /api/users/{id})
    // ─────────────────────────────────────────────────────────────────────────
    public UserResponse mettreAJourUtilisateur(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        // On met à jour uniquement les champs non null envoyés
        if (request.getNom() != null)       user.setNom(request.getNom());
        if (request.getPrenom() != null)    user.setPrenom(request.getPrenom());
        if (request.getTelephone() != null) user.setTelephone(request.getTelephone());
        if (request.getUsername() != null) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new IllegalArgumentException(
                        "Ce nom d'utilisateur est déjà pris : " + request.getUsername());
            }
            user.setUsername(request.getUsername());
        }

        User savedUser = userRepository.save(user);
        return mapToUserResponse(savedUser);
    }

    // ───────────────────────────────────────────────
    // Lire un utilisateur par ID
    // ───────────────────────────────────────────────
    @Transactional(readOnly = true)
    public UserResponse obtenirUtilisateur(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        return mapToUserResponse(user);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ✅ AJOUT — supprimerUtilisateur (pour DELETE /api/users/{id})
    // ─────────────────────────────────────────────────────────────────────────
    public void supprimerUtilisateur(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        userRepository.deleteById(id);
    }

    // ───────────────────────────────────────────────
    // Mappers
    // ───────────────────────────────────────────────
    private UserResponse mapToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setNom(user.getNom());
        response.setPrenom(user.getPrenom());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setTelephone(user.getTelephone());
        response.setCreatedAt(user.getCreatedAt());

        if (user.getMobilityPass() != null) {
            response.setPassNumber(user.getMobilityPass().getPassNumber());
            response.setPassStatus(user.getMobilityPass().getStatus());
            response.setSolde(user.getMobilityPass().getSolde());
        }
        return response;
    }

    // Génère un numéro de pass unique de type : SMP-A3F9B2C1
    private String genererPassNumber() {
        String uuid = UUID.randomUUID().toString().toUpperCase().replace("-", "").substring(0, 8);
        return "SMP-" + uuid;
    }

}