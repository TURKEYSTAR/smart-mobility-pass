package com.smartmobility.userservice.exception;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(UUID id) {
        super("Utilisateur introuvable avec l'id : " + id);
    }

    public UserNotFoundException(String identifier) {
        super("Utilisateur introuvable : " + identifier);
    }
}