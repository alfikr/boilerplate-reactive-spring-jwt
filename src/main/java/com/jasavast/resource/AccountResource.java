package com.jasavast.resource;

import com.jasavast.core.security.filter.JWTFilter;
import com.jasavast.core.security.filter.TokenProvider;
import com.jasavast.core.api.ApiResponse;
import com.jasavast.resource.vm.ForgotPasswordVM;
import com.jasavast.resource.vm.JwtTokenVM;
import com.jasavast.resource.vm.LoginVM;
import com.jasavast.resource.vm.ResetPasswordVM;
import com.jasavast.resource.vm.UserVM;
import com.jasavast.service.AccountService;
import com.jasavast.service.dto.UserDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Locale;
import java.util.Map;

/**
 * Account lifecycle and authentication. These are regular REST endpoints because they are
 * public (pre-authentication) and have fixed contracts; business operations go through
 * {@link CoreApi}.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AccountResource {

    private final AccountService accountService;
    private final ReactiveAuthenticationManager authenticationManager;
    private final TokenProvider tokenProvider;

    @PostMapping("/login")
    public Mono<ResponseEntity<JwtTokenVM>> login(@Valid @RequestBody LoginVM loginVM) {
        UsernamePasswordAuthenticationToken credentials = UsernamePasswordAuthenticationToken.unauthenticated(
                loginVM.getLogin().toLowerCase(Locale.ROOT), loginVM.getPassword());
        return authenticationManager.authenticate(credentials)
                .map(authentication -> tokenProvider.createToken(authentication, loginVM.isRememberMe()))
                .map(jwt -> ResponseEntity.ok()
                        .header(JWTFilter.AUTHORIZATION_HEADER, "Bearer " + jwt)
                        .body(new JwtTokenVM(jwt)));
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Void> register(@Valid @RequestBody UserVM userVM) {
        return accountService.register(userVM).then();
    }

    @GetMapping("/activate")
    public Mono<ApiResponse<Map<String, String>>> activate(@RequestParam String key) {
        return accountService.activate(key)
                .thenReturn(ApiResponse.ok(Map.of("message", "Account activated")));
    }

    @GetMapping("/account")
    public Mono<UserDTO> getAccount() {
        return accountService.getCurrentAccount();
    }

    @PostMapping("/forgot-password")
    public Mono<ApiResponse<Map<String, String>>> forgotPassword(@Valid @RequestBody ForgotPasswordVM vm) {
        return accountService.requestPasswordReset(vm)
                .thenReturn(ApiResponse.ok(Map.of("message",
                        "If the account exists and is active, a reset link has been sent to its e-mail")));
    }

    @PostMapping("/reset-password")
    public Mono<ApiResponse<Map<String, String>>> resetPassword(@Valid @RequestBody ResetPasswordVM vm) {
        return accountService.completePasswordReset(vm.getKey(), vm.getNewPassword())
                .thenReturn(ApiResponse.ok(Map.of("message", "Password changed")));
    }
}
