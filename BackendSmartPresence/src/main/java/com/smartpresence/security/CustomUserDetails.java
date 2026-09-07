package com.smartpresence.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.smartpresence.entity.Utilisateur;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implémentation {@link UserDetails} pour Spring Security.
 *
 * <p>Supporte la gestion RBAC multi-rôles ({@code Set<Role>}).</p>
 *
 * @since 0.0.1
 */
@AllArgsConstructor
@Getter
public class CustomUserDetails implements UserDetails {

    private final UUID id;
    private final String email;
    private final String nom;
    private final String prenom;

    @JsonIgnore
    private final String password;

    private final boolean actif;
    private final Collection<? extends GrantedAuthority> authorities;

    public static CustomUserDetails build(Utilisateur utilisateur) {
        Collection<GrantedAuthority> authorities = utilisateur.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getCode()))
                .collect(Collectors.toList());

        return new CustomUserDetails(
                utilisateur.getId(),
                utilisateur.getEmail(),
                utilisateur.getNom(),
                utilisateur.getPrenom(),
                utilisateur.getMotDePasse(),
                utilisateur.isActif(),
                authorities
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return actif;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return actif;
    }
}
