package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    // Mock 开关
    private static final boolean mockPay = true;

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;

    /**
     * {@inheritDoc}
     */
    @Transactional
    @Override
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        // 异常情况的处理（收货地址为空、超出配送范围、购物车为空）
        log.info("根据id查询地址 {}", ordersSubmitDTO.getAddressBookId());
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            // 用户地址为空，不能下单
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        // 查询当前用户的购物车数据
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder().userId(userId).build();
        log.info("根据用户id查询购物车 {}", shoppingCart.getUserId());
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if (shoppingCartList.isEmpty()) {
            // 购物车数据为空，不能下单
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        // 构造订单数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setPhone(orders.getPhone());
        orders.setAddress(orders.getAddress());
        orders.setConsignee(orders.getConsignee());
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setUserId(userId);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setPayStatus(Orders.UN_PAID);
        orders.setOrderTime(LocalDateTime.now());

        // 向订单表插入1条数据
        log.info("向订单表插入1条数据 {}", orders);
        orderMapper.insert(orders);
        // 订单明细数据
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId()); // 设置当前 orderDetail 关联的订单id
            orderDetailList.add(orderDetail);
        }
        // 向订单明细表插入n条数据
        log.info("向订单明细表插入n条数据 {}", orderDetailList);
        orderDetailMapper.insertBatch(orderDetailList);
        // 清空购物车数据
        log.info("清空购物车数据 {}", userId);
        shoppingCartMapper.deleteByUserId(userId);
        // 封装返回结果
        return OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderTime(orders.getOrderTime())
                .orderAmount(orders.getAmount())
                .build();
    }

    /**
     * {@inheritDoc}
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception{
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);


        // ---------------------- 模拟支付流程 ---------------------- //
        if (mockPay) {
            log.info("【用户端】模拟支付，直接调用 paySuccess() 方法");
            // 模拟支付成功
            paySuccess(ordersPaymentDTO.getOrderNumber());
            // 构造极简版支付结果
            return OrderPaymentVO.builder()
                    .nonceStr("nonceStr")
                    .paySign("paySign")
                    .timeStamp("timeStamp")
                    .signType("signType")
                    .packageStr("packageStr")
                    .build(); // 前端已注释 wx.requestPayment, 所以这里可以返回任意值，直接拿到 VO 后重定向
        }

        // ---------------------- 实际支付流程 ---------------------- //
        log.info("【用户端】实际支付，调用微信支付接口");
        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        // 判断支付状态
        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        // 封装返回结果
        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * {@inheritDoc}
     */
    public void paySuccess(String outTradeNo) {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();

        // 根据订单号查询当前用户的订单
        Orders ordersDB = orderMapper.getByNumberAndUserId(outTradeNo, userId);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResult pageQueryForUser(int page, int pageSize, Integer status) {
        // 构造查询条件-ordersPageQueryDTO
        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        ordersPageQueryDTO.setStatus(status);

        // 设置分页查询条件
        PageHelper.startPage(page, pageSize);

        // 执行分页查询
        log.info("【用户端】历史订单查询:ordersPageQueryDTO={}", ordersPageQueryDTO);
        Page<Orders> pageResult = orderMapper.pageQuery(ordersPageQueryDTO);


        // 将订单数据封装到 OrderVO 中
        List<OrderVO> orderVOList = new ArrayList<>();
        if (pageResult.getResult() != null && pageResult.getTotal() > 0) {
            for (Orders order : pageResult.getResult()) {
                // 根据订单id查询订单明细
                log.info("【用户端】根据订单id查询订单明细:orderId={}", order.getId());
                List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(order.getId());

                // 将订单数据封装到 OrderVO 中
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(order, orderVO);
                orderVO.setOrderDetailList(orderDetailList);

                // 将 OrderVO 添加到 OrderVO 列表中
                orderVOList.add(orderVO);
            }
        }
        log.info("【用户端】历史订单查询:orderVOList={}", orderVOList);

        // 封装分页查询结果并返回
        return new PageResult(pageResult.getTotal(), orderVOList);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OrderVO getOrderDetailById(Long id) {
        // 根据订单id查询订单
        log.info("【用户端】根据订单id查询订单:id={}", id);
        Orders orders = orderMapper.getById(id);

        // 根据订单id查询订单明细
        log.info("【用户端】根据订单id查询订单明细:id={}", id);
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        // 将订单数据封装到 OrderVO 中
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);

        return orderVO;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void userCancelById(Long id) throws Exception {
        // 根据id查询订单
        log.info("【用户端】根据id查询订单:id={}", id);
        Orders ordersDB = orderMapper.getById(id);

        if (ordersDB == null) {
            // 订单不存在
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        log.info("【用户端】订单状态:{}", ordersDB.getStatus());

        // 订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        if (ordersDB.getStatus() > 2) {
            // 订单状态错误
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(id);

        // 订单处于待接单状态下取消，需要进行退款
        if (ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {

            if (mockPay) {
                // -------------------------- 模拟退款流程 ---------------------- //
                log.info("【用户端】模拟退款，直接返回成功");
            } else {
                // -------------------------- 实际退款流程 ---------------------- //
                log.info("【用户端】实际退款，调用微信支付退款接口");
                //调用微信支付退款接口
                weChatPayUtil.refund(
                        ordersDB.getNumber(), // 商户订单号
                        ordersDB.getNumber(), // 商户退款单号
                        new BigDecimal(0.01), // 退款金额，单位 元
                        new BigDecimal(0.01) // 原订单金额
                );
            }

            // 支付状态 修改为 退款
            orders.setPayStatus(Orders.REFUND);
        }

        // 更新订单状态、取消原因、取消时间
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }
}
