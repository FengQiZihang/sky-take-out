package com.sky.service;

import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

public interface OrderService {

    /**
     * 用户下单
     * @param ordersSubmitDTO 订单提交DTO
     * @return OrderSubmitVO 订单提交结果VO
     */
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * 订单支付
     * @param ordersPaymentDTO 订单支付DTO
     * @return OrderPaymentVO 订单支付结果VO
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
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
     */
    void userCancelById(Long id) throws Exception;
}
