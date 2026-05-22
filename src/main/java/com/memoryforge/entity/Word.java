package com.memoryforge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("tb_words")
public class Word {

    @TableId(type = IdType.AUTO)
    private Integer id;

    // 完全和数据库字段一样
    private String word;
    private String en_pronunciation;  // 英式音标
    private String us_pronunciation;  // 美式音标
    private String desc;               // 中文释义
}