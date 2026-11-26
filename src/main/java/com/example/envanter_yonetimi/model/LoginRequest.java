package com.example.envanter_yonetimi.model;

public class LoginRequest {
    private String email;
    private String password;

    // getter ve setter
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
}
