package com.hospital.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hospital.entity.po.CheckItem;

import java.util.List;

public interface CheckItemService {

    IPage<CheckItem> pageChecks(Integer page, Integer size, String keyword);

    List<CheckItem> listAll();

    void addCheck(CheckItem checkItem);

    void updateCheck(CheckItem checkItem);

    void deleteCheck(Integer checkId);
}
