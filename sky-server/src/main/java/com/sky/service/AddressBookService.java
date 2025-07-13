package com.sky.service;

import com.sky.entity.AddressBook;
import java.util.List;

public interface AddressBookService {

    /**
     * 根据条件查询地址簿
     * @param addressBook 地址簿
     * @return List<AddressBook> 地址簿列表
     */
    List<AddressBook> list(AddressBook addressBook);

    /**
     * 新增地址
     * @param addressBook 地址簿
     */
    void save(AddressBook addressBook);

    /**
     * 根据id查询地址簿
     * @param id 地址簿id
     * @return AddressBook 地址簿
     */
    AddressBook getById(Long id);

    /**
     * 修改地址簿
     * @param addressBook 地址簿
     */
    void update(AddressBook addressBook);

    /**
     * 设置默认地址
     * @param addressBook 地址簿
     */
    void setDefault(AddressBook addressBook);

    /**
     * 根据id删除地址簿
     * @param id 地址簿id
     */
    void deleteById(Long id);

}
