package com.smartmobility.userservice.service;

import com.smartmobility.userservice.dto.*;
import com.smartmobility.userservice.entity.PassStatus;
import com.smartmobility.userservice.entity.Role;
import com.smartmobility.userservice.entity.User;
import com.smartmobility.userservice.exception.UserNotFoundException;
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

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // ── Lire tous les utilisateurs — ADMIN (dashboard) ────────────────────────
    @Transactional(readOnly = true)
    public List<UserResponse> obtenirTousLesUtilisateurs() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    // ── Lire un utilisateur — USER (son profil) ou ADMIN ─────────────────────
    @Transactional(readOnly = true)
    public UserResponse obtenirUtilisateur(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        return mapToUserResponse(user);
    }

    // ── Modifier un profil — USER (son propre) ou ADMIN ──────────────────────
    public UserResponse mettreAJourUtilisateur(UUID id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

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

        return mapToUserResponse(userRepository.save(user));
    }

    // ── Supprimer un utilisateur — ADMIN uniquement ───────────────────────────
    public void supprimerUtilisateur(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        userRepository.deleteById(id);
    }

    // ── Mapper ────────────────────────────────────────────────────────────────
    private UserResponse mapToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setNom(user.getNom());
        response.setPrenom(user.getPrenom());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setTelephone(user.getTelephone());
        response.setRole(user.getRole().name());
        response.setCreatedAt(user.getCreatedAt());

        if (user.getMobilityPass() != null) {
            response.setPassNumber(user.getMobilityPass().getPassNumber());
            response.setPassStatus(user.getMobilityPass().getStatus());
            response.setSolde(user.getMobilityPass().getSolde());
        }
        return response;
    }
}