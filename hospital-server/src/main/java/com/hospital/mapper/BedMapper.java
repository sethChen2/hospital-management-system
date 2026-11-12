package com.hospital.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hospital.entity.po.Bed;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface BedMapper extends BaseMapper<Bed> {

    /** 管理员分页查询病床 */
    IPage<Bed> pageBeds(IPage<Bed> page,
                        @Param("bedStatus") String bedStatus,
                        @Param("keyword") String keyword);

    /** 患者查询本人住院信息 */
    List<Bed> selectByPatient(@Param("patientUserId") Integer patientUserId);

    /** 病床详情 */
    Bed selectDetail(@Param("bedId") Integer bedId);

    /** 取一张空闲床位号，没有则返回 null */
    Integer selectFreeBedId();

    /**
     * 原子占用床位：仅当仍为空闲时才成功，避免并发重复分配
     * @return 影响行数，0 表示已被别人抢走
     */
    int occupyBed(@Param("bedId") Integer bedId,
                  @Param("patientUserId") Integer patientUserId,
                  @Param("doctorId") Integer doctorId,
                  @Param("hospitalTime") LocalDateTime hospitalTime,
                  @Param("hospitalReason") String hospitalReason);

    /** 床位状态分布 [{name: 空闲/占用, value: n}] */
    List<Map<String, Object>> countByStatus();
}
