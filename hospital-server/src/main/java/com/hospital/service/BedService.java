package com.hospital.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hospital.entity.po.Bed;

import java.util.List;

public interface BedService {

    IPage<Bed> pageBeds(Integer page, Integer size, String bedStatus, String keyword);

    List<Bed> listFreeBeds();

    List<Bed> listByPatient(Integer patientUserId);

    void addBed(Bed bed);

    void updateBed(Bed bed);

    void deleteBed(Integer bedId);
}
