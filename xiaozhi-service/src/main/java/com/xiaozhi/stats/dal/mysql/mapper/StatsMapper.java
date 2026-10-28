package com.xiaozhi.stats.dal.mysql.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 监控面板的聚合查询。
 * <p>
 * 只读、只做 COUNT/GROUP BY，不落任何业务规则；时间条件一律由调用方用 DateUtils 算好传进来，
 * 保证与库里的 createTime 同一口径（库里存的是应用时区的本地时间）。
 */
@Mapper
public interface StatsMapper {

    @Select("SELECT COUNT(*) FROM sys_message WHERE createTime >= #{from}")
    long countMessagesSince(@Param("from") LocalDateTime from);

    @Select("SELECT COUNT(DISTINCT deviceId) FROM sys_message WHERE createTime >= #{from} AND deviceId IS NOT NULL")
    long countActiveDevicesSince(@Param("from") LocalDateTime from);

    @Select("SELECT DATE_FORMAT(createTime, '%Y-%m-%d') AS day, COUNT(*) AS total "
        + "FROM sys_message WHERE createTime >= #{from} GROUP BY day ORDER BY day")
    List<Map<String, Object>> dailyMessages(@Param("from") LocalDateTime from);

    @Select("SELECT deviceId AS deviceId, COUNT(*) AS total FROM sys_message "
        + "WHERE createTime >= #{from} AND deviceId IS NOT NULL "
        + "GROUP BY deviceId ORDER BY total DESC LIMIT #{limit}")
    List<Map<String, Object>> topDevices(@Param("from") LocalDateTime from, @Param("limit") int limit);
}
