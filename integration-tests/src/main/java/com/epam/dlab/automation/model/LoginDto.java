package com.epam.dlab.automation.model;

public class LoginDto {

    private String username;
    private String password;
    private String access_token;
    
    public String getUsername() {
        return username;
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
    
    public String getAccess_token() {
        return access_token;
    }
    
    public LoginDto(String username, String password) {
        this.username = username;
        this.password = password;
        this.access_token = "";
    }
   
    public LoginDto(){
        
    }
}
