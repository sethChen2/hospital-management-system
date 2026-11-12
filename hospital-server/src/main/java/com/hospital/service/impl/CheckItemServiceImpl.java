package com.hospital.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospital.common.exception.BusinessException;
import com.hospital.entity.po.CheckItem;
import com.hospital.entity.po.RegisterCheck;
import com.hospital.mapper.CheckItemMapper;
import com.hospital.mapper.RegisterCheckMapper;
import com.hospital.service.CheckItemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
public class CheckItemServiceImpl implements CheckItemService {

    private final CheckItemMapper checkItemMapper;
    private final RegisterCheckMapper registerCheckMapper;

    public CheckItemServiceImpl(CheckItemMapper checkItemMapper,
                                RegisterCheckMapper registerCheckMapper) {
        this.checkItemMapper = checkItemMapper;
        this.registerCheckMapper = registerCheckMapper;
    }

    @Override
    public IPage<CheckItem> pageChecks(Integer page, Integer size, String keyword) {
        return checkItemMapper.selectPage(new Page<>(page, size), Wrappers.<CheckItem>lambdaQuery()
                .like(StringUtils.hasText(keyword), CheckItem::getCheckName, keyword)
                .orderByAsc(CheckItem::getCheckId));
    }

    @Override
    public List<CheckItem> listAll() {
        return checkItemMapper.selectList(Wrappers.<CheckItem>lambdaQuery()
                .orderByAsc(CheckItem::getCheckId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addCheck(CheckItem checkItem) {
        if (!StringUtils.hasText(checkItem.getCheckName())) {
            throw new BusinessException("检查项目名称不能为空");
        }
        checkItem.setCheckId(null);
        checkItemMapper.insert(checkItem);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCheck(CheckItem checkItem) {
        if (checkItem.getCheckId() == null) {
            throw new BusinessException("检查项目ID不能为空");
        }
        if (checkItemMapper.selectById(checkItem.getCheckId()) == null) {
            throw new BusinessException("检查项目不存在");
        }
        checkItemMapper.updateById(checkItem);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCheck(Integer checkId) {
        Long used = registerCheckMapper.selectCount(Wrappers.<RegisterCheck>lambdaQuery()
                .eq(RegisterCheck::getCheckId, checkId));
        if (used != null && used > 0) {
            throw new BusinessException("该检查项目已被 " + used + " 张检查单引用，无法删除");
        }
        checkItemMapper.deleteById(checkId);
        log.info("删除检查项目：checkId={}", checkId);
    }
}
