package com.natyrov.spring.detection_with_vae.service.user;


import com.natyrov.spring.detection_with_vae.dto.RegisterDto;
import com.natyrov.spring.detection_with_vae.entity.User;


public interface UserService {
    void registerUser(RegisterDto registerDto);
    User findByEmail(String email);
}
