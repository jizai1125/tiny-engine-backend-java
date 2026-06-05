package com.tinyengine.it.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tinyengine.it.model.entity.AiUserSetting;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiUserSettingMapper extends BaseMapper<AiUserSetting> {
}
