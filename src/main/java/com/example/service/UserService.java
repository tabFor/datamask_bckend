package com.example.service;

import com.example.model.User;

import java.util.List;
import java.util.Optional;

public interface UserService {
    
    /**
     * 创建用户
     * @param user 用户信息
     * @return 创建的用户
     */
    User createUser(User user);
    
    /**
     * 获取所有用户
     * @return 用户列表
     */
    List<User> getAllUsers();
    
    /**
     * 根据ID查找用户
     * @param id 用户ID
     * @return 用户对象
     */
    Optional<User> getUserById(Long id);
    
    /**
     * 根据用户名查找用户
     * @param username 用户名
     * @return 用户对象
     */
    User findByUsername(String username);
    
    /**
     * 更新用户信息
     * @param user 用户信息
     * @return 更新后的用户
     */
    User updateUser(User user);
    
    /**
     * 删除用户
     * @param id 用户ID
     */
    void deleteUser(Long id);
    
    /**
     * 验证用户密码
     * @param rawPassword 原始密码
     * @param encodedPassword 加密后的密码
     * @return 是否匹配
     */
    boolean verifyPassword(String rawPassword, String encodedPassword);
    
    /**
     * 判断用户是否存在
     * @param id 用户ID
     * @return 是否存在
     */
    boolean existsById(Long id);
} 