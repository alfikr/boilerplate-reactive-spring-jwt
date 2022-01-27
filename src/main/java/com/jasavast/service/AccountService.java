package com.jasavast.service;

import com.jasavast.core.error.*;
import com.jasavast.core.security.AuthorityConstants;
import com.jasavast.core.security.SecurityUserService;
import com.jasavast.domain.Authority;
import com.jasavast.domain.User;
import com.jasavast.repository.AuthorityRepository;
import com.jasavast.repository.UserRepository;
import com.jasavast.resource.vm.LoginVM;
import com.jasavast.service.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.HashSet;
import java.util.Set;

@Service
@Slf4j
public class AccountService {
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuthorityRepository authorityRepository;
    public Mono<User> register(UserDTO userDTO,String password){
        return userRepository.findOneByLogin(userDTO.getLogin())
                .flatMap(existingUser ->{
                    if (!existingUser.isAktif()){
                        return userRepository.delete(existingUser);
                    }else {
                        return Mono.error(new UsernameAlreadyUsedException());
                    }
                })
                .then(userRepository.findFirstByEmailIgnoreCase(userDTO.getEmail()))
                .flatMap(existingUser->{
                    if (!existingUser.isAktif()){
                        return userRepository.delete(existingUser);
                    }else {
                        return Mono.error(new EmailAlreadyUsedException());
                    }
                }).publishOn(Schedulers.boundedElastic())
                .then(Mono.fromCallable(()->{
                    return User.builder().build();
                }))
                .flatMap(newUser -> {
                    Set<Authority> authoritySet=new HashSet<>();
                    return authorityRepository.findById(AuthorityConstants.USER)
                            .map(authoritySet::add)
                            .thenReturn(newUser)
                            .doOnNext(user->user.setAuthorities(authoritySet))
                            .flatMap(userRepository::save)
                            .doOnNext(user -> log.info("user created {}",user));
                });
    }
    public Mono<JSONObject> forgotPassword(UserDTO userDTO){
        if (!userDTO.getEmail().isEmpty()){
            //skema send to email
            userRepository.findFirstByEmailIgnoreCase(userDTO.getEmail())
                    .switchIfEmpty(Mono.defer(()->Mono.error(new EmailNotRegisteredException())))
                    .doOnNext(user -> {
                        //send email
                    })
                    .map(user -> {
                        return new JSONObject()
                                .put("success",true)
                                .put("message","we have send mail to "+user.getEmail());
                    });
        }else if (!userDTO.getLogin().isEmpty()){
            //skema ponsel
            if (userDTO.getLogin().matches("^[0-9]{0,12}$")){
                userRepository.findOneByLogin(userDTO.getLogin())
                        .switchIfEmpty(Mono.defer(()->Mono.error(new PhoneNotRegisteredException())))
                        .doOnNext(user -> {

                        })
                        .map(user -> {
                            return new JSONObject()
                                    .put("success",true)
                                    .put("message","we have send mail to "+user.getEmail());
                        });
            }else{
                userRepository.findOneByLogin(userDTO.getLogin())
                        .switchIfEmpty(Mono.defer(()->Mono.error(new LoginNotAlreadyRegisteredException())))
                        .doOnNext(user -> {

                        })
                        .map(user -> {
                            return new JSONObject()
                                    .put("success",true)
                                    .put("message","we have send mail to "+user.getEmail());
                        });
            }
        }
        return Mono.empty();
    }
}
