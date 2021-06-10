package com.guowy.workflow.webapp.util;

import com.github.pagehelper.Page;
import com.guowy.workflow.dto.BaseQueryDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

/**
 * @author LiJingTang
 * @version 2018年4月22日 上午10:27:07
 */
@Slf4j
public class PageUtils {

	private static final int PAGE_NUM = 1;
	private static final int PAGE_SIZE = 10;

	private PageUtils() {}

	public static <E> Page<E> newPage(BaseQueryDTO queryDTO, long total) {
		Page<E> page = new Page<>(ObjectUtils.defaultIfNull(queryDTO.getPageNum(), PAGE_NUM),
				ObjectUtils.defaultIfNull(queryDTO.getPageSize(), PAGE_SIZE),
				false);
		page.setReasonable(true);
		page.setTotal(total);

		return page;
	}

}
