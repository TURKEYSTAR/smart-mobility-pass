package com.smartmobility.userservice.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long id) {
        super("Utilisateur introuvable avec l'id : " + id);
    }

    public UserNotFoundException(String username) {
        super("Utilisateur introuvable avec le username : " + username);
    }
}