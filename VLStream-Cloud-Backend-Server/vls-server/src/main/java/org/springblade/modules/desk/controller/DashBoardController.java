package org.springblade.modules.desk.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.support.Kv;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Hidden;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Home page
 *
 * @author zhuangqian
 */
@Hidden
@RestController
@RequestMapping(AppConstant.APPLICATION_DESK_NAME + "/dashboard")
@AllArgsConstructor
@Tag(name = "Home page", description = "Home page")
public class DashBoardController {

	/**
	 * Active users
	 *
	 * @return
	 */
	@GetMapping("/activities")
	@Operation(summary = "Active users", description = "Active users")
	public R activities() {

		List<Map<String, Object>> list = new ArrayList<>();
		Map<String, Object> map1 = new HashMap<>(16);
		map1.put("id", "trend-1");
		map1.put("updatedAt", "2019-01-01");
		map1.put("user", Kv.init().set("name", "Qulili").set("avatar", "https://gw.alipayobjects.com/zos/rmsportal/ThXAXghbEsBCCSDihZxY.png"));
		map1.put("group", Kv.init().set("name", "Premium design team").set("link", "http://github.com/"));
		map1.put("project", Kv.init().set("name", "June iteration").set("link", "http://github.com/"));
		map1.put("template", "Create new project @{project} in @{group}");
		list.add(map1);

		Map<String, Object> map2 = new HashMap<>(16);
		map2.put("id", "trend-2");
		map2.put("updatedAt", "2019-01-01");
		map2.put("user", Kv.init().set("name", "Fu Xiaoxiao").set("avatar", "https://gw.alipayobjects.com/zos/rmsportal/ThXAXghbEsBCCSDihZxY.png"));
		map2.put("group", Kv.init().set("name", "Premium design team").set("link", "http://github.com/"));
		map2.put("project", Kv.init().set("name", "July monthly iteration").set("link", "http://github.com/"));
		map2.put("template", "Create new project @{project} in  @{group}");
		list.add(map2);

		return R.data(list);
	}

	/**
	 * Get message
	 *
	 * @return
	 */
	@GetMapping("/notices")
	@Operation(summary = "Message", description = "Message")
	public R notices() {
		List<Map<String, String>> list = new ArrayList<>();
		Map<String, String> map1 = new HashMap<>(16);
		map1.put("logo", "https://spring.io/img/homepage/icon-spring-framework.svg");
		map1.put("title", "SpringBoot");
		map1.put("description", "Almost all current web projects use the Spring framework, and using Spring inevitably requires configuring a large number of XML configuration files. The emergence of Spring Boot solved this problem; a project does not even need to be deployed to a server and can be run directly, just as Spring Boot says: \"just run\".");
		map1.put("member", "Chill");
		map1.put("href", "http://spring.io/projects/spring-boot");
		list.add(map1);

		Map<String, String> map2 = new HashMap<>(16);
		map2.put("logo", "https://spring.io/img/homepage/icon-spring-cloud.svg");
		map2.put("title", "SpringCloud");
		map2.put("description", "Spring Cloud is a microservices framework built on top of Spring Boot. It provides components needed for microservices development, such as configuration management, service discovery, circuit breakers, intelligent routing, micro-proxy, control bus, global lock, leadership election, distributed sessions, and cluster state management.");
		map2.put("member", "Chill");
		map2.put("href", "http://spring.io/projects/spring-cloud");
		list.add(map2);

		Map<String, String> map3 = new HashMap<>(16);
		map3.put("logo", "https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1546359961068&di=05ff9406e6675ca9a58a525a7e7950b9&imgtype=jpg&src=http%3A%2F%2Fimg0.imgtn.bdimg.com%2Fit%2Fu%3D575314515%2C4268715674%26fm%3D214%26gp%3D0.jpg");
		map3.put("title", "Mybatis");
		map3.put("description", "MyBatis is an outstanding persistence framework that supports customized SQL, stored procedures, and advanced mapping. MyBatis avoids almost all JDBC code, manual parameter setting, and retrieval of results. MyBatis can be configured and mapped from native information using simple XML or annotations, mapping interfaces and Java POJOs (Plain Old Java Objects) into database records.");
		map3.put("member", "Chill");
		map3.put("href", "http://www.mybatis.org/mybatis-3/getting-started.html");
		list.add(map3);

		Map<String, String> map4 = new HashMap<>(16);
		map4.put("logo", "https://gw.alipayobjects.com/zos/rmsportal/kZzEzemZyKLKFsojXItE.png");
		map4.put("title", "React");
		map4.put("description", "React originated as an internal project at Facebook. Because the company was unsatisfied with all existing JavaScript MVC frameworks on the market, they decided to write their own to build the Instagram website. Once built, they found it very useful and open-sourced it in May 2013.");
		map4.put("member", "Chill");
		map4.put("href", "https://reactjs.org/");
		list.add(map4);

		Map<String, String> map5 = new HashMap<>(16);
		map5.put("logo", "https://gw.alipayobjects.com/zos/rmsportal/dURIMkkrRFpPgTuzkwnB.png");
		map5.put("title", "Ant Design");
		map5.put("description", "Through a large number of project practices and summaries, the Ant Financial Experience Technology Department has developed the design language Ant Design. This is not simply design principles, control specifications, and visual dimensions, but is also accompanied by frontend code implementation solutions. In other words, after adopting Ant Design, UI design and frontend interface R&D can be completed synchronously, greatly improving efficiency.");
		map5.put("member", "Chill");
		map5.put("href", "https://ant.design/docs/spec/introduce-cn");
		list.add(map5);

		Map<String, String> map6 = new HashMap<>(16);
		map6.put("logo", "https://gw.alipayobjects.com/zos/rmsportal/sfjbOqnsXXJgNCjCzDBL.png");
		map6.put("title", "Ant Design Pro");
		map6.put("description", "Ant Design Pro is an enterprise-class out-of-the-box mid-and-back-end front-end/design solution. It conforms to Alibaba's philosophy of 'agile front-end + powerful mid-end'.");
		map6.put("member", "Chill");
		map6.put("href", "https://pro.ant.design");
		list.add(map6);

		return R.data(list);
	}

