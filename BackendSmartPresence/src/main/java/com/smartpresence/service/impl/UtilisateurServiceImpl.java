package com.smartpresence.service.impl;

import com.smartpresence.constants.RoleCode;
import com.smartpresence.dto.request.UserUpdateRequest;
import com.smartpresence.dto.response.UserResponse;
import com.smartpresence.entity.Role;
import com.smartpresence.entity.Utilisateur;
import com.smartpresence.exception.BusinessException;
import com.smartpresence.exception.ResourceNotFoundException;
import com.smartpresence.mapper.UtilisateurMapper;
import com.smartpresence.repository.RoleRepository;
import com.smartpresence.repository.UtilisateurRepository;
import com.smartpresence.service.UtilisateurService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UtilisateurServiceImpl implements UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final UtilisateurMapper utilisateurMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return utilisateurMapper.toResponseList(utilisateurRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findById(UUID id) {
        return utilisateurMapper.toResponse(getUtilisateur(id));
    }

    @Override
    @Transactional
    public UserResponse update(UUID id, UserUpdateRequest request) {
        Utilisateur utilisateur = getUtilisateur(id);

        if (request.getEmail() != null && !request.getEmail().equals(utilisateur.getEmail())
                && utilisateurRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Un compte existe déjà avec l'email : " + request.getEmail());
        }

        utilisateurMapper.updateEntityFromRequest(request, utilisateur);

        if (request.getMotDePasse() != null && !request.getMotDePasse().isBlank()) {
            utilisateur.setMotDePasse(passwordEncoder.encode(request.getMotDePasse()));
        }

        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            utilisateur.setRoles(resolveRoles(request.getRoles()));
        }

        if (request.getActif() != null) {
            utilisateur.setActif(request.getActif());
        }

        return utilisateurMapper.toResponse(utilisateurRepository.save(utilisateur));
    }

    @Override
    @Transactional
    public void deactivate(UUID id) {
        Utilisateur utilisateur = getUtilisateur(id);
        utilisateur.setActif(false);
        utilisateurRepository.save(utilisateur);
    }

    @Override
    @Transactional
    public void activate(UUID id) {
        Utilisateur utilisateur = getUtilisateur(id);
        utilisateur.setActif(true);
        utilisateurRepository.save(utilisateur);
    }

    private Utilisateur getUtilisateur(UUID id) {
        return utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", id));
    }

    private Set<Role> resolveRoles(Set<String> roleCodes) {
        Set<Role> roles = new HashSet<>();
        for (String roleStr : roleCodes) {
            String normalized = roleStr.startsWith("ROLE_") ? roleStr.substring(5) : roleStr;
            RoleCode roleCode = RoleCode.valueOf(normalized);
            Role role = roleRepository.findByCode(roleCode.name())
                    .orElseThrow(() -> new ResourceNotFoundException("Rôle introuvable : " + roleCode));
            roles.add(role);
        }
        return roles;
    }
}
