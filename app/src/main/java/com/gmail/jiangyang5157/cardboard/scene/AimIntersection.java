package com.gmail.jiangyang5157.cardboard.scene;

import com.gmail.jiangyang5157.cardboard.scene.projection.Model;
import com.gmail.jiangyang5157.tookit.math.Vector;

/**
 * @author Yang
 * @since 5/3/2016
 */
public class AimIntersection implements Comparable {

    public final Model model;
    public final Vector cameraPosVec;
    public final Vector intersecttPosVec;

    private final double t;

    public AimIntersection(Model model, Vector cameraPosVec, Vector intersecttPosVec, double t){
        this.model = model;
        this.cameraPosVec = cameraPosVec;
        this.intersecttPosVec = intersecttPosVec;
        this.t = t;
    }


    @Override
    public int compareTo(Object another) {
        AimIntersection that = (AimIntersection)another;

        double ret = this.t - that.t;
        if (ret < 0){
            return -1;
        } else if (ret > 0){
            return 1;
        } else {
            return 0;
        }
    }
}
