package com.yiliao.statistics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yiliao.statistics.entity.StatsFollowupMonthly;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StatsFollowupMonthlyMapper extends BaseMapper<StatsFollowupMonthly> {

    /** 物理删除当月快照（同 StatsOverviewDailyMapper#physicalDeleteByDate 的取舍说明）。 */
    @Delete("DELETE FROM stats_followup_monthly WHERE stat_month = #{month}")
    int physicalDeleteByMonth(@Param("month") String month);
}
