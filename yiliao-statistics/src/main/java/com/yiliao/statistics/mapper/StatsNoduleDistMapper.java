package com.yiliao.statistics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yiliao.statistics.entity.StatsNoduleDist;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StatsNoduleDistMapper extends BaseMapper<StatsNoduleDist> {

    /** 物理清空分布快照（每轮全量重建；同 StatsOverviewDailyMapper#physicalDeleteByDate 的取舍说明）。 */
    @Delete("DELETE FROM stats_nodule_dist")
    int physicalDeleteAll();
}
