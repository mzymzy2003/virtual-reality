package com.gmail.jiangyang5157.cardboard.scene.model;

import android.content.Context;
import android.opengl.Matrix;
import android.util.Log;

import com.gmail.jiangyang5157.cardboard.scene.Coordinate;
import com.gmail.jiangyang5157.cardboard.scene.Head;
import com.gmail.jiangyang5157.cardboard.scene.RayIntersection;
import com.gmail.jiangyang5157.tookit.app.AppUtils;
import com.gmail.jiangyang5157.tookit.math.Vector;
import com.gmail.jiangyang5157.tookit.math.Vector3d;
import com.google.android.gms.maps.model.LatLng;

/**
 * @author Yang
 * @since 4/12/2016.
 */
public class Marker extends Icosphere {
    private static final String TAG = "[Marker]";

    public static final float RADIUS = 1;
    public static final float ALTITUDE = -1 * RADIUS;

    private String name;
    private String description;
    private Coordinate coordinate;

    private ObjModel objModel;

    public Marker(Context context) {
        super(context, 3);
        setRadius(RADIUS);
    }

    @Override
    public void create(int program) {
        if (color == null) {
            setColor(AppUtils.getColor(context, com.gmail.jiangyang5157.tookit.R.color.Yellow, null));
        }
        super.create(program);

        setCreated(true);
        setVisible(true);
    }

    public void setLocation(LatLng latLng, float altitude) {
        this.coordinate = new Coordinate(latLng.latitude, latLng.longitude, altitude, Earth.RADIUS);
        Matrix.setIdentityM(translation, 0);
        Matrix.translateM(translation, 0,
                (float) coordinate.ecef[0],
                (float) coordinate.ecef[1],
                (float) coordinate.ecef[2]);
    }

    @Override
    public RayIntersection onIntersection(Head head) {
        if (!isCreated() || !isVisible()) {
            return null;
        }

        float[] position = getPosition();
        float[] cameraPos = head.getCamera().getPosition();
        float[] camera_pos = new float[]{
                position[0] - cameraPos[0],
                position[1] - cameraPos[1],
                position[2] - cameraPos[2]
        };
        // Convenience vector for extracting the position from a matrix via multiplication.
        float[] posMultiply = new float[]{camera_pos[0], camera_pos[1], camera_pos[2], 0.0f};
        // position in camera space
        float[] posInCameraSpace = new float[4];
        // Convert object space to camera space - Use the headView from onNewFrame.
        Matrix.multiplyMM(modelView, 0, head.getHeadView(), 0, model, 0);
        Matrix.multiplyMV(posInCameraSpace, 0, modelView, 0, posMultiply, 0);

        double pitch = Math.atan2(posInCameraSpace[1], -posInCameraSpace[2]);
        double yaw = Math.atan2(posInCameraSpace[0], -posInCameraSpace[2]);
        final double PITCH_LIMIT = 0.02;
        final double YAW_LIMIT = 0.02;
        if (Math.abs(pitch) > PITCH_LIMIT || Math.abs(yaw) > YAW_LIMIT) {
            return null;
        } else {
            Vector camera_pos_vec = new Vector3d(camera_pos[0], camera_pos[1], camera_pos[2]);
            return new RayIntersection(this, camera_pos_vec.length() - radius);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ObjModel getObjModel() {
        return objModel;
    }

    public void setObjModel(ObjModel model) {
        this.objModel = model;
    }

    @Override
    public void destroy() {
        Log.d(TAG, "destroy");
        super.destroy();
    }
}