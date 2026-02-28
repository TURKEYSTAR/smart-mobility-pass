package com.smartmobility.userservice.security;

import com.smartmobility.userservice.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Wrapper Spring Security autour de l'entité User.
 *
 * Pourquoi cette classe existe ?
 * ---------------------------------------------------------------
 * Spring Security a besoin d'un objet "UserDetails" pour gérer
 * l'authentification. Plutôt que de polluer l'entité JPA User
 * avec des méthodes Spring Security (couplage fort, problèmes
 * de transactions Lazy), on crée cette classe intermédiaire.
 *
 * Avantages :
 *  ✅ User.java reste une entité JPA pure
 *  ✅ Pas de risque de LazyInitializationException
 *  ✅ On contrôle exactement ce qu'on expose à Spring Security
 *  ✅ Facile à mocker dans les tests unitaires
 *
 * Fonctionnement :
 *  UserDetailsServiceImpl charge un User depuis la base,
 *  l'enveloppe dans un UserPrincipal, et retourne ce dernier
 *  à Spring Security.
 *
 * Comment récupérer le User métier depuis un Controller ?
 *  @AuthenticationPrincipal UserPrincipal principal
 *  User user = principal.getUser();
 */
public class UserPrincipal implements UserDetails , OAuth2User {

    public User getUser() {
        return user;
    }

    /**
     * L'entité métier User.
     * On expose un getter pour pouvoir récupérer le User
     * dans les controllers via @AuthenticationPrincipal.
     */

    private final User user;
    private Map<String, Object> attributes;

    public UserPrincipal(User user, Map<String, Object> attributes) {
        this.user = user;
        this.attributes = attributes;
    }
    public UserPrincipal(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );
    }


    @Override
    public String getPassword() {
        return user.getPassword();
    }

    /**
     * Identifiant utilisé pour le login.
     * On utilise l'email (plus standard que le username).
     */
    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }


    public Long getId() {
        return user.getId();
    }



    // ── OAuth2User ───────────────────────────────────────────────────────────
    @Override
    public Map<String, Object> getAttributes() {
        return attributes != null ? attributes : Map.of();
    }

    @Override
    public String getName() {
        // Requis par OAuth2User — on retourne l'email
        return user.getEmail();
    }
    public static UserPrincipal create(User user, Map<String, Object> attributes) {
        return new UserPrincipal(user, attributes);
    }
}