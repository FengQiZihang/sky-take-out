package com.sky.service;

import com.sky.dto.ShoppingCartDTO;

public interface ShoppingCartService {
    /**
     * 添加购物车
     * @param shoppingCartDTO 购物车DTO
     */
    void addShoppingCart(ShoppingCartDTO shoppingCartDTO);
}