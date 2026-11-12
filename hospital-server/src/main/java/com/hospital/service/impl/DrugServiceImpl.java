package com.hospital.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospital.common.exception.BusinessException;
import com.hospital.entity.po.Drug;
import com.hospital.entity.po.Prescription;
import com.hospital.mapper.DrugMapper;
import com.hospital.mapper.PrescriptionMapper;
import com.hospital.service.DrugService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
public class DrugServiceImpl implements DrugService {

    private final DrugMapper drugMapper;
    private final PrescriptionMapper prescriptionMapper;

    public DrugServiceImpl(DrugMapper drugMapper, PrescriptionMapper prescriptionMapper) {
        this.drugMapper = drugMapper;
        this.prescriptionMapper = prescriptionMapper;
    }

    @Override
    public IPage<Drug> pageDrugs(Integer page, Integer size, String keyword) {
        return drugMapper.selectPage(new Page<>(page, size), Wrappers.<Drug>lambdaQuery()
                .like(StringUtils.hasText(keyword), Drug::getDrugName, keyword)
                .orderByAsc(Drug::getDrugId));
    }

    @Override
    public List<Drug> listAll() {
        return drugMapper.selectList(Wrappers.<Drug>lambdaQuery().orderByAsc(Drug::getDrugId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addDrug(Drug drug) {
        if (!StringUtils.hasText(drug.getDrugName())) {
            throw new BusinessException("药品名称不能为空");
        }
        drug.setDrugId(null);
        drugMapper.insert(drug);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDrug(Drug drug) {
        if (drug.getDrugId() == null) {
            throw new BusinessException("药品ID不能为空");
        }
        if (drugMapper.selectById(drug.getDrugId()) == null) {
            throw new BusinessException("药品不存在");
        }
        drugMapper.updateById(drug);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDrug(Integer drugId) {
        Long used = prescriptionMapper.selectCount(Wrappers.<Prescription>lambdaQuery()
                .eq(Prescription::getDrugId, drugId));
        if (used != null && used > 0) {
            throw new BusinessException("该药品已被 " + used + " 张处方引用，无法删除");
        }
        drugMapper.deleteById(drugId);
        log.info("删除药品：drugId={}", drugId);
    }
}
