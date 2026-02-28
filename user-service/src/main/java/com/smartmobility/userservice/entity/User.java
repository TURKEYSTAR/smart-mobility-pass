package com.smartmobility.userservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


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

    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Column(nullable = false)
    private String password;


    @Column(length = 20)
    private String telephone;
    LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "google_id", unique = true)
    private String googleId;

    @Column(nullable = false)
    private boolean enabled = true;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "mobility_pass_id")
    MobilityPass mobilityPass;

    public User() {
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public MobilityPass getMobilityPass() {
        return mobilityPass;
    }

    public void setMobilityPass(MobilityPass mobilityPass) {
        this.mobilityPass = mobilityPass;
    }


    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
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
}
