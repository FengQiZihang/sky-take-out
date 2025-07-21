package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    // Mock 开关
    private static final boolean MOCK_PAY = true;

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
    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * {@inheritDoc}
     */
    @Transactional
    @Override
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        /**
         * 业务逻辑
         * 校验收货地址和购物车数据
         * 创建订单主表记录
         * 批量插入订单明细
         * 清空用户购物车
         */
        // 异常情况的处理（收货地址为空、超出配送范围、购物车为空）
        log.info("查看用户地址是否为空");
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            // 用户地址为空，不能下单
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        // 查询当前用户的购物车数据
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder().userId(userId).build();

        log.info("查看用户购物车是否为空");
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if (shoppingCartList.isEmpty()) {
            // 购物车数据为空，不能下单
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        // 构造订单数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setPhone(addressBook.getPhone());
        // 拼接详细地址
        String address = addressBook.getProvinceName() + addressBook.getCityName() + addressBook.getDistrictName() + addressBook.getDetail();
        orders.setAddress(address);
        orders.setConsignee(addressBook.getConsignee());
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setUserId(userId);
        orders.setStatus(Orders.PENDING_PAYMENT); // 待付款
        orders.setPayStatus(Orders.UN_PAID); // 未支付
        orders.setOrderTime(LocalDateTime.now()); // 设置下单时间

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
    @Override
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception{
        /**
         * 业务逻辑
         * 支持模拟支付和真实微信支付两种模式
         * 调用微信支付接口生成预支付交易单
         * 返回支付参数供前端调起支付
         */
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);
        
        // ---------------------- 模拟支付流程 ---------------------- //
        if (MOCK_PAY) {
            log.info("模拟支付，直接调用 paySuccess() 方法");
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
        log.info("实际支付，调用微信支付接口");
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
        log.info("根据订单号查询当前用户的订单:outTradeNo={}, userId={}", outTradeNo, userId);
        Orders ordersDB = orderMapper.getByNumberAndUserId(outTradeNo, userId);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED) // 待接单
                .payStatus(Orders.PAID) // 已支付
                .checkoutTime(LocalDateTime.now())
                .build();

        log.info("根据订单id更新订单的状态、支付状态、结账时间:{}", orders);
        orderMapper.update(orders);

        // 通过websocket向客户端浏览器推送消息
        Map map = new HashMap();
        map.put("type", 1); // 1表示来单提醒 2表示客户催单
        map.put("orderId", ordersDB.getId());
        map.put("content", "订单号:" + outTradeNo + ",支付成功!");
        webSocketServer.sendToAllClient(JSON.toJSONString(map));
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
        log.info("历史订单查询:{}", ordersPageQueryDTO);
        Page<Orders> pageResult = orderMapper.pageQuery(ordersPageQueryDTO);


        // 将订单数据封装到 OrderVO 中
        List<OrderVO> orderVOList = new ArrayList<>();
        if (pageResult.getResult() != null && pageResult.getTotal() > 0) {
            for (Orders order : pageResult.getResult()) {
                // 根据订单id查询订单明细
                log.info("根据订单id查询订单明细:orderId={}", order.getId());
                List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(order.getId());

                // 将订单数据封装到 OrderVO 中
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(order, orderVO);
                orderVO.setOrderDetailList(orderDetailList);

                // 将 OrderVO 添加到 OrderVO 列表中
                orderVOList.add(orderVO);
            }
        }
        log.info("历史订单查询:{}", orderVOList);

        // 封装分页查询结果并返回
        return new PageResult(pageResult.getTotal(), orderVOList);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OrderVO getOrderDetailById(Long id) {
        // 根据订单id查询订单
        log.info("根据订单id查询订单:id={}", id);
        Orders orders = orderMapper.getById(id);

        // 根据订单id查询订单明细
        log.info("根据订单id查询订单明细:id={}", id);
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
        /**
         * 业务逻辑
         * 待付款：用户直接取消
         * 待接单：商家需要为用户退款
         * 已接单/派送中：用户需要电话联系商家
         */
        // 根据id查询订单
        Orders ordersDB = getOrderById(id);

        log.info("订单状态:{},1 待付款 2 待接单 3 已接单 4 派送中 5 已完成 6 已取消", ordersDB.getStatus());
        if (ordersDB.getStatus() > 2) {
            // 订单状态错误
            // TODO 电话联系商家
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 订单处于待接单状态下取消，需要进行退款
        boolean needRefund = ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED);
        if (needRefund) {
            doRefund(ordersDB);
        }

        // 用户取消：只设置取消原因
        updateOrderToCancelled(id, "用户取消", null, needRefund);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void repetition(Long id) {
        // 查询当前的用户id
        Long userId = BaseContext.getCurrentId();

        // 根据订单id查询当前订单详情
        log.info("根据订单id查询当前订单详情:id={}", id);
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        // 将订单详情对象转换为购物车对象
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map((item) -> {
            ShoppingCart shoppingCart = new ShoppingCart();

            // 将 orderDetail 的属性值拷贝到 shoppingCart 中
            BeanUtils.copyProperties(item, shoppingCart, "id");

            // 设置购物车对象的用户id和创建时间
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());

            return shoppingCart;
        }).collect(Collectors.toList());

        // 将购物车对象批量插入购物车表
        log.info("将购物车对象批量插入购物车表:{}", shoppingCartList);
        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        // 设置分页查询条件
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        // 执行分页查询
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        // 部分订单状态，需要额外返回订单菜品信息，将Orders转化为OrderVO
        List<OrderVO> orderVOList = page.getResult().stream().map((order) -> {
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(order, orderVO);
            String orderDishes = getOrderDishesStr(order);
            orderVO.setOrderDishes(orderDishes);
            return orderVO;
        }).collect(Collectors.toList());
        log.info("订单搜索:{}", orderVOList);

        // 封装分页查询结果并返回
        return new PageResult(page.getTotal(), orderVOList);
    }

    /**
     * 根据订单id获取菜品信息字符串
     * @param order 订单
     * @return 订单菜品信息字符串
     */
    private String getOrderDishesStr(Orders order) {
        // 根据订单id查询订单明细
        log.info("根据订单id查询订单明细:orderId={}", order.getId());
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(order.getId());

        // 将每一条订单菜品信息拼接为字符串（格式：宫保鸡丁*3；）
        List<String> orderDishList = orderDetailList.stream().map(item -> {
            return item.getName() + " * " + item.getNumber() + "; ";
        }).collect(Collectors.toList());
        log.info("订单菜品信息字符串:{}", orderDishList);

        // 将该订单对应的所有菜品信息拼接在一起
        return String.join("", orderDishList);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OrderStatisticsVO statistics() {
        // 根据状态，统计订单数量
        log.info("分别统计 待接单、待派送、派送中 状态的订单数量");
        Integer toBeConfirmed = orderMapper.countByStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countByStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countByStatus(Orders.DELIVERY_IN_PROGRESS);

        // 将订单数据封装到 OrderStatisticsVO 中
        return OrderStatisticsVO.builder()
                .toBeConfirmed(toBeConfirmed)
                .confirmed(confirmed)
                .deliveryInProgress(deliveryInProgress)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED) // 已接单
                .build();
        log.info("根据订单id更新订单的状态:{}", orders);
        orderMapper.update(orders);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        /**
         * 业务逻辑
         * 待接单：商家可以拒单，需给出拒单原因
         * 已支付：商家需要为用户退款
         */
        // 根据id查询订单
        Orders ordersDB = getOrderById(ordersRejectionDTO.getId());

        log.info("订单状态:{},1 待付款 2 待接单 3 已接单 4 派送中 5 已完成 6 已取消", ordersDB.getStatus());
        if (!ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            // 订单状态错误
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 支付状态为1（已支付）时要为用户退款
        log.info("订单支付状态:{},0 未支付 1 已支付 2 退款", ordersDB.getPayStatus());
        boolean needRefund = ordersDB.getPayStatus().equals(Orders.PAID);
        if (needRefund) {
            doRefund(ordersDB);
        }

        // 商家拒单：只设置拒绝原因
        updateOrderToCancelled(ordersRejectionDTO.getId(), null, ordersRejectionDTO.getRejectionReason(), needRefund);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cancel(OrdersCancelDTO ordersCancelDTO) throws Exception {
        /**
         * 业务逻辑
         * 已支付：商家需要为用户退款
         * 取消时需要指定取消原因
         */
        // 根据id查询订单
        Orders ordersDB = getOrderById(ordersCancelDTO.getId());

        // 支付状态为1（已支付）时要为用户退款
        log.info("订单支付状态:{},0 未支付 1 已支付 2 退款", ordersDB.getPayStatus());
        boolean needRefund = ordersDB.getPayStatus().equals(Orders.PAID);
        if (needRefund) {
            doRefund(ordersDB);
        }

        // 商家取消：只设置取消原因
        updateOrderToCancelled(ordersCancelDTO.getId(), ordersCancelDTO.getCancelReason(), null, needRefund);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delivery(Long id) {
        // 根据id查询订单
        Orders ordersDB = getOrderById(id);

        // 订单状态为3（已接单）才可以派送
        log.info("订单状态:{},1 待付款 2 待接单 3 已接单 4 派送中 5 已完成 6 已取消", ordersDB.getStatus());
        if (!ordersDB.getStatus().equals(Orders.CONFIRMED)) {
            // 订单状态错误
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 更新订单状态、派送时间
        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.DELIVERY_IN_PROGRESS) // 派送中
                .build();
        log.info("更新订单状态、派送时间:{}", orders);
        orderMapper.update(orders);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void complete(Long id) {
        // 根据id查询订单
        Orders ordersDB = getOrderById(id);

        // 订单状态为4（派送中）才可以完成
        log.info("订单状态:{},1 待付款 2 待接单 3 已接单 4 派送中 5 已完成 6 已取消", ordersDB.getStatus());
        if (!ordersDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            // 订单状态错误
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 更新订单状态
        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.COMPLETED) // 已完成
                .deliveryTime(LocalDateTime.now()) // 设置送达时间
                .build();
        log.info("更新订单状态:{}", orders);
        orderMapper.update(orders);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reminder(Long id) {
        // 根据id查询订单
        Orders orders = getOrderById(id);

        // 通过websocket向客户端浏览器推送消息
        Map map = new HashMap();
        map.put("type", 2); // 1表示来单提醒 2表示客户催单
        map.put("orderId", orders.getId());
        map.put("content", "订单号:" + orders.getNumber() + ",提醒催单了!");
        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }

    // ---------------------------- 抽取公共方法 ---------------------------- //
    /**
     * 根据ID查询订单，如果不存在则抛异常
     * @param id 订单ID
     * @return 订单信息
     */
    private Orders getOrderById(Long id) {
        // 根据id查询订单
        log.info("根据id查询订单:id={}", id);
        Orders orders = orderMapper.getById(id);
        if (orders == null) {
            // 订单不存在
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        return orders;
    }

    /**
     * 执行退款操作
     * @param ordersDB 订单信息
     * @throws Exception 退款异常
     */
    private void doRefund(Orders ordersDB) throws Exception {
        if (MOCK_PAY) {
            // -------------------------- 模拟退款流程 ---------------------- //
            log.info("模拟退款，直接返回成功");
        } else {
            // -------------------------- 实际退款流程 ---------------------- //
            log.info("实际退款，调用微信支付退款接口");
            //调用微信支付退款接口
            weChatPayUtil.refund(
                    ordersDB.getNumber(), // 商户订单号
                    ordersDB.getNumber(), // 商户退款单号
                    new BigDecimal(0.01), // 退款金额，单位 元
                    new BigDecimal(0.01) // 原订单金额
            );
        }
    }

    /**
     * 更新订单为取消状态的通用方法
     * @param id 订单ID
     * @param cancelReason 取消原因（可为null）
     * @param rejectionReason 拒绝原因（可为null）
     * @param needRefund 是否需要退款
     */
    private void updateOrderToCancelled(Long id, String cancelReason, String rejectionReason, boolean needRefund) {
        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.CANCELLED) // 已取消
                .cancelTime(LocalDateTime.now()) // 设置取消时间
                .build();

        // 设置取消原因（如果有）
        if (cancelReason != null) {
            orders.setCancelReason(cancelReason);
        }

        // 设置拒绝原因（如果有）
        if (rejectionReason != null) {
            orders.setRejectionReason(rejectionReason);
        }

        // 设置退款状态（如果需要）
        if (needRefund) {
            orders.setPayStatus(Orders.REFUND); // 支付状态设置为 退款
        }

        log.info("更新订单状态为已取消:{}", orders);
        orderMapper.update(orders);
    }
}
