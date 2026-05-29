package com.memoryforge.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.memoryforge.entity.VerificationCode;

public interface VerificationCodeService extends IService<VerificationCode> {
    String generateCode(String email, String type);
    boolean verify(String email, String code, String type);
}
