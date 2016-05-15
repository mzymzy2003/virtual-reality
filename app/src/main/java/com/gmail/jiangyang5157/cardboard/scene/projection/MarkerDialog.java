package com.gmail.jiangyang5157.cardboard.scene.projection;

import android.content.Context;

import com.gmail.jiangyang5157.tookit.app.AppUtils;

/**
 * @author Yang
 * @since 5/13/2016
 */
public class MarkerDialog extends Dialog{

    private Marker marker;

    public MarkerDialog(Context context) {
        super(context);
    }

    public Marker getMarker() {
        return marker;
    }

    public void setMarker(Marker marker) {
        this.marker = marker;
    }

    @Override
    public void destroy() {
        super.destroy();
        setMarker(null);
    }

    @Override
    public void create() {
        // TODO: 5/14/2016 analysis marker for creating muti-child-panel
        TextField tf1 = new TextField(context);
        tf1.create(marker.name);
        addPanel(tf1);
        TextField tf2 = new TextField(context);
        tf2.create("2nd TextField");
        addPanel(tf2);
        TextField tf3 = new TextField(context);
        tf3.create("3rd");
        addPanel(tf3);

        float w = 0;
        float h = 0;
        h += PADDING_BOARD;
        for (Panel panel : panels) {
            w = Math.max(w, panel.width);
            h += panel.height;
            h += PADDING_BOARD;
        }
        w += PADDING_BOARD * 2;
        width = w;
        height = h;

        create(width, height, AppUtils.getColor(context, COLOR_BACKGROUND_RES_ID));
    }

    @Override
    public void setPosition(float[] cameraPos, float[] forward, float[] up, float[] right, float[] eulerAngles) {
        super.setPosition(cameraPos, forward, up, right, eulerAngles);

        //
        cameraPos[0] -= forward[0] * PADDING_LAYER;
        cameraPos[1] -= forward[1] * PADDING_LAYER;
        cameraPos[2] -= forward[2] * PADDING_LAYER;

        //
        cameraPos[0] += up[0] * height / 2;
        cameraPos[1] += up[1] * height / 2;
        cameraPos[2] += up[2] * height / 2;

        cameraPos[0] -= up[0] * PADDING_BOARD;
        cameraPos[1] -= up[1] * PADDING_BOARD;
        cameraPos[2] -= up[2] * PADDING_BOARD;

        for (Panel panel : panels){
            cameraPos[0] -= up[0] * panel.height / 2;
            cameraPos[1] -= up[1] * panel.height / 2;
            cameraPos[2] -= up[2] * panel.height / 2;

            panel.setPosition(cameraPos, forward, up, right, eulerAngles);

            cameraPos[0] -= up[0] * panel.height / 2;
            cameraPos[1] -= up[1] * panel.height / 2;
            cameraPos[2] -= up[2] * panel.height / 2;

            cameraPos[0] -= up[0] * PADDING_BOARD;
            cameraPos[1] -= up[1] * PADDING_BOARD;
            cameraPos[2] -= up[2] * PADDING_BOARD;
        }
    }
}
