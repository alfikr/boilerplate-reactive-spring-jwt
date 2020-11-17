package com.jasavast.core.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.UserDetails;
import reactor.core.publisher.Mono;

public class SecurityUtils {
    private SecurityUtils(){}
    private static String extractPrincipal(Authentication authentication){
        if (authentication==null){
            return null;
        }else if(authentication.getPrincipal() instanceof UserDetails){
            UserDetails springSecurityUser = (UserDetails) authentication.getPrincipal();
            return springSecurityUser.getUsername();
        }else if (authentication.getPrincipal() instanceof String){
            return (String) authentication.getPrincipal();
        }
        return null;
    }
    public static Mono<String> getCurrentUserLogin(){
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(authentication ->
                        Mono.justOrEmpty(extractPrincipal(authentication))
                );
    }
    public static Mono<String> getCurrentUserJWT() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(authentication -> authentication.getCredentials() instanceof String)
                .map(authentication -> (String) authentication.getCredentials());
    }
    public static Mono<Boolean> isAuthenticated() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getAuthorities)
                .map(authorities -> authorities.stream()
                        .map(GrantedAuthority::getAuthority)
                        .noneMatch(AuthorityConstants.ANONYMOUS::equals)
                );
    }
    public static Mono<Boolean> isCurrentUserInRole(String authority) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getAuthorities)
                .map(authorities -> authorities.stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(authority::equals)
                );
    }
}
