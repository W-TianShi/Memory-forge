package com.memoryforge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.memoryforge.entity.VerificationCode;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface VerificationCodeMapper extends BaseMapper<VerificationCode> {
    @Select("SELECT * FROM verification_code WHERE email = #{email} AND code = #{code} AND type = #{type} AND used = 0 AND expires_at > NOW() ORDER BY created_at DESC LIMIT 1")
    VerificationCode findValidCode(String email, String code, String type);

    @Select("SELECT * FROM verification_code WHERE email = #{email} AND type = #{type} AND created_at > DATE_SUB(NOW(), INTERVAL #{seconds} SECOND) ORDER BY created_at DESC LIMIT 1")
    VerificationCode findRecentCode(String email, String type, int seconds);

    @Delete("DELETE FROM verification_code WHERE expires_at < NOW()")
    int deleteExpired();
}
