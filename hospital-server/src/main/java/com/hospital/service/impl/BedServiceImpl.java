package com.hospital.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospital.common.Constants;
import com.hospital.common.exception.BusinessException;
import com.hospital.entity.po.Bed;
import com.hospital.mapper.BedMapper;
import com.hospital.service.BedService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class BedServiceImpl implements BedService {

    private final BedMapper bedMapper;

    public BedServiceImpl(BedMapper bedMapper) {
        this.bedMapper = bedMapper;
    }

    @Override
    public IPage<Bed> pageBeds(Integer page, Integer size, String bedStatus, String keyword) {
        return bedMapper.pageBeds(new Page<>(page, size), bedStatus, keyword);
    }

    @Override
    public List<Bed> listFreeBeds() {
        return bedMapper.selectList(Wrappers.<Bed>lambdaQuery()
                .eq(Bed::getBedStatus, Constants.BED_FREE)
                .orderByAsc(Bed::getBedId));
    }

    @Override
    public List<Bed> listByPatient(Integer patientUserId) {
        return bedMapper.selectByPatient(patientUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addBed(Bed bed) {
        bed.setBedId(null);
        bed.setBedStatus(Constants.BED_FREE);
        bed.setPatientUserId(null);
        bed.setDoctorId(null);
        bed.setHospitalTime(null);
        bed.setHospitalReason(null);
        bed.setVersion(0);
        bedMapper.insert(bed);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBed(Bed bed) {
        if (bed.getBedId() == null) {
            throw new BusinessException("病床ID不能为空");
        }
        if (bedMapper.selectById(bed.getBedId()) == null) {
            throw new BusinessException("病床不存在");
        }
        bedMapper.updateById(bed);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBed(Integer bedId) {
        Bed bed = bedMapper.selectById(bedId);
        if (bed == null) {
            throw new BusinessException("病床不存在");
        }
        if (Constants.BED_USED.equals(bed.getBedStatus())) {
            throw new BusinessException("该病床正在使用中，无法删除");
        }
        bedMapper.deleteById(bedId);
    }
}
