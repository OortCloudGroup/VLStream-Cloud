package org.springblade.vlstream.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.core.tool.utils.SpringUtil;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.vlstream.mapper.VlsAlgorithmMapper;
import org.springblade.vlstream.mapper.VlsDeviceInfoMapper;
import org.springblade.vlstream.pojo.entity.Algorithm;
import org.springblade.vlstream.pojo.entity.DeviceInfo;
import org.springblade.vlstream.pojo.entity.SceneGovernance;
import org.springblade.vlstream.pojo.vo.SceneGovernanceVO;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Scene governance table wrapper class, returning fields required for view layer
 *
 * @author Oort
 * @since 2025-12-23
 */
public class VlsSceneGovernanceWrapper extends BaseEntityWrapper<SceneGovernance, SceneGovernanceVO> {

	private static final VlsAlgorithmMapper vlsAlgorithmMapper = SpringUtil.getBean(VlsAlgorithmMapper.class);
	private static final VlsDeviceInfoMapper vlsDeviceInfoMapper = SpringUtil.getBean(VlsDeviceInfoMapper.class);

	public static VlsSceneGovernanceWrapper build() {
		return new VlsSceneGovernanceWrapper();
	}

	@Override
	public SceneGovernanceVO entityVO(SceneGovernance vlsSceneGovernance) {
		if (vlsSceneGovernance == null) {
			return null;
		}
		SceneGovernanceVO vlsSceneGovernanceVO = BeanUtil.copyProperties(vlsSceneGovernance, SceneGovernanceVO.class);
		if (vlsSceneGovernanceVO == null) {
			return null;
		}

		//User createUser = UserCache.getUser(vlsSceneGovernance.getCreateUser());
		//User updateUser = UserCache.getUser(vlsSceneGovernance.getUpdateUser());
		//vlsSceneGovernanceVO.setCreateUserName(createUser.getName());
		//vlsSceneGovernanceVO.setUpdateUserName(updateUser.getName());

		vlsSceneGovernanceVO.setCamerasName(resolveDeviceNames(vlsSceneGovernance.getCameras()));
		return vlsSceneGovernanceVO;
	}

	private static String resolveAlgorithmNames(String algorithmIds) {
		List<Long> algorithmIdList = splitToIds(algorithmIds);
		if (algorithmIdList.isEmpty()) {
			return "";
		}
		List<Long> uniqueAlgorithmIds = algorithmIdList.stream().distinct().toList();
		List<Algorithm> algorithms = vlsAlgorithmMapper.selectBatchIds(uniqueAlgorithmIds);
		if (algorithms == null || algorithms.isEmpty()) {
			return "";
		}
		Map<Long, String> algorithmNameMap = algorithms.stream()
			.filter(Objects::nonNull)
			.collect(Collectors.toMap(Algorithm::getId, Algorithm::getName, (left, right) -> left));
		return algorithmIdList.stream()
			.map(algorithmNameMap::get)
			.filter(StringUtil::isNotBlank)
			.collect(Collectors.joining(","));
	}

	private static String resolveDeviceNames(String cameraIds) {
		List<Long> cameraIdList = splitToIds(cameraIds);
		if (cameraIdList.isEmpty()) {
			return "";
		}
		List<Long> uniqueCameraIds = cameraIdList.stream().distinct().toList();
		List<DeviceInfo> deviceInfos = vlsDeviceInfoMapper.selectBatchIds(uniqueCameraIds);
		if (deviceInfos == null || deviceInfos.isEmpty()) {
			return "";
		}
		Map<Long, String> deviceNameMap = deviceInfos.stream()
			.filter(Objects::nonNull)
			.collect(Collectors.toMap(DeviceInfo::getId, DeviceInfo::getDeviceName, (left, right) -> left));
		return cameraIdList.stream()
			.map(deviceNameMap::get)
			.filter(StringUtil::isNotBlank)
			.collect(Collectors.joining(","));
	}

	private static List<Long> splitToIds(String rawIds) {
		if (StringUtil.isBlank(rawIds)) {
			return List.of();
		}
		return Arrays.stream(rawIds.split(","))
			.map(String::trim)
			.filter(StringUtil::isNotBlank)
			.map(VlsSceneGovernanceWrapper::parseId)
			.filter(Objects::nonNull)
			.toList();
	}

	private static Long parseId(String rawId) {
		try {
			return Long.valueOf(rawId);
		} catch (NumberFormatException numberFormatException) {
			return null;
		}
	}

}
