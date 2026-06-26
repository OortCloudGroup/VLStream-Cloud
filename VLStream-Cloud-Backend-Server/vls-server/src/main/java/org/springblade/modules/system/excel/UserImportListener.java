package org.springblade.modules.system.excel;

import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.event.AnalysisEventListener;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.springblade.modules.system.service.IUserService;

import java.util.ArrayList;
import java.util.List;

/**
 * UserImportListener
 *
 * @author Chill
 */
@Data
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserImportListener extends AnalysisEventListener<UserExcel> {

	/**
	 * Save to database every 3000 records by default
	 */
	private int batchCount = 3000;
	/**
	 * Cached data list
	 */
	private List<UserExcel> list = new ArrayList<>();
	/**
	 * User service
	 */
	private final IUserService userService;

	@Override
	public void invoke(UserExcel data, AnalysisContext context) {
		list.add(data);
		// Once BATCH_COUNT is reached, call importer method to persist data, preventing tens of thousands of data items in memory which easily causes OOM
		if (list.size() >= batchCount) {
			// Call importer method
			userService.importUser(list);
			// Storage completed cleanup list
			list.clear();
		}
	}

	@Override
	public void doAfterAllAnalysed(AnalysisContext analysisContext) {
		// Call importer method
		userService.importUser(list);
		// Storage completed cleanup list
		list.clear();
	}

}
