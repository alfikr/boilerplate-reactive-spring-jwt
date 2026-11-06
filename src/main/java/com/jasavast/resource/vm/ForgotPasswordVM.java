package com.jasavast.resource.vm;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/** Either email or login identifies the account. */
@Data
public class ForgotPasswordVM {
    @Size(max = 254)
    private String email;
    @Size(max = 50)
    private String login;

    @AssertTrue(message = "email or login is required")
    public boolean isIdentified() {
        return StringUtils.isNotBlank(email) || StringUtils.isNotBlank(login);
    }
}
