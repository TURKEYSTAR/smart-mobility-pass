package com.smartmobility.userservice.controller;

import com.smartmobility.userservice.dto.CreateUserRequest;
import com.smartmobility.userservice.dto.UserDto;
import com.smartmobility.userservice.entity.Role;
import com.smartmobility.userservice.entity.User;
import com.smartmobility.userservice.repository.UserRepository;
import com.smartmobility.userservice.service.PassMobilityService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

/**
 * InternalUserController — user-service
 *
 * Endpoints INTERNES appelés uniquement par l'auth-service via Feign.
 * Ces routes ne sont PAS exposées publiquement via la Gateway
 * (pas de route /internal/** dans application.properties de la Gateway).
 *
 * ⚠️  En production : ajouter une vérification que l'appelant
 *     est bien un service interne (header X-Internal-Token ou réseau privé).
 */
@RestController
@RequestMapping("/internal/users")
public class InternalUserController {

    private final UserRepository userRepository;
    private final PassMobilityService passMobilityService;

    public InternalUserController(UserRepository userRepository, PassMobilityService passMobilityService) {
        this.userRepository = userRepository;
        this.passMobilityService = passMobilityService;
    }


    // ── Chercher par email ────────────────────────────────────────────────────
    @GetMapping("/email/{email}")
    public UserDto findByEmail(@PathVariable String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Utilisateur non trouvé : " + email));
        return toDto(user);
    }

    // ── Créer un utilisateur ──────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<UserDto> createUser(@RequestBody CreateUserRequest request) {
        User user = new User();
        user.setNom(request.nom());
        user.setPrenom(request.prenom());
        user.setEmail(request.email());
        user.setPassword(request.password());   // déjà encodé par l'auth-service
        user.setUsername(request.username());
        user.setTelephone(request.telephone());
        user.setRole(Role.valueOf(request.role()));
        user.setEnabled(request.enabled());
        user.setGoogleId(request.googleId());

        user.setCreatedAt(LocalDateTime.now());

        User saved = userRepository.save(user);
        passMobilityService.creerPassAutomatique(saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    // ── Mapper User → UserDto ─────────────────────────────────────────────────
    private UserDto toDto(User user) {
        return new UserDto(
                user.getId(),
                user.getNom(),
                user.getPrenom(),
                user.getEmail(),
                user.getPassword(),
                user.getUsername(),
                user.getTelephone(),
                user.getRole().name(),
                user.isEnabled(),
                user.getGoogleId()
        );
    }
}