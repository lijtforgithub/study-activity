package com.guowy.workflow.webapp.config.image;

import java.awt.*;
import java.awt.geom.Ellipse2D;

/**
 * @author LiJingTang
 * @date 2020-05-28 18:07
 */
@FunctionalInterface
public interface DrawEventConsumer {

    /**
     *  自定义 Consumer
     *
     * @param circle Ellipse2D
     * @param paint Paint
     * @param stroke Stroke
     */
    void accept(Ellipse2D circle, Paint paint, Stroke stroke);

}
