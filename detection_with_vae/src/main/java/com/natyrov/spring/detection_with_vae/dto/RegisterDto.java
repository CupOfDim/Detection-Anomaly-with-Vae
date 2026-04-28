package com.natyrov.spring.detection_with_vae.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;

@Getter
@Setter
public class RegisterDto {

    @NotBlank(message = "Имя обязательно для заполнения")
    private String username;

    @Email(message = "Некорректный Email")
    @NotBlank(message = "Email обязателен")
    private String email;

    @Size(message = "Пароль должен быть не кароче 8 символов", min = 8)
    @NotBlank(message = "Вы не указали пароль")
    private String password;

    @NotBlank(message = "Подтвердите пароль")
    private String confirmPassword;
}