package com.gb28181.sipserver.repository;

import com.gb28181.sipserver.entity.DeviceInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;

/**
 * 设备信息数据访问层
 * 
 * 提供设备信息的数据库操作接口，包括：
 * - 基本的CRUD操作
 * - 设备状态查询
 * - 超时设备清理
 * 
 * @author GB28181 Team
 * @version 1.0.0
 */
@Repository
public interface DeviceRepository extends JpaRepository<DeviceInfo, String> {

    /**
     * 根据在线状态查询设备
     * 
     * @param online 在线状态
     * @return 设备列表
     */
    List<DeviceInfo> findByOnline(Boolean online);

    /**
     * 根据在线状态分页查询设备
     * 
     * @param online   在线状态
     * @param pageable 分页参数
     * @return 分页设备列表
     */
    Page<DeviceInfo> findByOnline(Boolean online, Pageable pageable);

    /**
     * 根据推流状态查询设备
     * 
     * @param live 推流状态
     * @return 设备列表
     */
    List<DeviceInfo> findByLive(Boolean live);

    /**
     * 查询超时的设备
     * 
     * @param timeoutThreshold 超时阈值时间戳
     * @return 超时设备列表
     */
    @Query("SELECT d FROM DeviceInfo d WHERE d.lastHeartbeatTime < :timeoutThreshold")
    List<DeviceInfo> findTimeoutDevices(@Param("timeoutThreshold") Long timeoutThreshold);

    /**
     * 批量删除超时设备
     * 
     * @param timeoutThreshold 超时阈值时间戳
     * @return 删除的设备数量
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM DeviceInfo d WHERE d.lastHeartbeatTime < :timeoutThreshold AND d.live = false")
    int deleteTimeoutDevices(@Param("timeoutThreshold") Long timeoutThreshold);

    /**
     * 更新设备在线状态
     * 
     * @param deviceId 设备ID
     * @param online   在线状态
     * @return 更新的记录数
     */
    @Modifying
    @Transactional
    @Query("UPDATE DeviceInfo d SET d.online = :online WHERE d.deviceId = :deviceId")
    int updateOnlineStatus(@Param("deviceId") String deviceId, @Param("online") Boolean online);

    /**
     * 更新设备推流状态
     * 
     * @param deviceId 设备ID
     * @param live     推流状态
     * @return 更新的记录数
     */
    @Modifying
    @Transactional
    @Query("UPDATE DeviceInfo d SET d.live = :live WHERE d.deviceId = :deviceId")
    int updateLiveStatus(@Param("deviceId") String deviceId, @Param("live") Boolean live);

    /**
     * 统计在线设备数量
     * 
     * @return 在线设备数量
     */
    @Query("SELECT COUNT(d) FROM DeviceInfo d WHERE d.online = true")
    long countOnlineDevices();

    /**
     * 统计推流设备数量
     * 
     * @return 推流设备数量
     */
    @Query("SELECT COUNT(d) FROM DeviceInfo d WHERE d.live = true")
    long countLiveDevices();

    /**
     * 查询僵尸推流会话（有callId但未进入推流状态的设备）
     * 
     * @return 僵尸会话设备列表
     */
    List<DeviceInfo> findByLiveCallIDIsNotNullAndLive(Boolean live);

    // ============ 原子更新方法（解决并发竞态条件） ============

    /**
     * 原子更新设备心跳（仅更新心跳相关字段，不影响推流状态）
     *
     * @param deviceId 设备ID
     * @param now      当前时间戳
     * @return 更新的记录数
     */
    @Modifying
    @Transactional
    @Query("UPDATE DeviceInfo d SET d.lastHeartbeatTime = :now, d.time = :now, d.online = true WHERE d.deviceId = :deviceId")
    int atomicUpdateHeartbeat(@Param("deviceId") String deviceId, @Param("now") Long now);

    /**
     * 原子更新设备推流信息（仅更新推流相关字段，不影响心跳状态）
     *
     * @param deviceId 设备ID
     * @param callId   会话ID
     * @param fromInfo From信息
     * @param toInfo   To信息
     * @param ssrc     SSRC
     * @return 更新的记录数
     */
    @Modifying
    @Transactional
    @Query("UPDATE DeviceInfo d SET d.liveCallID = :callId, d.liveFromInfo = :fromInfo, " +
            "d.liveToInfo = :toInfo, d.ssrc = :ssrc, d.live = true WHERE d.deviceId = :deviceId")
    int atomicUpdateLiveInfo(@Param("deviceId") String deviceId,
            @Param("callId") String callId,
            @Param("fromInfo") String fromInfo,
            @Param("toInfo") String toInfo,
            @Param("ssrc") String ssrc);

    /**
     * 原子清除设备推流信息
     *
     * @param deviceId 设备ID
     * @return 更新的记录数
     */
    @Modifying
    @Transactional
    @Query("UPDATE DeviceInfo d SET d.liveCallID = null, d.liveFromInfo = null, " +
            "d.liveToInfo = null, d.ssrc = null, d.live = false WHERE d.deviceId = :deviceId")
    int atomicClearLiveInfo(@Param("deviceId") String deviceId);

    /**
     * 原子标记设备需要强制重新注册
     *
     * @param deviceId 设备ID
     * @param now      当前时间戳
     * @return 更新的记录数
     */
    @Modifying
    @Transactional
    @Query("UPDATE DeviceInfo d SET d.forceReregister = true, d.forceReregisterTime = :now, " +
            "d.online = false WHERE d.deviceId = :deviceId")
    int atomicMarkForceReregister(@Param("deviceId") String deviceId, @Param("now") Long now);

    /**
     * 原子清除强制重新注册标记
     *
     * @param deviceId 设备ID
     * @return 更新的记录数
     */
    @Modifying
    @Transactional
    @Query("UPDATE DeviceInfo d SET d.forceReregister = false, d.forceReregisterTime = null WHERE d.deviceId = :deviceId")
    int atomicClearForceReregister(@Param("deviceId") String deviceId);
}
