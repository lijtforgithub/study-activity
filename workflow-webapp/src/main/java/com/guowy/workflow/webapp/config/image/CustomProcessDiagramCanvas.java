package com.guowy.workflow.webapp.config.image;

import lombok.extern.slf4j.Slf4j;
import org.activiti.bpmn.model.AssociationDirection;
import org.activiti.bpmn.model.GraphicInfo;
import org.activiti.image.impl.DefaultProcessDiagramCanvas;
import org.activiti.image.impl.icon.IconType;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Objects;
import java.util.function.Consumer;

import static com.guowy.workflow.webapp.config.image.CustomProcessDiagramGenerator.FONT;

/**
 * @author LiJingTang
 * @date 2020-05-27 11:55
 */
@Slf4j
@SuppressWarnings("squid:S2184")
public class CustomProcessDiagramCanvas extends DefaultProcessDiagramCanvas {

    private static final String ASSOCIATION = "association";

    /**
     * 已完成节点颜色
     */
    static final Color COMPLETED_COLOR = new Color(40, 180, 99);
    /**
     * 可以执行的节点颜色
     */
    private static final Color RUN_COLOR = new Color(171, 235, 198);
    /**
     * 执行一部分的节点颜色
     */
    private static final Color CURRENT_COLOR = new Color(93, 173, 226);

    public CustomProcessDiagramCanvas(int width, int height, int minX, int minY,
                                      String activityFontName, String labelFontName, String annotationFontName) {
        super(width, height, minX, minY, activityFontName, labelFontName, annotationFontName);
        g.setFont(new Font(activityFontName, Font.PLAIN, 12));
    }

    @Override
    @SuppressWarnings("squid:S2696")
    public void initialize() {
        super.initialize();
        // 覆盖默认流程线条件字大小
        LABEL_FONT = new Font(FONT, Font.ITALIC, 11);
    }

    @Override
    public void drawConnection(int[] xPoints, int[] yPoints, boolean conditional, boolean isDefault,
                               String connectionType, AssociationDirection associationDirection, boolean highLighted) {
        Paint originalPaint = g.getPaint();
        Stroke originalStroke = g.getStroke();
        g.setPaint(CONNECTION_COLOR);

        if (ASSOCIATION.equals(connectionType)) {
            g.setStroke(ASSOCIATION_STROKE);
        } else if (highLighted) {
            // 完成色代替高亮色
            g.setPaint(COMPLETED_COLOR);
            g.setStroke(HIGHLIGHT_FLOW_STROKE);
        }

        for (int i = 1; i < xPoints.length; i++) {
            int sourceX = xPoints[i - 1];
            int sourceY = yPoints[i - 1];
            int targetX = xPoints[i];
            int targetY = yPoints[i];
            Line2D.Double line = new Line2D.Double(sourceX, sourceY, targetX, targetY);
            g.draw(line);
        }

        if (isDefault) {
            Line2D.Double line = new Line2D.Double(xPoints[0], yPoints[0], xPoints[1], yPoints[1]);
            drawDefaultSequenceFlowIndicator(line);
        }
        if (conditional) {
            Line2D.Double line = new Line2D.Double(xPoints[0], yPoints[0], xPoints[1], yPoints[1]);
            drawConditionalSequenceFlowIndicator(line);
        }
        if (associationDirection.equals(AssociationDirection.ONE) || associationDirection.equals(AssociationDirection.BOTH)) {
            Line2D.Double line = new Line2D.Double(xPoints[xPoints.length - 2], yPoints[xPoints.length - 2],
                    xPoints[xPoints.length - 1], yPoints[xPoints.length - 1]);
            drawArrowHead(line);
        }
        if (associationDirection.equals(AssociationDirection.BOTH)) {
            Line2D.Double line = new Line2D.Double(xPoints[1], yPoints[1], xPoints[0], yPoints[0]);
            drawArrowHead(line);
        }

        g.setPaint(originalPaint);
        g.setStroke(originalStroke);
    }

