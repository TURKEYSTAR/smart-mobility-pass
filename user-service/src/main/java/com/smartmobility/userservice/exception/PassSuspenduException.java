package com.smartmobility.userservice.exception;

public class PassSuspenduException extends RuntimeException {

    public PassSuspenduException(String passNumber) {
        super("Le Mobility Pass " + passNumber + " est suspendu. Veuillez le réactiver pour continuer.");
    }
}