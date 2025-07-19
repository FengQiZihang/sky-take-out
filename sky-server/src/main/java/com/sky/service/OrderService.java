package com.sky.service;

import com.sky.dto.*;
import com.sky.exception.OrderBusinessException;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

public interface OrderService {

    // ---------------------------- 用户端接口 ---------------------------- //
    /**
     * 用户下单
     * @param ordersSubmitDTO 订单提交DTO
     * @return OrderSubmitVO 订单提交结果VO
     */
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * 订单支付，返回支付参数供前端调起支付
     * @param ordersPaymentDTO 订单支付DTO
     * @return OrderPaymentVO 订单支付结果VO
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功回调，修改订单状态
     * @param outTradeNo 商户订单号
     */
    void paySuccess(String outTradeNo);

    /**
     * 历史订单查询
     * @param page 页码
     * @param pageSize 每页记录数
     * @param status 订单状态
     * @return PageResult 分页查询结果
     */
    PageResult pageQueryForUser(int page, int pageSize, Integer status);

    /**
     * 根据订单id查询订单详情
     * @param id 订单id
     * @return OrderVO 订单VO
     */
    OrderVO getOrderDetailById(Long id);

    /**
     * 用户取消订单
     * @param id 订单id
     * @throws OrderBusinessException 订单不存在
     * @throws OrderBusinessException 订单状态错误
     */
    void userCancelById(Long id) throws Exception;

    /**
     * 再来一单
     * @param id 订单id
     */
    void repetition(Long id);


    // ---------------------------- 商户端接口 ---------------------------- //
    /**
     * 条件订单搜索
     * @param ordersPageQueryDTO 订单搜索条件
     * @return PageResult 分页查询结果
     */
    PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 各个状态的订单数量统计
     * @return OrderStatisticsVO 订单统计VO
     */
    OrderStatisticsVO statistics();

    /**
     * 接单
     * @param ordersConfirmDTO 订单接单DTO
     */
    void confirm(OrdersConfirmDTO ordersConfirmDTO);

    /**
     * 拒单
     * @param ordersRejectionDTO 订单拒单DTO
     */
    void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception;

    /**
     * 取消订单
     * @param ordersCancelDTO 订单取消DTO
     */
    void cancel(OrdersCancelDTO ordersCancelDTO) throws Exception;
}