    @Override
    protected void drawTask(String id, String name, GraphicInfo graphicInfo, boolean thickBorder) {
        Paint originalPaint = g.getPaint();
        int x = (int) graphicInfo.getX();
        int y = (int) graphicInfo.getY();
        int width = (int) graphicInfo.getWidth();
        int height = (int) graphicInfo.getHeight();

        // Create a new gradient paint for every task box, gradient depends on x and y and is not relative
        g.setPaint(TASK_BOX_COLOR);
        // 选色
        Color color = getColor(id);
        if (Objects.nonNull(color)) {
            g.setPaint(color);
        }

        int arcR = 6;
        if (thickBorder) {
            arcR = 3;
        }

        // shape
        RoundRectangle2D rect = new RoundRectangle2D.Double(x, y, width, height, arcR, arcR);
        g.fill(rect);
        g.setPaint(TASK_BORDER_COLOR);

        if (thickBorder) {
            Stroke originalStroke = g.getStroke();
            g.setStroke(THICK_TASK_BORDER_STROKE);
            g.draw(rect);
            g.setStroke(originalStroke);
        } else {
            g.draw(rect);
        }

        g.setPaint(originalPaint);
        // text
        if (name != null && name.length() > 0) {
            int boxWidth = width - (2 * TEXT_PADDING);
            // 解决任务名称不显示问题
            int boxHeight = height - 16 - ICON_PADDING - ICON_PADDING - 2 - 2;
            int boxX = x + width / 2 - boxWidth / 2;
            // 调整任务名称上下坐标
            int boxY = y + height / 2 - boxHeight / 2 + ICON_PADDING - 2 - 2;

            drawMultilineCentredText(name, boxX, boxY, boxWidth, boxHeight);
        }

        // set element's id
        g.setCurrentGroupId(id);
    }

    @Override
    public void drawHighLight(int x, int y, int width, int height) {
        Paint originalPaint = g.getPaint();
        Stroke originalStroke = g.getStroke();
        g.setPaint(HIGHLIGHT_COLOR);
        g.setStroke(THICK_TASK_BORDER_STROKE);

        // 参照任务
        int arcR = 6;
        RoundRectangle2D rect = new RoundRectangle2D.Double(x, y, width, height, arcR, arcR);
        g.draw(rect);

        g.setPaint(originalPaint);
        g.setStroke(originalStroke);
    }

    @Override
    public void drawStartEvent(String id, GraphicInfo graphicInfo, IconType icon) {
        DrawEventConsumer consumer = (circle, paint, stroke) -> {
            g.draw(circle);
            g.setPaint(paint);

            // calculate coordinates to center image
            if (icon != null) {
                int imageX = (int) Math.round(graphicInfo.getX() + (graphicInfo.getWidth() / 2) - (icon.getWidth() / 2));
                int imageY = (int) Math.round(graphicInfo.getY() + (graphicInfo.getHeight() / 2) - (icon.getHeight() / 2));

                icon.drawIcon(imageX, imageY, ICON_PADDING, g);
            }
        };

        drawEvent(id, graphicInfo, consumer);
    }

    private void drawEvent(String id, GraphicInfo graphicInfo, DrawEventConsumer consumer) {
        Paint originalPaint = g.getPaint();
        Stroke originalStroke = g.getStroke();
        g.setPaint(EVENT_COLOR);
        Ellipse2D circle = new Ellipse2D.Double(graphicInfo.getX(), graphicInfo.getY(), graphicInfo.getWidth(), graphicInfo.getHeight());
        g.fill(circle);
        g.setPaint(EVENT_BORDER_COLOR);
        // 选色
        Color color = getColor(id);
        if (Objects.nonNull(color)) {
            g.setPaint(color);
        }

        consumer.accept(circle, originalPaint, originalStroke);

        g.setCurrentGroupId(id);
    }

    @Override
    public void drawNoneEndEvent(String id, String name, GraphicInfo graphicInfo) {
        DrawEventConsumer consumer = (circle, paint, stroke) -> {
            g.setStroke(END_EVENT_STROKE);
            g.draw(circle);
            g.setStroke(stroke);
            g.setPaint(paint);
        };

        drawEvent(id, graphicInfo, consumer);
        // set element's id
        drawLabel(name, graphicInfo);
    }

    @Override
    public void drawParallelGateway(String id, GraphicInfo graphicInfo) {
        Consumer<GraphicInfo> consumer = info -> {
            int x = (int) info.getX();
            int y = (int) info.getY();
            int width = (int) info.getWidth();
            int height = (int) info.getHeight();

            // plus inside rhombus
            Stroke originalStroke = g.getStroke();
            g.setStroke(GATEWAY_TYPE_STROKE);
            // horizontal
            Line2D.Double line = new Line2D.Double(x + 10, y + height / 2, x + width - 10, y + height / 2);
            g.draw(line);
            // vertical
            line = new Line2D.Double(x + width / 2, y + height - 10, x + width / 2, y + 10);
            g.draw(line);
            g.setStroke(originalStroke);
        };

        drawGateway(id, graphicInfo, consumer);
    }

