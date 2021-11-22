package com.jasavast.resource;

import com.jasavast.resource.vm.UserVM;
import com.jasavast.service.AccountService;
import com.jasavast.service.dto.UserDTO;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
public class AccountResource {
    @Autowired
    private AccountService accountService;
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Void> register(@RequestBody UserVM userVM){
        return accountService.register(userVM,userVM.getPassword())
                .doOnSuccess(user -> {
                    //TODO Send email
                }).then();
    }
    @PostMapping("/forgot-password")
    public Mono<JSONObject> forgotPassword(@RequestBody UserDTO userDto){
        return accountService.forgotPassword(userDto);
    }
}
