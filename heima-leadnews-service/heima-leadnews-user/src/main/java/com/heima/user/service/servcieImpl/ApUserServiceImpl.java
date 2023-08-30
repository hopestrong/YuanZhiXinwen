package com.heima.user.service.servcieImpl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.user.dtos.LoginDto;
import com.heima.model.user.pojos.ApUser;
import com.heima.user.mapper.ApUserMapper;
import com.heima.user.service.ApUserService;
import com.heima.utils.common.AppJwtUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.HashMap;
import java.util.Map;


@Service
public class ApUserServiceImpl extends ServiceImpl<ApUserMapper, ApUser> implements ApUserService {
    @Override
    public ResponseResult login(LoginDto dto) {
        if (!StringUtils.isBlank(dto.getPhone()) &&
                !StringUtils.isBlank((dto.getPassword()))) {
            ApUser ap = getOne(Wrappers.<ApUser>lambdaQuery().eq(ApUser::getPhone, dto.getPhone()));
            if (ap == null) {
                return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST, "用户不存在");
            }

            String salt = ap.getSalt();
            String password = ap.getPassword();
            String encodePassword = DigestUtils.md5DigestAsHex((dto.getPassword() + salt).getBytes());
            if (!encodePassword.equals(password)) {
                return ResponseResult.errorResult(AppHttpCodeEnum.LOGIN_PASSWORD_ERROR, "密码错误");
            }
            Map<String,Object> map = new HashMap();
            map.put("token", AppJwtUtil.getToken(ap.getId().longValue()));
            ap.setPassword("");
            ap.setSalt("");
            map.put("user",ap);
            return ResponseResult.okResult(map);
        }else{
            Map<String, Object> map = new HashMap<>();
            map.put("token", AppJwtUtil.getToken(0l));
            return ResponseResult.okResult(map);
        }

    }
}
