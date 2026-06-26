package org.springblade.vlstream.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.vlstream.mapper.VlsAlgorithmMapper;
import org.springblade.vlstream.mapper.VlsDeviceInfoMapper;
import org.springblade.vlstream.mapper.VlsMobileSceneGovernanceMapper;
import org.springblade.vlstream.mapper.VlsMobileSceneGovernanceSubTaskMapper;
import org.springblade.vlstream.pojo.entity.Algorithm;
import org.springblade.vlstream.pojo.entity.DeviceInfo;
import org.springblade.vlstream.pojo.entity.MobileSceneGovernance;
import org.springblade.vlstream.pojo.entity.MobileSceneGovernanceSubTask;
import org.springblade.vlstream.pojo.vo.MobileSceneGovernanceLoopVO;
import org.springblade.vlstream.service.IVlsMobileSceneGovernanceService;
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
 * Mobile scene governance main task table Service implementation class
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
			throw new ServiceException("Request parameters cannot be empty");
		}
		if (StringUtils.isBlank(mobileSceneGovernance.getName())) {
			throw new ServiceException("Governance name cannot be empty");
		}
		if (StringUtils.isBlank(mobileSceneGovernance.getLocationIds())) {
			throw new ServiceException("Analysis area cannot be empty");
		}
		if (StringUtils.isBlank(mobileSceneGovernance.getAlgorithmIds())) {
			throw new ServiceException("AI algorithm cannot be empty");
		}
		if (StringUtils.isBlank(mobileSceneGovernance.getCameraIds())) {
			throw new ServiceException("Camera cannot be empty");
		}
	}

	private void validateLoop(MobileSceneGovernance mobileSceneGovernance) {
		if (StringUtils.isBlank(mobileSceneGovernance.getCycleType())) {
			throw new ServiceException("Loop cycle type cannot be empty");
		}
		if (mobileSceneGovernance.getStartTime() == null || mobileSceneGovernance.getEndTime() == null) {
			throw new ServiceException("Start time and end time cannot be empty");
		}
		if (mobileSceneGovernance.getEndTime().isBefore(mobileSceneGovernance.getStartTime())) {
			throw new ServiceException("End time cannot be earlier than start time");
		}
		if (StringUtils.isBlank(mobileSceneGovernance.getTriggerTimes())) {
			throw new ServiceException("Trigger time cannot be empty");
		}
		if ("everyOtherDay".equals(mobileSceneGovernance.getCycleType())) {
			Integer intervalDays = mobileSceneGovernance.getIntervalDays();
			if (intervalDays == null || intervalDays <= 0) {
				throw new ServiceException("Interval days in alternate days mode must be greater than 0");
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
		throw new ServiceException("Unsupported loop cycle type");
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
			throw new ServiceException("Trigger time cannot be empty");
		}
		return triggerTimeList;
	}

	private Set<Integer> parseIntSet(String valueText) {
		if (StringUtils.isBlank(valueText)) {
			return new java.util.HashSet<>();
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
		List<Algorithm> algorithmList = vlsAlgorithmMapper.selectBatchIds(algorithmIdList.stream().distinct().toList());
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
		List<DeviceInfo> deviceInfoList = vlsDeviceInfoMapper.selectBatchIds(cameraIdList.stream().distinct().toList());
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
			return List.of();
		}
		return Arrays.stream(idText.split(","))
			.map(String::trim)
			.filter(StringUtils::isNotBlank)
			.map(this::parseLong)
			.filter(Objects::nonNull)
			.toList();
	}

	private Long parseLong(String numberText) {
		try {
			return Long.valueOf(numberText);
		} catch (NumberFormatException numberFormatException) {
			return null;
		}
	}
}
