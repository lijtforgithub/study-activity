package com.guowy.workflow.webapp.web;

import com.guowy.cloud.common.util.JsonResult;
import com.guowy.workflow.webapp.biz.CommonBiz;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author LiJingTang
 * @date 2020-05-14 10:09
 */
@Api(value = "基础数据查询接口", tags = "基础数据查询接口")
@RestController
@RequestMapping("/common")
public class CommonController {

    /**
     * 绘图页面元素
     */
    private static final String FILE_NAME = "stencil-set.json";

    @Autowired
    private CommonBiz commonBiz;

    @ApiOperation("查询模型绘画所需元素")
    @GetMapping("/stencil-set")
    public String stencilSets() throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(FILE_NAME)) {
            Assert.notNull(inputStream, "文件不存在");
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        }
    }

    @ApiOperation("查询注册中心所有应用，用作分类")
    @GetMapping("/apps")
    public JsonResult<List<String>> findAllApp() {
        return commonBiz.findAllApp();
    }

    @ApiOperation("查询所有模型KEY")
    @GetMapping("/keys")
    public JsonResult<List<String>> findAllKey() {
        return commonBiz.findAllKey();
    }

}
