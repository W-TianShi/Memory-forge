package com.memoryforge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("tb_words")
public class Word {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String word;

    @TableField("en_pronunciation")
    private String enPronunciation;

    @TableField("us_pronunciation")
    private String usPronunciation;

    private String desc;
}