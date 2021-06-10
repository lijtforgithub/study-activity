package com.guowy.workflow.webapp.config.image;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.*;
import org.activiti.image.impl.DefaultProcessDiagramCanvas;
import org.activiti.image.impl.DefaultProcessDiagramGenerator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author LiJingTang
 * @date 2020-05-27 11:49
 */
@Slf4j
@Component
public class CustomProcessDiagramGenerator extends DefaultProcessDiagramGenerator {

    static final String FONT = "宋体";
    /**
     * 一个汉字的宽度
     */
    private static final int WIDTH = 15;

    static ThreadLocal<Set<String>> completedNodeSet = new ThreadLocal<>();
    static ThreadLocal<Set<String>> currentNodeSet = new ThreadLocal<>();
    static ThreadLocal<Set<String>> runNodeSet = new ThreadLocal<>();

    public InputStream generate(BpmnModel bpmnModel,
                                Set<String> completedNodes, Set<String> currentNodes, Set<String> runNodes,
                                Set<String> highLightedFlows, String highLightedNode) {
        Assert.notNull(bpmnModel, "bpmnModel为空");
        if (Objects.isNull(completedNodes)) {
            completedNodes = Collections.emptySet();
        }
        if (Objects.isNull(currentNodes)) {
            currentNodes = Collections.emptySet();
        }
        if (Objects.isNull(runNodes)) {
            runNodes = Collections.emptySet();
        }
        if (Objects.isNull(highLightedFlows)) {
            highLightedFlows = Collections.emptySet();
        }
        List<String> highLightedNodes = new ArrayList<>(1);
        if (StringUtils.isNotBlank(highLightedNode)) {
            highLightedNodes.add(highLightedNode);
        }

        initFlowName(bpmnModel);
        completedNodeSet.set(completedNodes);
        currentNodeSet.set(currentNodes);
        runNodeSet.set(runNodes);
        InputStream input = generateDiagramCanvas(bpmnModel, new ArrayList<>(highLightedFlows), highLightedNodes).generateImage();
        completedNodeSet.remove();
        currentNodeSet.remove();
        runNodeSet.remove();

        return input;
    }

    public static List<SequenceFlow> getSequenceFlows(BpmnModel bpmnModel) {
        return bpmnModel.getProcesses().stream()
                    .flatMap(process -> process.findFlowElementsOfType(SequenceFlow.class).stream())
                    .collect(Collectors.toList());
    }

    private DefaultProcessDiagramCanvas generateDiagramCanvas(BpmnModel bpmnModel,
                                                              List<String> highLightedFlows, List<String> highLightedNodes) {
        prepareBpmnModel(bpmnModel);
        DefaultProcessDiagramCanvas processDiagramCanvas = initDiagramCanvas(bpmnModel);

        // Draw pool shape, if process is participant in collaboration
        for (Pool pool : bpmnModel.getPools()) {
            GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(pool.getId());
            processDiagramCanvas.drawPoolOrLane(pool.getId(), pool.getName(), graphicInfo);
        }

        // Draw lanes
        for (Process process : bpmnModel.getProcesses()) {
            for (Lane lane : process.getLanes()) {
                GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(lane.getId());
                processDiagramCanvas.drawPoolOrLane(lane.getId(), lane.getName(), graphicInfo);
            }
        }

        // Draw activities and their sequence-flows
        for (Process process : bpmnModel.getProcesses()) {
            for (FlowNode flowNode : process.findFlowElementsOfType(FlowNode.class)) {
                drawActivity(processDiagramCanvas, bpmnModel, flowNode, highLightedNodes, highLightedFlows);
            }
        }

        // Draw artifacts
        drawArtifacts(bpmnModel, processDiagramCanvas);

        return processDiagramCanvas;
    }

    private void drawArtifacts(BpmnModel bpmnModel, DefaultProcessDiagramCanvas processDiagramCanvas) {
        for (Process process : bpmnModel.getProcesses()) {
            for (Artifact artifact : process.getArtifacts()) {
                drawArtifact(processDiagramCanvas, bpmnModel, artifact);
            }

            List<SubProcess> subProcesses = process.findFlowElementsOfType(SubProcess.class, true);
            if (subProcesses != null) {
                for (SubProcess subProcess : subProcesses) {
                    for (Artifact subProcessArtifact : subProcess.getArtifacts()) {
                        drawArtifact(processDiagramCanvas, bpmnModel, subProcessArtifact);
                    }
                }
            }
        }
    }

    private static void initFlowName(BpmnModel bpmnModel) {
        getSequenceFlows(bpmnModel).stream().filter(f -> StringUtils.isNotBlank(f.getName()))
                .forEach(sequenceFlow -> {
                    List<GraphicInfo> infoList = bpmnModel.getFlowLocationGraphicInfo(sequenceFlow.getId());

                    double maxWidth = 0;
                    GraphicInfo info = new GraphicInfo();
                    int len = sequenceFlow.getName().length();

                    for (int i = infoList.size() - 1; i > 0; i--) {
                        GraphicInfo current = infoList.get(i);
                        GraphicInfo pre = infoList.get(i - 1);
                        double width = Math.abs(current.getX() - pre.getX());
                        if (width > maxWidth) {
                            maxWidth = width;
                            info.setX(Math.min(current.getX(), pre.getX()) + (width - WIDTH * len) / 2);
                            info.setY((current.getY() + pre.getY()) / 2 + 1);
                            info.setWidth(width);
                            info.setHeight(1);
                            if (i == 1) {
                                info.setX(info.getX() + 3);
                            }
                        }
                    }
                    // 有此坐标 才有流条件的名称
                    bpmnModel.addLabelGraphicInfo(sequenceFlow.getId(), info);
                    log.debug("{} 有条件 {}", sequenceFlow.getId(), JSON.toJSONString(info));
                });
    }

    private static DefaultProcessDiagramCanvas initDiagramCanvas(BpmnModel bpmnModel) {
        DefaultProcessDiagramCanvas diagramCanvas = initProcessDiagramCanvas(bpmnModel, FONT, FONT, FONT);
        try {
            int width = widthField.getInt(diagramCanvas);
            int height = heightField.getInt(diagramCanvas);
            int minX = minxField.getInt(diagramCanvas);
            int minY = minyField.getInt(diagramCanvas);
            return new CustomProcessDiagramCanvas(width, height, minX, minY,
                    FONT, FONT, FONT);

        } catch (IllegalAccessException e) {
            log.error("创建自定义画布失败", e);
            throw new NullPointerException("创建画布失败：" + e.getMessage());
        }
    }

    private static Field widthField;
    private static Field heightField;
    private static Field minxField;
    private static Field minyField;

    static {
        try {
            widthField = DefaultProcessDiagramCanvas.class.getDeclaredField("canvasWidth");
            heightField = DefaultProcessDiagramCanvas.class.getDeclaredField("canvasHeight");
            minxField = DefaultProcessDiagramCanvas.class.getDeclaredField("minX");
            minyField = DefaultProcessDiagramCanvas.class.getDeclaredField("minY");
            widthField.setAccessible(true);
            heightField.setAccessible(true);
            minxField.setAccessible(true);
            minyField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            log.error("获取自定义画布父类字段失败", e);
        }
    }

}
