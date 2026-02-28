package com.smartmobility.userservice.dto;

import java.time.LocalDateTime;

public class ApiResponse<T> {

    private int status;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    // Constructeur privé — on passe par les méthodes statiques
    private ApiResponse(int status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    // ───────────────────────────────────────────────
    // Fabriques statiques pour les cas courants
    // ───────────────────────────────────────────────

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }

    public static <T> ApiResponse<T> created(String message, T data) {
        return new ApiResponse<>(201, message, data);
    }

    public static <T> ApiResponse<T> error(int status, String message) {
        return new ApiResponse<>(status, message, null);
    }

    // Surcharge pour les erreurs de validation qui contiennent le détail des champs
    public static <T> ApiResponse<T> error(int status, String message, T data) {
        return new ApiResponse<>(status, message, data);
    }

    // ───────────────────────────────────────────────
    // Getters / Setters
    // ───────────────────────────────────────────────

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}