    @Override
    public void drawExclusiveGateway(String id, GraphicInfo graphicInfo) {
        Consumer<GraphicInfo> consumer = info -> {
            int x = (int) info.getX();
            int y = (int) info.getY();
            int width = (int) info.getWidth();
            int height = (int) info.getHeight();

            int quarterWidth = width / 4;
            int quarterHeight = height / 4;

            // X inside rhombus
            Stroke originalStroke = g.getStroke();
            g.setStroke(GATEWAY_TYPE_STROKE);
            Line2D.Double line = new Line2D.Double(x + quarterWidth + 3, y + quarterHeight + 3, x + 3 * quarterWidth - 3, y + 3 * quarterHeight - 3);
            g.draw(line);
            line = new Line2D.Double(x + quarterWidth + 3, y + 3 * quarterHeight - 3, x + 3 * quarterWidth - 3, y + quarterHeight + 3);
            g.draw(line);
            g.setStroke(originalStroke);
        };

        drawGateway(id, graphicInfo, consumer);
    }

    @Override
    public void drawInclusiveGateway(String id, GraphicInfo graphicInfo) {
        Consumer<GraphicInfo> consumer = info -> {
            int x = (int) info.getX();
            int y = (int) info.getY();
            int width = (int) info.getWidth();
            int height = (int) info.getHeight();

            int diameter = width / 2;

            // circle inside rhombus
            Stroke originalStroke = g.getStroke();
            g.setStroke(GATEWAY_TYPE_STROKE);
            Ellipse2D.Double circle = new Ellipse2D.Double(((width - diameter) / 2) + x, ((height - diameter) / 2) + y, diameter, diameter);
            g.draw(circle);
            g.setStroke(originalStroke);
        };

        drawGateway(id, graphicInfo, consumer);
    }

    private void drawGateway(String id, GraphicInfo graphicInfo, Consumer<GraphicInfo> consumer) {
        Paint originalPaint = g.getPaint();
        // 选色
        Color color = getColor(id);
        if (Objects.nonNull(color)) {
            g.setPaint(color);
        }
        // rhombus
        drawGateway(graphicInfo);
        consumer.accept(graphicInfo);
        // set element's id
        g.setCurrentGroupId(id);
        g.setPaint(originalPaint);
    }

    @Override
    public void drawMultiInstanceMarker(boolean sequential, int x, int y, int width, int height) {
        // 修改多实例线长短
        int rectangleWidth = MARKER_WIDTH - 4;
        int rectangleHeight = MARKER_WIDTH - 6;
        int lineX = x + (width - rectangleWidth) / 2;
        int lineY = y + height - rectangleHeight - 3;

        Stroke originalStroke = g.getStroke();
        g.setStroke(MULTI_INSTANCE_STROKE);

        if (sequential) {
            g.draw(new Line2D.Double(lineX, lineY, lineX + rectangleWidth, lineY));
            g.draw(new Line2D.Double(lineX, lineY + rectangleHeight / 2, lineX + rectangleWidth, lineY + rectangleHeight / 2));
            g.draw(new Line2D.Double(lineX, lineY + rectangleHeight, lineX + rectangleWidth, lineY + rectangleHeight));
        } else {
            g.draw(new Line2D.Double(lineX, lineY, lineX, lineY + rectangleHeight));
            g.draw(new Line2D.Double(lineX + rectangleWidth / 2, lineY, lineX + rectangleWidth / 2, lineY + rectangleHeight));
            g.draw(new Line2D.Double(lineX + rectangleWidth, lineY, lineX + rectangleWidth, lineY + rectangleHeight));
        }

        g.setStroke(originalStroke);
    }

    private static Color getColor(String id) {
        if (CustomProcessDiagramGenerator.completedNodeSet.get().contains(id)) {
            return COMPLETED_COLOR;
        }
        if (CustomProcessDiagramGenerator.currentNodeSet.get().contains(id)) {
            return CURRENT_COLOR;
        }
        if (CustomProcessDiagramGenerator.runNodeSet.get().contains(id)) {
            return RUN_COLOR;
        }

        return null;
    }

}
