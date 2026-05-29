package com.memoryforge.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.memoryforge.entity.VerificationCode;
import com.memoryforge.mapper.VerificationCodeMapper;
import com.memoryforge.service.VerificationCodeService;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Random;

@Service
public class VerificationCodeServiceImpl extends ServiceImpl<VerificationCodeMapper, VerificationCode> implements VerificationCodeService {

    @Override
    public String generateCode(String email, String type) {
        String code = String.format("%06d", new Random().nextInt(1000000));
        VerificationCode vc = new VerificationCode();
        vc.setEmail(email);
        vc.setCode(code);
        vc.setType(type);
        vc.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        vc.setUsed(0);
        save(vc);
        return code;
    }

    @Override
    public boolean verify(String email, String code, String type) {
        VerificationCode vc = getBaseMapper().findValidCode(email, code, type);
        if (vc == null) return false;
        vc.setUsed(1);
        updateById(vc);
        return true;
    }
}
