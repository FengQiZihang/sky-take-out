package com.sky.service;

import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.PasswordErrorException;
import com.sky.result.PageResult;

public interface EmployeeService {

    /**
     * 校验登录 并返回员工实体
     * @param employeeLoginDTO 员工登录DTO,包含用户名和密码
     * @return employee 员工实体
     * @throws AccountNotFoundException 如果用户名不存在
     * @throws PasswordErrorException 如果密码错误
     * @throws AccountLockedException 如果账号被锁定
     */
    Employee login(EmployeeLoginDTO employeeLoginDTO);

    /**
     * 新增员工 将 DTO 转换为实体类 并设置初始值 插入数据库
     * @param employeeDTO 员工DTO
     */
    void save(EmployeeDTO employeeDTO);

    /**
     * 分页查询员工
     * @param employeePageQueryDTO 员工分页查询DTO
     * @return PageResult 分页查询结果
     */
    PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO);

    /**
     * 启用或禁用员工账号
     * @param status 状态 0禁用 1启用
     * @param id 员工ID
     */
    void startOrStop(Integer status, Long id);

    /**
     * 根据id查询员工
     * @param id 员工ID
     * @return Employee 员工实体
     */
    Employee getById(Long id);
}
