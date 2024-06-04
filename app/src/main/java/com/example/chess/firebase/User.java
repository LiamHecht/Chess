package com.example.chess.firebase;

public class User {
    private int id;

    private String email;
    private String username;

    private int rating;
    private int games;
    private String password;
    private String profileImageUrl;

    public User() {
    }

    public User(String username, String password, String email, int rating, int games) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.rating = rating;
        this.games = games;
    }

    public String getEmail() {
        return email;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getRating() {
        return rating;
    }

    public int getGames() {
        return games;
    }
    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
    public void setRating(int rating) {
        this.rating = rating;
    }
}
