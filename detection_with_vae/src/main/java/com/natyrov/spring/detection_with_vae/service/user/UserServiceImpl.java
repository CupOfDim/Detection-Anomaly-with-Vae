package com.natyrov.spring.detection_with_vae.service.user;

import com.natyrov.spring.detection_with_vae.dto.RegisterDto;
import com.natyrov.spring.detection_with_vae.entity.User;
import com.natyrov.spring.detection_with_vae.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService{
    @Autowired
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void registerUser(RegisterDto registerDto) {
        if(userRepository.existsByEmail(registerDto.getEmail())){
            throw new RuntimeException("Пользователь с таким email уже существует");
        }

        if(!registerDto.getPassword().equals(registerDto.getConfirmPassword())){
            throw new RuntimeException("Пароли не совпадают");
        }

        User user = User.builder()
                .username(registerDto.getUsername())
                .email(registerDto.getEmail())
                .password(passwordEncoder.encode(registerDto.getPassword()))
                .role("ROLE_USER")
                .build();

        userRepository.save(user);
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findAllByEmail(email)
                .orElseThrow(()->new RuntimeException("Пользователь с таким email не найден"));
    }
}
