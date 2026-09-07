package com.smartpresence.service.impl;

import com.smartpresence.constants.RoleCode;
import com.smartpresence.dto.request.LoginRequest;
import com.smartpresence.dto.request.RefreshTokenRequest;
import com.smartpresence.dto.request.UserRegistrationRequest;
import com.smartpresence.dto.response.AuthResponse;
import com.smartpresence.dto.response.UserResponse;
import com.smartpresence.entity.Role;
import com.smartpresence.entity.Utilisateur;
import com.smartpresence.exception.BusinessException;
import com.smartpresence.exception.ResourceNotFoundException;
import com.smartpresence.mapper.UtilisateurMapper;
import com.smartpresence.repository.RoleRepository;
import com.smartpresence.repository.UtilisateurRepository;
import com.smartpresence.security.CustomUserDetails;
import com.smartpresence.security.JwtUtils;
import com.smartpresence.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implémentation du service d'authentification et d'inscription.
 *
 * @since 0.0.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final UtilisateurMapper utilisateurMapper;

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Tentative de connexion pour l'utilisateur : {}", request.getEmail());
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getMotDePasse())
        );

        CustomUserDetails userPrincipal = (CustomUserDetails) authentication.getPrincipal();

        String accessToken = jwtUtils.generateAccessToken(authentication);
        String refreshToken = jwtUtils.generateRefreshToken(authentication);

        Set<String> roles = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresInMs(jwtUtils.getExpirationMs())
                .userId(userPrincipal.getId())
                .email(userPrincipal.getEmail())
                .nom(userPrincipal.getNom())
                .prenom(userPrincipal.getPrenom())
                .roles(roles)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String token = request.getRefreshToken();
        if (!jwtUtils.validateToken(token)) {
            throw new BusinessException("Jeton de rafraîchissement invalide ou expiré");
        }

        String email = jwtUtils.getUsernameFromToken(token);
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'email : " + email));

        CustomUserDetails userDetails = CustomUserDetails.build(utilisateur);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        String newAccessToken = jwtUtils.generateAccessToken(authentication);
        String newRefreshToken = jwtUtils.generateRefreshToken(authentication);

        Set<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresInMs(jwtUtils.getExpirationMs())
                .userId(utilisateur.getId())
                .email(utilisateur.getEmail())
                .nom(utilisateur.getNom())
                .prenom(utilisateur.getPrenom())
                .roles(roles)
                .build();
    }

    @Override
    @Transactional
    public UserResponse register(UserRegistrationRequest request) {
        log.info("Inscription d'un nouvel utilisateur : {}", request.getEmail());
        if (utilisateurRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Un compte existe déjà avec l'email : " + request.getEmail());
        }

        Utilisateur utilisateur = utilisateurMapper.toEntity(request);
        utilisateur.setMotDePasse(passwordEncoder.encode(request.getMotDePasse()));

        Set<Role> roles = new HashSet<>();
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            for (String roleStr : request.getRoles()) {
                RoleCode roleCode;
                try {
                    String normalized = roleStr.startsWith("ROLE_") ? roleStr.substring(5) : roleStr;
                    roleCode = RoleCode.valueOf(normalized);
                } catch (IllegalArgumentException e) {
                    throw new BusinessException("Rôle invalide : " + roleStr);
                }
                Role role = roleRepository.findByCode(roleCode.name())
                        .orElseThrow(() -> new ResourceNotFoundException("Rôle introuvable : " + roleCode));
                roles.add(role);
            }
        } else {
            Role defaultRole = roleRepository.findByCode(RoleCode.ENSEIGNANT.name())
                    .orElseThrow(() -> new ResourceNotFoundException("Rôle par défaut introuvable"));
            roles.add(defaultRole);
        }

        utilisateur.setRoles(roles);
        Utilisateur saved = utilisateurRepository.save(utilisateur);

        return utilisateurMapper.toResponse(saved);
    }
}
