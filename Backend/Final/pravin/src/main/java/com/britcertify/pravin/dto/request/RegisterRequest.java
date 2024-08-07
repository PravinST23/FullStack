package com.britcertify.pravin.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    @NonNull
    private String name;

    @NonNull
    private String email;

    @NonNull
    private String mobilenumber;

    @NonNull
    private String password;

    @NonNull
    private String role;
    
    private String age;

    private String address;
     
    private String state;

    private String city;

    private String postalcode;

    private String skills;

    public String getMobilenumber() {
        return mobilenumber;
    }
}
