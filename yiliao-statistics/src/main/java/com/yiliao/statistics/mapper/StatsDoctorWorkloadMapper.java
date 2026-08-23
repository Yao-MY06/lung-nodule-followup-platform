package com.yiliao.statistics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yiliao.statistics.entity.StatsDoctorWorkload;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

@Mapper
public interface StatsDoctorWorkloadMapper extends BaseMapper<StatsDoctorWorkload> {

    /** 物理删除当日工作量快照（同 StatsOverviewDailyMapper#physicalDeleteByDate 的取舍说明）。 */
    @Delete("DELETE FROM stats_doctor_workload WHERE stat_date = #{date}")
    int physicalDeleteByDate(@Param("date") LocalDate date);
}
