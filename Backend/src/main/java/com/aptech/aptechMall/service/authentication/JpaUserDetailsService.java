package com.aptech.aptechMall.service.authentication;

import com.aptech.aptechMall.model.jpa.User;
import com.aptech.aptechMall.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JpaUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Check if username exists, otherwise try email
        boolean usernameExists = userRepository.existsByUsername(username);
        User user = usernameExists
                ? userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng: " + username))
                : userRepository.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng: " + username));

        // Return the User entity directly (it implements UserDetails)
        // This allows AuthenticationUtil.getCurrentUser() to work correctly
        return user;
    }
}
