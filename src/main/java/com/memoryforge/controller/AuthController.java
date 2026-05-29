package com.memoryforge.controller;

import com.memoryforge.dto.AuthRequest;
import com.memoryforge.dto.AuthResponse;
import com.memoryforge.dto.EmailRequest;
import com.memoryforge.dto.VerifyRequest;
import com.memoryforge.entity.User;
import com.memoryforge.mapper.UserMapper;
import com.memoryforge.service.EmailService;
import com.memoryforge.service.VerificationCodeService;
import com.memoryforge.config.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final VerificationCodeService vcService;
    private final EmailService emailService;

    public AuthController(UserMapper userMapper, PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil, VerificationCodeService vcService,
                          EmailService emailService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.vcService = vcService;
        this.emailService = emailService;
    }

    @PostMapping("/send-code")
    public AuthResponse sendCode(@RequestBody @Valid EmailRequest req) {
        // check rate limit: one code per 60 seconds
        if (vcService.hasRecentCode(req.getEmail(), "REGISTER", 60)) {
            return new AuthResponse(false, "发送太频繁，请稍后再试", null, null);
        }
        String code = vcService.generateCode(req.getEmail(), "REGISTER");
        emailService.sendCode(req.getEmail(), code);
        return new AuthResponse(true, "验证码已发送", null, null);
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody @Valid AuthRequest req) {
        User exist = userMapper.findByEmail(req.getEmail());
        if (exist != null) {
            return new AuthResponse(false, "该邮箱已注册", null, null);
        }
        if (!vcService.verify(req.getEmail(), req.getCode(), "REGISTER")) {
            return new AuthResponse(false, "验证码错误或已过期", null, null);
        }
        User user = new User();
        user.setEmail(req.getEmail());
        user.setUsername(req.getEmail().split("@")[0]);
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setEmailVerified(0);
        user.setStatus(1);
        userMapper.insert(user);

        String token = jwtUtil.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(true, "注册成功", token, user.getUsername());
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody @Valid AuthRequest req) {
        User user = userMapper.findByEmail(req.getEmail());
        if (user == null) {
            return new AuthResponse(false, "邮箱未注册", null, null);
        }
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            return new AuthResponse(false, "密码错误", null, null);
        }
        String token = jwtUtil.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(true, "登录成功", token, user.getUsername());
    }

    @PostMapping("/verify")
    public AuthResponse verifyEmail(@RequestBody @Valid VerifyRequest req) {
        boolean ok = vcService.verify(req.getEmail(), req.getCode(), "REGISTER");
        if (!ok) {
            return new AuthResponse(false, "验证码错误或已过期", null, null);
        }
        User user = userMapper.findByEmail(req.getEmail());
        if (user != null) {
            user.setEmailVerified(1);
            userMapper.updateById(user);
        }
        return new AuthResponse(true, "验证成功", null, null);
    }
}
