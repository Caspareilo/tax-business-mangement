package com.example.tax.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.tax.user.entity.SysUser;
import com.example.tax.user.mapper.SysUserMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final SysUserMapper userMapper;

    public CustomUserDetailsService(SysUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("账号不存在");
        }
        Set<String> roles = new HashSet<>(userMapper.selectRoleCodes(user.getId()));
        Set<String> permissions = new HashSet<>(userMapper.selectPermissionCodes(user.getId()));
        return new SecurityUser(user.getId(), user.getUsername(), user.getPasswordHash(),
                user.getDisplayName(), user.getStatus(), roles, permissions);
    }
}
