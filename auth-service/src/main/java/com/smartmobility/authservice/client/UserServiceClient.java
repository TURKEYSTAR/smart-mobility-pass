package com.smartmobility.authservice.client;

import com.smartmobility.authservice.dto.CreateUserRequest;
import com.smartmobility.authservice.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    /**
     * Cherche un utilisateur par email.
     * Retourne null (404) si non trouvé — géré dans AuthService.
     */
    @GetMapping("/internal/users/email/{email}")
    UserDto findByEmail(@PathVariable("email") String email);

    /**
     * Crée un nouvel utilisateur (register ou première connexion OAuth2).
     */
    @PostMapping("/internal/users")
    UserDto createUser(@RequestBody CreateUserRequest request);
}