package com.example.tax.auth.vo;

import java.util.List;

public record CurrentUserVO(
        Long id,
        String username,
        String displayName,
        List<String> roles,
        List<String> permissions,
        List<MenuVO> menus
) {
    public record MenuVO(String code, String name, String path, Integer sortOrder) {
    }
}
