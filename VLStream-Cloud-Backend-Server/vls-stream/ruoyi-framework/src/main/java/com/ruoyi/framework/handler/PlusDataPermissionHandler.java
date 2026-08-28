/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.framework.handler;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ObjectUtil;
import com.ruoyi.common.annotation.DataColumn;
import com.ruoyi.common.annotation.DataPermission;
import com.ruoyi.common.core.domain.dto.RoleDTO;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.enums.DataScopeType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.DataPermissionHelper;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.StreamUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.spring.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.expression.BeanResolver;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * data
 *
 * @author Lion Li
 * @version 3.5.0
 */
@Slf4j
public class PlusDataPermissionHandler {

    /**
     * method ( ) and
     */
    private final Map<String, DataPermission> dataPermissionCacheMap = new ConcurrentHashMap<>();

    /**
     * method
     */
    private final Set<String> invalidCacheSet = new ConcurrentHashSet<>();

    /**
     * spel Parse
     */
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParserContext parserContext = new TemplateParserContext();
    /**
     * beanParse Process spel in bean
     */
    private final BeanResolver beanResolver = new BeanFactoryResolver(SpringUtils.getBeanFactory());


    public Expression getSqlSegment(Expression where, String mappedStatementId, boolean isSelect) {
        DataColumn[] dataColumns = findAnnotation(mappedStatementId);
        if (ArrayUtil.isEmpty(dataColumns)) {
            invalidCacheSet.add(mappedStatementId);
            return where;
        }
        LoginUser currentUser = DataPermissionHelper.getVariable("user");
        if (ObjectUtil.isNull(currentUser)) {
            currentUser = LoginHelper.getLoginUser();
            DataPermissionHelper.setVariable("user", currentUser);
        }
        // if is administrator, data
        if (LoginHelper.isAdmin()) {
            return where;
        }
        String dataFilterSql = buildDataFilter(dataColumns, isSelect);
        if (StringUtils.isBlank(dataFilterSql)) {
            return where;
        }
        try {
            Expression expression = CCJSqlParserUtil.parseExpression(dataFilterSql);
            // data and
            Parenthesis parenthesis = new Parenthesis(expression);
            if (ObjectUtil.isNotNull(where)) {
                return new AndExpression(where, parenthesis);
            } else {
                return parenthesis;
            }
        } catch (JSQLParserException e) {
            throw new ServiceException("数据权限解析异常 => " + e.getMessage());
        }
    }

    /**
     * data sql
     */
    private String buildDataFilter(DataColumn[] dataColumns, boolean isSelect) {
        // new Delete all
        String joinStr = isSelect ? " OR " : " AND ";
        LoginUser user = DataPermissionHelper.getVariable("user");
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setBeanResolver(beanResolver);
        DataPermissionHelper.getContext().forEach(context::setVariable);
        Set<String> conditions = new HashSet<>();
        for (RoleDTO role : user.getRoles()) {
            user.setRoleId(role.getRoleId());
            // Get role
            DataScopeType type = DataScopeType.findCode(role.getDataScope());
            if (ObjectUtil.isNull(type)) {
                throw new ServiceException("角色数据范围异常 => " + role.getDataScope());
            }
            // full data
            if (type == DataScopeType.ALL) {
                return "";
            }
            boolean isSuccess = false;
            for (DataColumn dataColumn : dataColumns) {
                if (dataColumn.key().length != dataColumn.value().length) {
                    throw new ServiceException("角色数据范围异常 => key与value长度不匹配");
                }
                // key variable Process
                if (!StringUtils.containsAny(type.getSqlTemplate(),
                    Arrays.stream(dataColumn.key()).map(key -> "#" + key).toArray(String[]::new)
                )) {
                    continue;
                }
                // Set variable key to variable value to variable value
                for (int i = 0; i < dataColumn.key().length; i++) {
                    context.setVariable(dataColumn.key()[i], dataColumn.value()[i]);
                }

                // Parse sql fill
                String sql = parser.parseExpression(type.getSqlTemplate(), parserContext).getValue(context, String.class);
                conditions.add(joinStr + sql);
                isSuccess = true;
            }
            // not Process successfully fill
            if (!isSuccess && StringUtils.isNotBlank(type.getElseSql())) {
                conditions.add(joinStr + type.getElseSql());
            }
        }

        if (CollUtil.isNotEmpty(conditions)) {
            String sql = StreamUtils.join(conditions, Function.identity(), "");
            return sql.substring(joinStr.length());
        }
        return "";
    }

    private DataColumn[] findAnnotation(String mappedStatementId) {
        StringBuilder sb = new StringBuilder(mappedStatementId);
        int index = sb.lastIndexOf(".");
        String clazzName = sb.substring(0, index);
        String methodName = sb.substring(index + 1, sb.length());
        Class<?> clazz = ClassUtil.loadClass(clazzName);
        List<Method> methods = Arrays.stream(ClassUtil.getDeclaredMethods(clazz))
            .filter(method -> method.getName().equals(methodName)).collect(Collectors.toList());
        DataPermission dataPermission;
        // Get method
        for (Method method : methods) {
            dataPermission = dataPermissionCacheMap.get(mappedStatementId);
            if (ObjectUtil.isNotNull(dataPermission)) {
                return dataPermission.value();
            }
            if (AnnotationUtil.hasAnnotation(method, DataPermission.class)) {
                dataPermission = AnnotationUtil.getAnnotation(method, DataPermission.class);
                dataPermissionCacheMap.put(mappedStatementId, dataPermission);
                return dataPermission.value();
            }
        }
        dataPermission = dataPermissionCacheMap.get(clazz.getName());
        if (ObjectUtil.isNotNull(dataPermission)) {
            return dataPermission.value();
        }
        // Get
        if (AnnotationUtil.hasAnnotation(clazz, DataPermission.class)) {
            dataPermission = AnnotationUtil.getAnnotation(clazz, DataPermission.class);
            dataPermissionCacheMap.put(clazz.getName(), dataPermission);
            return dataPermission.value();
        }
        return null;
    }

    /**
     * whether to method data
     */
    public boolean isInvalid(String mappedStatementId) {
        return invalidCacheSet.contains(mappedStatementId);
    }
}
