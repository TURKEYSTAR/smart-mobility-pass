package com.smartmobility.authservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartmobility.authservice.dto.AuthResponse;
import com.smartmobility.authservice.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handler appelé par Spring Security après un login Google réussi.
 *
 * Au lieu de rediriger vers une page HTML (on est en mode API),
 * on retourne directement le JWT en JSON.
 *
 * Flux complet OAuth2 :
 *  1. Client → GET /oauth2/authorization/google
 *  2. Redirect → Google consent screen
 *  3. Google → callback /api/auth/oauth2/callback/google
 *  4. Spring Security traite le code → appelle ce handler
 *  5. Ce handler → appelle AuthService.loginOAuth2() → retourne JWT en JSON
 */
@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;
    private final ObjectMapper objectMapper;

    public OAuth2SuccessHandler(AuthService authService, ObjectMapper objectMapper) {
        this.authService = authService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email    = oAuth2User.getAttribute("email");
        String nom      = oAuth2User.getAttribute("family_name");
        String prenom   = oAuth2User.getAttribute("given_name");
        String googleId = oAuth2User.getAttribute("sub");

        // Crée ou récupère le user, génère le JWT
        AuthResponse authResponse = authService.loginOAuth2(email, nom, prenom, googleId);

        // Retourne le JWT directement en JSON (API mode, pas de redirect HTML)
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(objectMapper.writeValueAsString(authResponse));
    }
}