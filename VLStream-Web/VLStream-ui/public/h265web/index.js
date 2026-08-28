/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

/********************************************************* 
 * LICENSE: LICENSE-Free_CN.MD
 * 
 * Author: Numberwolf - ChangYanlong
 * QQ: 531365872
 * QQ Group:925466059
 * Wechat: numberwolf11
 * Discord: numberwolf#8694
 * E-Mail: porschegt23@foxmail.com
 * Github: https://github.com/numberwolf/h265web.js
 * 
 * : (Numberwolf)( )
 * QQ: 531365872
 * QQ : 531365872
 * : numberwolf11
 * Discord: numberwolf#8694
 * : porschegt23@foxmail.com
 * : https://www.jianshu.com/u/9c09c1e00fd1
 * Github: https://github.com/numberwolf/h265web.js
 * 
 **********************************************************/
require('./h265webjs-v20221106');
export default class h265webjs {
	static createPlayer(videoURL, config) {
		return window.new265webjs(videoURL, config);
	}

	static clear() {
		global.STATICE_MEM_playerCount = -1;
		global.STATICE_MEM_playerIndexPtr = 0;
    }
}
