package com.hospital.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hospital.entity.po.Drug;

import java.util.List;

public interface DrugService {

    IPage<Drug> pageDrugs(Integer page, Integer size, String keyword);

    List<Drug> listAll();

    void addDrug(Drug drug);

    void updateDrug(Drug drug);

    void deleteDrug(Integer drugId);
}
