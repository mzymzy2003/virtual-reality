package com.gmail.jiangyang5157.cardboard.scene.model;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import com.gmail.jiangyang5157.cardboard.kml.KmlPlacemark;
import com.gmail.jiangyang5157.cardboard.scene.RayIntersection;
import com.gmail.jiangyang5157.cardboard.scene.Lighting;
import com.gmail.jiangyang5157.cardboard.scene.tree.OcTree;
import com.gmail.jiangyang5157.cardboard.scene.tree.OcTreeNode;
import com.gmail.jiangyang5157.cardboard.scene.tree.OcTreeObject;
import com.gmail.jiangyang5157.tookit.math.Vector;
import com.gmail.jiangyang5157.tookit.opengl.GlUtils;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Collections;

/**
 * @author Yang
 * @since 7/2/2016
 */
public class AtomMarkers extends Marker3d {
    private static final String TAG = "[AtomMarkers]";

    private OcTree ocTree;
    private ArrayList<OcTreeNode> ocTreeNodes;
    private ArrayList<AtomMarker> markers;

    public AtomMarkers(Context context) {
        super(context);
        markers = new ArrayList<>();
        ocTree = new OcTree(new float[3], Earth.RADIUS);
    }

    @Override
    public void create(int program) {
        super.create(program);

        ocTreeNodes = ocTree.getValidNodes();
        Log.d(TAG, "ocTree: " + ocTree.toString() + ", valid ocTreeNodes: " + ocTreeNodes.size());

        int iSize = markers.size();
        for(int i = 0; i < iSize; i++){
            AtomMarker marker = markers.get(i);
            marker.create(program);
            marker.mvMatrixHandle = mvMatrixHandle;
            marker.mvpMatrixHandle = mvpMatrixHandle;
            marker.colorHandle = colorHandle;
            marker.indicesBufferCapacity = indicesBufferCapacity;
        }

        setCreated(true);
        setVisible(true);
    }

    @Override
    public void draw() {
        if (!isCreated() || !isVisible()) {
            return;
        }

        GLES20.glUseProgram(program);
        GLES20.glEnableVertexAttribArray(vertexHandle);
        GLES20.glEnableVertexAttribArray(normalHandle);
        if (lighting != null) {
            GLES20.glUniform3fv(lightPosHandle, 1, lighting.getLightPosInCameraSpace(), 0);
        }

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, verticesBuffHandle);
        GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, 0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, normalsBuffHandle);
        GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT, false, 0, 0);

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indicesBuffHandle);

        int iSize = markers.size();
        for(int i = 0; i < iSize; i++){
            markers.get(i).draw();
        }

        GLES20.glDisableVertexAttribArray(vertexHandle);
        GLES20.glDisableVertexAttribArray(normalHandle);
        GLES20.glUseProgram(0);

        GlUtils.printGlError(TAG + " - draw end");
    }

    @Override
    public void update(float[] view, float[] perspective) {
        int iSize = markers.size();
        for(int i = 0; i < iSize; i++){
           markers.get(i).update(view, perspective);
        }
    }

    private void addMarker(AtomMarker marker) {
        ocTree.insertObject(new OcTreeObject(marker));
        markers.add(marker);
    }

    public AtomMarker addMarker(KmlPlacemark kmlPlacemark, MarkerOptions markerUrlStyle, int markerColorInteger) {
        LatLng latLng = markerUrlStyle.getPosition();
        AtomMarker marker = new AtomMarker(context);
        if (markerColorInteger != 0) {
            marker.setColor(markerColorInteger);
        }
        marker.setOnClickListener(onClickListener);
        marker.setLocation(latLng, AtomMarker.ALTITUDE);
        marker.setName(kmlPlacemark.getProperty("name"));
        marker.setDescription(kmlPlacemark.getProperty("description"));

        String objProperty = kmlPlacemark.getProperty("obj");
        if (objProperty != null) {
            ObjModel objModel = new ObjModel(context, kmlPlacemark.getProperty("title"), objProperty);
            objModel.setLighting(new Lighting() {
                @Override
                public float[] getLightPosInCameraSpace() {
                    return Lighting.LIGHT_POS_IN_CAMERA_SPACE_CENTER;
                }
            });
            marker.setObjModel(objModel);
        }

        addMarker(marker);
        return marker;
    }

    public RayIntersection getIntersection(Vector cameraPos_vec, Vector headForwardFrac_vec, final float[] headView) {
        if (!isCreated() || !isVisible()) {
            return null;
        }

        RayIntersection ret = null;
        ArrayList<RayIntersection> rayIntersections = new ArrayList<>();
        int iSize = ocTreeNodes.size();
        for (int i = 0; i < iSize; i++){
            RayIntersection rayIntersection = ocTreeNodes.get(i).getObjectIntersection(cameraPos_vec, headForwardFrac_vec, headView);
            if (rayIntersection != null) {
                rayIntersections.add(rayIntersection);
            }
        }

        int size = rayIntersections.size();
        if (size > 0) {
            if (size > 1) {
                Collections.sort(rayIntersections);
            }
            ret = rayIntersections.get(0);
        }

        return ret;
    }

    public OcTree getOcTree() {
        return ocTree;
    }

    public ArrayList<AtomMarker> getMarkers() {
        return markers;
    }

    public void destoryOcTree() {
        ocTree.clean();
    }

    public void destoryMarks() {
        int iSize = markers.size();
        for(int i = 0; i < iSize; i++){
            markers.get(i).destroy();
        }
        markers.clear();
    }

    @Override
    public void destroy() {
        destoryOcTree();
        destoryMarks();
        super.destroy();
    }
}
