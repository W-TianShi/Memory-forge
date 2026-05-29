package com.memoryforge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("verification_code")
public class VerificationCode {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String email;
    private String code;
    private String type;
    private LocalDateTime expiresAt;
    private Integer used;
    private LocalDateTime createdAt;
}
