package com.guowy.workflow.webapp.util;

import org.apache.commons.lang3.StringUtils;

/**
 * 处理查询参数
 * 
 * @author LiJingTang
 * @version 2017年2月13日 下午3:27:56
 */
public class ParamUtils {
	
	private ParamUtils() {}

	private static final String WILDCARD = "%";
	private static final String PLACEHOLDER = "_";

	/**
	 * 在查询参数前面加通配符%
	 * 
	 * <pre>
	 * ParamUtils.prefixLike(null) = null 
	 * ParamUtils.prefixLike(" ") = null
	 * ParamUtils.prefixLike(" abc ") = %abc
	 * <pre>
	 * 
	 * @param param 参数
	 * @return 处理后的参数
	 */
	public static String prefixLike(String param) {
		return StringUtils.isNotBlank(param) ? WILDCARD + param.trim() : null;
	}

	/**
	 * 在查询参数后面加通配符%
	 * 
	 * <pre>
	 * ParamUtils.prefixLike(null) = null 
	 * ParamUtils.prefixLike(" ") = null
	 * ParamUtils.prefixLike(" abc ") = abc%
	 * <pre>
	 * 
	 * @param param 参数
	 * @return 处理后的参数
	 */
	public static String suffixLike(String param) {
		return StringUtils.isNotBlank(param) ? param.trim() + WILDCARD : null;
	}

	/**
	 * 在查询参数两端加通配符%
	 * 
	 * <pre>
	 * ParamUtils.prefixLike(null) = null 
	 * ParamUtils.prefixLike(" ") = null
	 * ParamUtils.prefixLike(" abc ") = %abc%
	 * <pre>
	 * 
	 * @param param 参数
	 * @return 处理后的参数
	 */
	public static String bothLike(String param) {
		return StringUtils.isNotBlank(param) ? WILDCARD + param.trim() + WILDCARD : null;
	}

	/**
	 * 在查询参数前面加占位符_
	 * 
	 * <pre>
	 * ParamUtils.prefixPlaceholder(null, 2) = null
	 * ParamUtils.prefixPlaceholder(" ", 2) = null
	 * ParamUtils.prefixPlaceholder(" abc ", 2) = __abc
	 * <pre>
	 * 
	 * @param param 参数
	 * @return 处理后的参数
	 */
	public static String prefixPlaceholder(String param, int length) {
		return StringUtils.isNotBlank(param) ? getPlaceholder(length).append(param.trim()).toString() : null;
	}

	/**
	 * 在查询参数后面加占位符_
	 * 
	 * <pre>
	 * ParamUtils.prefixPlaceholder(null, 3) = null
	 * ParamUtils.prefixPlaceholder(" ", 3) = null
	 * ParamUtils.prefixPlaceholder(" abc ", 3) = abc___
	 * <pre>
	 * 
	 * @param param 参数
	 * @return 处理后的参数
	 */
	public static String suffixPlaceholder(String param, int length) {
		return StringUtils.isNotBlank(param) ? getPlaceholder(length).insert(0, param.trim()).toString() : null;
	}

	/**
	 * 在查询参数两端加占位符_
	 * 
	 * <pre>
	 * ParamUtils.prefixPlaceholder(null, 2, 3) = null
	 * ParamUtils.prefixPlaceholder(" ", 2, 3) = null
	 * ParamUtils.prefixPlaceholder(" abc ", 2, 3) = __abc___
	 * <pre>
	 * 
	 * @param param 参数
	 * @param prefix 前面占位符个数
	 * @param suffix 后面占位符个数
	 * @return 处理后的参数
	 */
	public static String bothPlaceholder(String param, int prefix, int suffix) {
		return StringUtils.isNotBlank(param) ? getPlaceholder(prefix).append(param.trim()).append(getPlaceholder(suffix)).toString() : null;
	}

	private static StringBuilder getPlaceholder(int length) {
		StringBuilder sbulider = new StringBuilder();

		for (int i = 0; i < length; i++) {
			sbulider.append(PLACEHOLDER);
		}

		return sbulider;
	}

}
