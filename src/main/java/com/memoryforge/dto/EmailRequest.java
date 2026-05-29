package com.memoryforge.dto;

import lombok.Data;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Data
public class EmailRequest {
    @NotBlank @Email
    private String email;
}
