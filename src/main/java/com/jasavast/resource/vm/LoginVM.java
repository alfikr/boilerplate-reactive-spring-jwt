package com.jasavast.resource.vm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

@Data
public class LoginVM {
    @NotBlank
    @Size(max = 50)
    private String login;
    @NotBlank
    @Size(max = 100)
    @ToString.Exclude
    private String password;
    private boolean rememberMe;
}
