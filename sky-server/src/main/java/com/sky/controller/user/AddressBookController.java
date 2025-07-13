package com.sky.controller.user;

import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.entity.AddressBook;
import com.sky.result.Result;
import com.sky.service.AddressBookService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/user/addressBook")
@Api(tags = "C端-地址簿接口")
@Slf4j
public class AddressBookController {

    @Autowired
    private AddressBookService addressBookService;

    /**
     * 查询当前登录用户的所有地址信息
     * @return Result<List<AddressBook>> 地址列表
     */
    @GetMapping("/list")
    @ApiOperation("查询当前登录用户的所有地址信息")
    public Result<List<AddressBook>> list() {
        log.info("【用户端】查询当前登录用户的所有地址信息");
        AddressBook addressBook = new AddressBook();
        addressBook.setUserId(BaseContext.getCurrentId());
        List<AddressBook> list = addressBookService.list(addressBook);
        return Result.success(list);
    }

    /**
     * 新增地址
     * @param addressBook 地址信息
     */
    @PostMapping
    @ApiOperation("新增地址")
    public Result<String> save(@RequestBody AddressBook addressBook) {
        log.info("【用户端】新增地址:{}", addressBook);
        addressBookService.save(addressBook);
        return Result.success();
    }

    /**
     * 根据id查询地址
     * @param id 地址id
     * @return Result<AddressBook> 地址信息
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询地址")
    public Result<AddressBook> getById(@PathVariable Long id) {
        log.info("【用户端】根据id查询地址:{}", id);
        AddressBook addressBook = addressBookService.getById(id);
        return Result.success(addressBook);
    }

    /**
     * 根据id修改地址
     * @param addressBook 地址信息
     */
    @PutMapping
    @ApiOperation("根据id修改地址")
    public Result<String> update(@RequestBody AddressBook addressBook) {
        log.info("【用户端】根据id修改地址:{}", addressBook);
        addressBookService.update(addressBook);
        return Result.success();
    }

    /**
     * 设置默认地址
     * @param addressBook 地址信息
     */
    @PutMapping("/default")
    @ApiOperation("设置默认地址")
    public Result<String> setDefault(@RequestBody AddressBook addressBook) {
        log.info("【用户端】设置默认地址:{}", addressBook);
        addressBookService.setDefault(addressBook);
        return Result.success();
    }

    /**
     * 根据id删除地址
     * @param id 地址id
     */
    @DeleteMapping
    @ApiOperation("根据id删除地址")
    public Result<String> deleteById(Long id) {
        log.info("【用户端】根据id删除地址:{}", id);
        addressBookService.deleteById(id);
        return Result.success();
    }

    /**
     * 查询默认地址
     * @return Result<AddressBook> 地址信息
     */
    @GetMapping("default")
    @ApiOperation("查询默认地址")
    public Result<AddressBook> getDefault() {
        log.info("【用户端】查询默认地址");
        //SQL:select * from address_book where user_id = ? and is_default = 1
        AddressBook addressBook = new AddressBook();
        addressBook.setIsDefault(StatusConstant.DEFAULT);
        addressBook.setUserId(BaseContext.getCurrentId());
        List<AddressBook> list = addressBookService.list(addressBook);

        if (!list.isEmpty() && list.get(0) != null) {
            return Result.success(list.get(0));
        }

        return Result.error(MessageConstant.DEFAULT_ADDRESS_NOT_FOUND);
    }

}