	/**
	 * Get my messages
	 *
	 * @return
	 */
	@GetMapping("/my-notices")
	@Operation(summary = "Message", description = "Message")
	public R myNotices() {
		List<Map<String, String>> list = new ArrayList<>();
		Map<String, String> map1 = new HashMap<>(16);
		map1.put("id", "000000001");
		map1.put("avatar", "https://gw.alipayobjects.com/zos/rmsportal/ThXAXghbEsBCCSDihZxY.png");
		map1.put("title", "You received 14 new weekly reports");
		map1.put("datetime", "2018-08-09");
		map1.put("type", "notification");
		list.add(map1);

		Map<String, String> map2 = new HashMap<>(16);
		map2.put("id", "000000002");
		map2.put("avatar", "https://gw.alipayobjects.com/zos/rmsportal/OKJXDXrmkNshAMvwtvhu.png");
		map2.put("title", "Qu Nini, recommended by you, has passed the third round of interviews");
		map2.put("datetime", "2018-08-08");
		map2.put("type", "notification");
		list.add(map2);


		Map<String, String> map3 = new HashMap<>(16);
		map3.put("id", "000000003");
		map3.put("avatar", "https://gw.alipayobjects.com/zos/rmsportal/fcHMVNCjPOsbUGdEduuv.jpeg");
		map3.put("title", "Qulili commented on you");
		map3.put("description", "Description info description info description info");
		map3.put("datetime", "2018-08-07");
		map3.put("type", "message");
		map3.put("clickClose", "true");
		list.add(map3);


		Map<String, String> map4 = new HashMap<>(16);
		map4.put("id", "000000004");
		map4.put("avatar", "https://gw.alipayobjects.com/zos/rmsportal/fcHMVNCjPOsbUGdEduuv.jpeg");
		map4.put("title", "Zhu Pianyou replied to you");
		map4.put("description", "This template is used to remind who interacted with you, with \"who's\" avatar placed on the left");
		map4.put("type", "message");
		map4.put("datetime", "2018-08-07");
		map4.put("clickClose", "true");
		list.add(map4);


		Map<String, String> map5 = new HashMap<>(16);
		map5.put("id", "000000005");
		map5.put("title", "Task name");
		map5.put("description", "Task needs to start before 2018-01-12 20:00");
		map5.put("extra", "Not Started");
		map5.put("status", "todo");
		map5.put("type", "event");
		list.add(map5);

		return R.data(list);
	}
}
