package com.ruoyi.vlstream.test.vlstream.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.mp.base.BaseServiceImpl;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAlgorithmMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsDeviceInfoMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsMobileSceneGovernanceMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsMobileSceneGovernanceSubTaskMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.Algorithm;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceInfo;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.MobileSceneGovernance;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.MobileSceneGovernanceSubTask;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.MobileSceneGovernanceLoopVO;
import com.ruoyi.vlstream.test.vlstream.service.IVlsMobileSceneGovernanceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 移动端场景治理主任务表 服务实现类
 */
@Service
@RequiredArgsConstructor
public class VlsMobileSceneGovernanceServiceImpl extends BaseServiceImpl<VlsMobileSceneGovernanceMapper, MobileSceneGovernance> implements IVlsMobileSceneGovernanceService {

	private static final String MODE_IMMEDIATE = "immediate";
	private static final String MODE_LOOP = "loop";
	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

	private final VlsMobileSceneGovernanceSubTaskMapper mobileSceneGovernanceSubTaskMapper;
	private final VlsAlgorithmMapper vlsAlgorithmMapper;
	private final VlsDeviceInfoMapper vlsDeviceInfoMapper;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean saveImmediate(MobileSceneGovernance mobileSceneGovernance) {
		validateCommon(mobileSceneGovernance);
		mobileSceneGovernance.setGovernanceMode(MODE_IMMEDIATE);
		mobileSceneGovernance.setCycleType(null);
		mobileSceneGovernance.setIntervalDays(null);
		mobileSceneGovernance.setWeeklyDays(null);
		mobileSceneGovernance.setMonthlyDays(null);
		mobileSceneGovernance.setTriggerTimes(null);
		return save(mobileSceneGovernance);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean saveLoop(MobileSceneGovernance mobileSceneGovernance) {
		validateCommon(mobileSceneGovernance);
		validateLoop(mobileSceneGovernance);
		mobileSceneGovernance.setGovernanceMode(MODE_LOOP);
		boolean saved = save(mobileSceneGovernance);
		if (!saved || mobileSceneGovernance.getId() == null) {
			return false;
		}
		List<MobileSceneGovernanceSubTask> subTaskList = buildSubTaskList(mobileSceneGovernance);
		for (MobileSceneGovernanceSubTask subTask : subTaskList) {
			mobileSceneGovernanceSubTaskMapper.insert(subTask);
		}
		return true;
	}

	@Override
	public IPage<MobileSceneGovernance> listImmediate(IPage<MobileSceneGovernance> page) {
		IPage<MobileSceneGovernance> immediatePage = page(page, Wrappers.<MobileSceneGovernance>lambdaQuery()
			.eq(MobileSceneGovernance::getGovernanceMode, MODE_IMMEDIATE)
			.orderByDesc(MobileSceneGovernance::getCreateTime));
		for (MobileSceneGovernance immediate : immediatePage.getRecords()) {
			immediate.setAlgorithmNames(resolveAlgorithmNames(immediate.getAlgorithmIds()));
			immediate.setCameraNames(resolveCameraNames(immediate.getCameraIds()));
		}
		return immediatePage;
	}

	@Override
	public IPage<MobileSceneGovernanceLoopVO> listLoop(IPage<MobileSceneGovernance> page) {
		IPage<MobileSceneGovernance> loopPage = page(page, Wrappers.<MobileSceneGovernance>lambdaQuery()
			.eq(MobileSceneGovernance::getGovernanceMode, MODE_LOOP)
			.orderByDesc(MobileSceneGovernance::getCreateTime));
		List<MobileSceneGovernanceLoopVO> resultList = new ArrayList<>();
		for (MobileSceneGovernance loop : loopPage.getRecords()) {
			MobileSceneGovernanceLoopVO loopVO = new MobileSceneGovernanceLoopVO();
			loopVO.setId(loop.getId());
			loopVO.setName(loop.getName());
			loopVO.setGovernanceMode(loop.getGovernanceMode());
			loopVO.setCycleType(loop.getCycleType());
			loopVO.setIntervalDays(loop.getIntervalDays());
			loopVO.setWeeklyDays(loop.getWeeklyDays());
			loopVO.setMonthlyDays(loop.getMonthlyDays());
			loopVO.setStartTime(loop.getStartTime());
			loopVO.setEndTime(loop.getEndTime());
			loopVO.setTriggerTimes(loop.getTriggerTimes());
			loopVO.setLocationIds(loop.getLocationIds());
			loopVO.setAlgorithmIds(loop.getAlgorithmIds());
			loopVO.setCameraIds(loop.getCameraIds());
			loopVO.setDescription(loop.getDescription());
			loopVO.setAlgorithmNames(resolveAlgorithmNames(loop.getAlgorithmIds()));
			loopVO.setCameraNames(resolveCameraNames(loop.getCameraIds()));
			loopVO.setTenantId(loop.getTenantId());
			loopVO.setCreateUser(loop.getCreateUser());
			loopVO.setCreateDept(loop.getCreateDept());
			loopVO.setCreateTime(loop.getCreateTime());
			loopVO.setUpdateUser(loop.getUpdateUser());
			loopVO.setUpdateTime(loop.getUpdateTime());
			loopVO.setStatus(loop.getStatus());
			loopVO.setIsDeleted(loop.getIsDeleted());
			List<MobileSceneGovernanceSubTask> subTaskList = mobileSceneGovernanceSubTaskMapper.selectList(Wrappers.<MobileSceneGovernanceSubTask>lambdaQuery()
				.eq(MobileSceneGovernanceSubTask::getGovernanceId, loop.getId())
				.orderByAsc(MobileSceneGovernanceSubTask::getExecuteTime));
			loopVO.setSubTaskList(subTaskList);
			resultList.add(loopVO);
		}
		Page<MobileSceneGovernanceLoopVO> resultPage = new Page<>(loopPage.getCurrent(), loopPage.getSize(), loopPage.getTotal());
		resultPage.setRecords(resultList);
		return resultPage;
	}

	private void validateCommon(MobileSceneGovernance mobileSceneGovernance) {
		if (mobileSceneGovernance == null) {
			throw new ServiceException("请求参数不能为空");
		}
		if (StringUtils.isBlank(mobileSceneGovernance.getName())) {
			throw new ServiceException("治理名称不能为空");
		}
		if (StringUtils.isBlank(mobileSceneGovernance.getLocationIds())) {
			throw new ServiceException("分析区域不能为空");
		}
		if (StringUtils.isBlank(mobileSceneGovernance.getAlgorithmIds())) {
			throw new ServiceException("AI算法不能为空");
		}
		if (StringUtils.isBlank(mobileSceneGovernance.getCameraIds())) {
			throw new ServiceException("摄像头不能为空");
		}
	}

	private void validateLoop(MobileSceneGovernance mobileSceneGovernance) {
		if (StringUtils.isBlank(mobileSceneGovernance.getCycleType())) {
			throw new ServiceException("循环周期类型不能为空");
		}
		if (mobileSceneGovernance.getStartTime() == null || mobileSceneGovernance.getEndTime() == null) {
			throw new ServiceException("开始时间和结束时间不能为空");
		}
		if (mobileSceneGovernance.getEndTime().isBefore(mobileSceneGovernance.getStartTime())) {
			throw new ServiceException("结束时间不能早于开始时间");
		}
		if (StringUtils.isBlank(mobileSceneGovernance.getTriggerTimes())) {
			throw new ServiceException("触发时间不能为空");
		}
		if ("everyOtherDay".equals(mobileSceneGovernance.getCycleType())) {
			Integer intervalDays = mobileSceneGovernance.getIntervalDays();
			if (intervalDays == null || intervalDays <= 0) {
				throw new ServiceException("隔天模式间隔天数必须大于0");
			}
		}
	}

	private List<MobileSceneGovernanceSubTask> buildSubTaskList(MobileSceneGovernance mobileSceneGovernance) {
		List<LocalTime> triggerTimeList = parseTriggerTimeList(mobileSceneGovernance.getTriggerTimes());
		List<MobileSceneGovernanceSubTask> resultList = new ArrayList<>();
		LocalDate startDate = mobileSceneGovernance.getStartTime().toLocalDate();
		LocalDate endDate = mobileSceneGovernance.getEndTime().toLocalDate();
		for (LocalDate currentDate = startDate; !currentDate.isAfter(endDate); currentDate = currentDate.plusDays(1)) {
			if (!matchCycleDate(mobileSceneGovernance, currentDate, startDate)) {
				continue;
			}
			for (LocalTime triggerTime : triggerTimeList) {
				LocalDateTime executeTime = LocalDateTime.of(currentDate, triggerTime);
				if (executeTime.isBefore(mobileSceneGovernance.getStartTime()) || executeTime.isAfter(mobileSceneGovernance.getEndTime())) {
					continue;
				}
				MobileSceneGovernanceSubTask subTask = new MobileSceneGovernanceSubTask();
				subTask.setGovernanceId(mobileSceneGovernance.getId());
				subTask.setName(mobileSceneGovernance.getName());
				subTask.setExecuteTime(executeTime);
				subTask.setTaskStatus("pending");
				subTask.setLocationIds(mobileSceneGovernance.getLocationIds());
				subTask.setAlgorithmIds(mobileSceneGovernance.getAlgorithmIds());
				subTask.setCameraIds(mobileSceneGovernance.getCameraIds());
				resultList.add(subTask);
			}
		}
		return resultList;
	}

	private boolean matchCycleDate(MobileSceneGovernance mobileSceneGovernance, LocalDate currentDate, LocalDate startDate) {
		String cycleType = mobileSceneGovernance.getCycleType();
		if ("everyday".equals(cycleType)) {
			return true;
		}
		if ("everyOtherDay".equals(cycleType)) {
			Integer intervalDays = mobileSceneGovernance.getIntervalDays() == null ? 1 : mobileSceneGovernance.getIntervalDays();
			long dayOffset = java.time.temporal.ChronoUnit.DAYS.between(startDate, currentDate);
			return dayOffset % intervalDays == 0;
		}
		if ("weekly".equals(cycleType)) {
			Set<Integer> weeklyDaySet = parseIntSet(mobileSceneGovernance.getWeeklyDays());
			if (weeklyDaySet.isEmpty()) {
				weeklyDaySet.add(startDate.getDayOfWeek().getValue());
			}
			DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
			return weeklyDaySet.contains(dayOfWeek.getValue());
		}
		if ("monthly".equals(cycleType)) {
			Set<Integer> monthlyDaySet = parseIntSet(mobileSceneGovernance.getMonthlyDays());
			if (monthlyDaySet.isEmpty()) {
				monthlyDaySet.add(startDate.getDayOfMonth());
			}
			return monthlyDaySet.contains(currentDate.getDayOfMonth());
		}
		throw new ServiceException("不支持的循环周期类型");
	}

	private List<LocalTime> parseTriggerTimeList(String triggerTimes) {
		List<LocalTime> triggerTimeList = new ArrayList<>();
		String[] triggerTimeArray = triggerTimes.split(",");
		for (String triggerTimeText : triggerTimeArray) {
			String triggerTimeTrimText = StringUtils.trimToEmpty(triggerTimeText);
			if (StringUtils.isBlank(triggerTimeTrimText)) {
				continue;
			}
			triggerTimeList.add(LocalTime.parse(triggerTimeTrimText, TIME_FORMATTER));
		}
		if (triggerTimeList.isEmpty()) {
			throw new ServiceException("触发时间不能为空");
		}
		return triggerTimeList;
	}

	private Set<Integer> parseIntSet(String valueText) {
		if (StringUtils.isBlank(valueText)) {
			return new HashSet<>();
		}
		return Arrays.stream(valueText.split(","))
			.map(String::trim)
			.filter(StringUtils::isNotBlank)
			.map(Integer::valueOf)
			.collect(Collectors.toSet());
	}

	private String resolveAlgorithmNames(String algorithmIds) {
		List<Long> algorithmIdList = parseIdList(algorithmIds);
		if (algorithmIdList.isEmpty()) {
			return StringUtils.EMPTY;
		}
		List<Algorithm> algorithmList = vlsAlgorithmMapper.selectBatchIds(algorithmIdList.stream().distinct().collect(java.util.stream.Collectors.toList()));
		Map<Long, String> algorithmNameMap = new HashMap<>();
		for (Algorithm algorithm : algorithmList) {
			if (algorithm == null || algorithm.getId() == null) {
				continue;
			}
			algorithmNameMap.put(algorithm.getId(), algorithm.getName());
		}
		List<String> algorithmNameList = new ArrayList<>();
		for (Long algorithmId : algorithmIdList) {
			String algorithmName = algorithmNameMap.get(algorithmId);
			if (StringUtils.isNotBlank(algorithmName)) {
				algorithmNameList.add(algorithmName);
			}
		}
		return String.join(",", algorithmNameList);
	}

	private String resolveCameraNames(String cameraIds) {
		List<Long> cameraIdList = parseIdList(cameraIds);
		if (cameraIdList.isEmpty()) {
			return StringUtils.EMPTY;
		}
		List<DeviceInfo> deviceInfoList = vlsDeviceInfoMapper.selectBatchIds(cameraIdList.stream().distinct().collect(java.util.stream.Collectors.toList()));
		Map<Long, String> cameraNameMap = new HashMap<>();
		for (DeviceInfo deviceInfo : deviceInfoList) {
			if (deviceInfo == null || deviceInfo.getId() == null) {
				continue;
			}
			cameraNameMap.put(deviceInfo.getId(), deviceInfo.getDeviceName());
		}
		List<String> cameraNameList = new ArrayList<>();
		for (Long cameraId : cameraIdList) {
			String cameraName = cameraNameMap.get(cameraId);
			if (StringUtils.isNotBlank(cameraName)) {
				cameraNameList.add(cameraName);
			}
		}
		return String.join(",", cameraNameList);
	}

	private List<Long> parseIdList(String idText) {
		if (StringUtils.isBlank(idText)) {
			return java.util.Arrays.asList();
		}
		return Arrays.stream(idText.split(","))
			.map(String::trim)
			.filter(StringUtils::isNotBlank)
			.map(this::parseLong)
			.filter(Objects::nonNull)
			.collect(java.util.stream.Collectors.toList());
	}

	private Long parseLong(String numberText) {
		try {
			return Long.valueOf(numberText);
		} catch (NumberFormatException numberFormatException) {
			return null;
		}
	}
}
