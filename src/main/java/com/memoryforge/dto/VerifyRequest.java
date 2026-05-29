package com.memoryforge.dto;

import lombok.Data;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Data
public class VerifyRequest {
    @NotBlank @Email
    private String email;
    @NotBlank
    private String code;
}
