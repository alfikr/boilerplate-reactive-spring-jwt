package com.jasavast.resource.vm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

@Data
public class ResetPasswordVM {
    @NotBlank
    @ToString.Exclude
    private String key;

    @NotBlank
    @Size(min = UserVM.PASSWORD_MIN_LENGTH, max = UserVM.PASSWORD_MAX_LENGTH)
    @ToString.Exclude
    private String newPassword;
}
