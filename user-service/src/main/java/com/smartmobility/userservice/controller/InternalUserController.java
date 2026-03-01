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
 * InternalUserController — appelé uniquement par l'auth-service via Feign.
 * PAS exposé par la Gateway (/internal/** non routé).
 */
@RestController
@RequestMapping("/internal/users")
public class InternalUserController {

    private final UserRepository userRepository;
    private final PassMobilityService passMobilityService;

    public InternalUserController(UserRepository userRepository,
                                  PassMobilityService passMobilityService) {
        this.userRepository = userRepository;
        this.passMobilityService = passMobilityService;
    }

    // GET /internal/users/email/{email} — auth-service cherche si l'email existe
    @GetMapping("/email/{email}")
    public UserDto findByEmail(@PathVariable String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Utilisateur non trouvé : " + email));
        return toDto(user);
    }

    // POST /internal/users — auth-service crée un nouveau compte
    @PostMapping
    public ResponseEntity<UserDto> createUser(@RequestBody CreateUserRequest request) {

        // Vérification email unique (sécurité supplémentaire côté user-service)
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Email déjà utilisé : " + request.email());
        }

        User user = new User();
        user.setNom(request.nom());
        user.setPrenom(request.prenom());
        user.setEmail(request.email());
        user.setPassword(request.password());   // déjà encodé BCrypt par auth-service
        user.setUsername(request.username());
        user.setTelephone(request.telephone());
        user.setRole(Role.valueOf(request.role()));
        user.setEnabled(request.enabled());
        user.setGoogleId(request.googleId());
        user.setCreatedAt(LocalDateTime.now());

        User saved = userRepository.save(user);

        // Créer et lier le MobilityPass automatiquement
        passMobilityService.creerPassAutomatique(saved);

        // Recharger pour avoir l'ID du pass dans la réponse
        User withPass = userRepository.findById(saved.getId()).orElse(saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(withPass));
    }

    // ── Mapper ────────────────────────────────────────────────────────────────
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