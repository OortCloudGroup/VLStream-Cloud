package com.ruoyi.vlstream.test.vlstream.wrapper;

import org.apache.commons.lang3.StringUtils;
import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.VideoRecord;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.VideoRecordVO;

import java.util.Objects;

/**
 * 视频录制记录表 包装类,返回视图层所需的字段
 *
 * @author Oort
 * @since 2025-12-25
 */
public class VlsVideoRecordWrapper extends BaseEntityWrapper<VideoRecord, VideoRecordVO>  {

	public static VlsVideoRecordWrapper build() {
		return new VlsVideoRecordWrapper();
 	}

	@Override
	public VideoRecordVO entityVO(VideoRecord vlsVideoRecord) {
		VideoRecordVO vlsVideoRecordVO = Objects.requireNonNull(BeanUtil.copyProperties(vlsVideoRecord, VideoRecordVO.class));
		if (vlsVideoRecordVO.getId() != null && StringUtils.isBlank(vlsVideoRecordVO.getUrl())) {
			vlsVideoRecordVO.setUrl("/vlsVideoRecord/stream/" + vlsVideoRecordVO.getId());
		}

		//User createUser = UserCache.getUser(vlsVideoRecord.getCreateUser());
		//User updateUser = UserCache.getUser(vlsVideoRecord.getUpdateUser());
		//vlsVideoRecordVO.setCreateUserName(createUser.getName());
		//vlsVideoRecordVO.setUpdateUserName(updateUser.getName());

		return vlsVideoRecordVO;
	}

}
