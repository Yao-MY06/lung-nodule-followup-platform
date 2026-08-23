package com.yiliao.statistics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yiliao.statistics.entity.StatsOverviewDaily;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

@Mapper
public interface StatsOverviewDailyMapper extends BaseMapper<StatsOverviewDaily> {

    /**
     * 物理删除当日快照：快照为可再生预聚合数据，重建走「物理删+重插」；
     * 不走 @TableLogic 逻辑删除（会与 uk_stat_date 唯一键冲突，见 docker/init-sql/yiliao_stats.sql 头注）。
     */
    @Delete("DELETE FROM stats_overview_daily WHERE stat_date = #{date}")
    int physicalDeleteByDate(@Param("date") LocalDate date);
}
