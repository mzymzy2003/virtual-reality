package com.gmail.jiangyang5157.cardboard.scene.tree;

import com.gmail.jiangyang5157.cardboard.scene.model.Sphere;

/**
 * @author Yang
 * @since 7/10/2016
 */
public class SphereObj extends TreeObject{

    private Sphere sphere;

    public SphereObj(Sphere sphere) {
        this.sphere = sphere;
        this.center = sphere.getPosition();
        this.radius = sphere.getRadius();
    }
}