package com.jasavast.core.security;


import com.jasavast.core.error.UserNotActivatedException;
import com.jasavast.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Component("userDetailsService")
@Slf4j
public class SecurityUserService implements ReactiveUserDetailsService {
    @Autowired
    private UserRepository userRepository;
    @Override
    public Mono<UserDetails> findByUsername(String s) {
        return userRepository.findOneByLogin(s.toLowerCase())
                .switchIfEmpty(Mono.error(new UsernameNotFoundException("User "+s.toLowerCase()+" tidak ditemukan")))
                .map(user -> {
                    if (!user.isAktif()){
                        throw new UserNotActivatedException("user "+ s.toLowerCase()+" belum melakukan aktivasi");
                    }
                    List<GrantedAuthority> grantedAuthorityList = user.getAuthorities()
                            .stream()
                            .map(authority -> new SimpleGrantedAuthority(authority.getName()))
                            .collect(Collectors.toList());
                    return new User(user.getLogin(),"",grantedAuthorityList);
                });
    }
}
