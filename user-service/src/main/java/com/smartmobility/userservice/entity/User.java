package com.smartmobility.userservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String prenom;

    @NotBlank(message = "Le nom d'utilisateur est obligatoire")
    @Size(min = 3, max = 50)
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Email
    @Column(nullable = false, unique = true)
    private String email;

    // Nullable : les comptes OAuth2 (Google) n'ont pas de mot de passe
    @Column(nullable = true)
    private String password;

    @Column(length = 20)
    private String telephone;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "google_id", unique = true)
    private String googleId;

    @Column(nullable = false)
    private boolean enabled = true;

    // User est le côté propriétaire de la relation
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "mobility_pass_id", referencedColumnName = "id")
    private MobilityPass mobilityPass;

    public User() {
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getGoogleId() {
        return googleId;
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public MobilityPass getMobilityPass() {
        return mobilityPass;
    }

    public void setMobilityPass(MobilityPass mobilityPass) {
        this.mobilityPass = mobilityPass;
    }
}