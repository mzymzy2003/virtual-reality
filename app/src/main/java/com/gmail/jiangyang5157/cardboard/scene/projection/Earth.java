package com.gmail.jiangyang5157.cardboard.scene.projection;

import android.content.Context;
import android.opengl.GLES20;
import android.os.Handler;
import android.util.Log;

import com.android.volley.VolleyError;
import com.gmail.jiangyang5157.cardboard.kml.KmlLayer;
import com.gmail.jiangyang5157.cardboard.kml.KmlPlacemark;
import com.gmail.jiangyang5157.cardboard.net.Downloader;
import com.gmail.jiangyang5157.cardboard.scene.Creation;
import com.gmail.jiangyang5157.cardboard.scene.Intersection;
import com.gmail.jiangyang5157.cardboard.scene.Head;
import com.gmail.jiangyang5157.cardboard.scene.Lighting;
import com.gmail.jiangyang5157.cardboard.vr.Constant;
import com.gmail.jiangyang5157.cardboard.vr.R;
import com.gmail.jiangyang5157.tookit.app.AppUtils;
import com.gmail.jiangyang5157.tookit.data.buffer.BufferUtils;
import com.gmail.jiangyang5157.tookit.math.Vector;
import com.gmail.jiangyang5157.tookit.math.Vector3d;
import com.gmail.jiangyang5157.tookit.opengl.GlUtils;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

/**
 * @author Yang
 * @since 4/12/2016.
 */
public class Earth extends UvSphere implements Creation {
    private static final String TAG = "[Earth]";

    private String urlTexture;
    private String urlKml;

    private static final int VERTEX_SHADER_RAW_RESOURCE = R.raw.earth_uv_vertex_shader;
    private static final int FRAGMENT_SHADER_RAW_RESOURCE = R.raw.earth_uv_fragment_shader;

    public static final float RADIUS = 4000f;
    private static final int STACKS = 32;
    private static final int SLICES = 32;

    private static final float MARKER_RADIUS = RADIUS / 50;
    private static final float MARKER_ALTITUDE = -1 * MARKER_RADIUS;
    private static final float CAMERA_ALTITUDE = (Math.abs(MARKER_ALTITUDE) + MARKER_RADIUS + Panel.DISTANCE + Ray.SPACE) * (MARKER_ALTITUDE > 0 ? 1 : -1);

    private ArrayList<Marker> markers;

    private Intersection.Clickable onMarkerClickListener;

    private final int[] buffers = new int[3];
    private final int[] texBuffers = new int[1];

    protected Lighting markerLighting;
    protected Lighting markerObjModelLighting;

    protected int creationState = STATE_BEFORE_PREPARE;

    public Earth(Context context, String urlKml, String urlTexture) {
        super(context, VERTEX_SHADER_RAW_RESOURCE, FRAGMENT_SHADER_RAW_RESOURCE);
        markers = new ArrayList<>();
        this.urlKml = urlKml;
        this.urlTexture = urlTexture;
        radius = RADIUS;
    }

    public boolean checkPreparation() {
        File fileKml = new File(Constant.getAbsolutePath(context, Constant.getPath(urlKml)));
        File fileTexture = new File(Constant.getAbsolutePath(context, Constant.getPath(urlTexture)));
        return fileKml.exists() && fileTexture.exists();
    }

    @Override
    public void prepare(final Ray ray) {
        Handler handle = getHandler();
        if (handle != null) {
            handle.post(new Runnable() {
                @Override
                public void run() {
                    creationState = STATE_PREPARING;
                    ray.addBusy();

                    if (checkPreparation()) {
                        final File fileKml = new File(Constant.getAbsolutePath(context, Constant.getPath(urlKml)));
                        prepareMarks(fileKml);

                        ray.subtractBusy();
                        creationState = STATE_BEFORE_CREATE;
                    } else {
                        final File fileKml = new File(Constant.getAbsolutePath(context, Constant.getPath(urlKml)));
                        if (!fileKml.exists()) {
                            Log.d(TAG, fileKml.getAbsolutePath() + " not exist.");
                            new Downloader(urlKml, fileKml, new Downloader.ResponseListener() {
                                @Override
                                public boolean onStart(Map<String, String> headers) {
                                    return true;
                                }

                                @Override
                                public void onComplete(Map<String, String> headers) {
                                    prepareMarks(fileKml);

                                    if (checkPreparation()) {
                                        ray.subtractBusy();
                                        creationState = STATE_BEFORE_CREATE;
                                    }
                                }

                                @Override
                                public void onError(String url, VolleyError volleyError) {
                                    AppUtils.buildToast(context, url + " " + volleyError.toString());
                                    ray.subtractBusy();
                                    creationState = STATE_BEFORE_PREPARE;
                                }
                            });
                        }

                        File fileTexture = new File(Constant.getAbsolutePath(context, Constant.getPath(urlTexture)));
                        if (!fileTexture.exists()) {
                            Log.d(TAG, fileTexture.getAbsolutePath() + " not exist.");
                            new Downloader(urlTexture, fileTexture, new Downloader.ResponseListener() {
                                @Override
                                public boolean onStart(Map<String, String> headers) {
                                    return true;
                                }

                                @Override
                                public void onComplete(Map<String, String> headers) {
                                    if (checkPreparation()) {
                                        ray.subtractBusy();
                                        creationState = STATE_BEFORE_CREATE;
                                    }
                                }

                                @Override
                                public void onError(String url, VolleyError volleyError) {
                                    AppUtils.buildToast(context, url + " " + volleyError.toString());
                                    ray.subtractBusy();
                                    creationState = STATE_BEFORE_PREPARE;
                                }
                            });
                        }
                    }
                }
            });
        }
    }

    private void prepareMarks(File fileKml) {
        InputStream in = null;
        try {
            in = new FileInputStream(fileKml);
            KmlLayer kmlLayer = new KmlLayer(this, in, context);
            kmlLayer.addLayerToMap();
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void create() {
        creationState = STATE_CREATING;
        create(radius, STACKS, SLICES);

        for (Marker marker : markers) {
            marker.create();
        }

        creationState = STATE_BEFORE_CREATE;
    }

    public void adjustMarkerRadius(float[] cameraPos) {
        final Vector cameraPosVec = new Vector3d(cameraPos[0], cameraPos[1], cameraPos[2]);
        for (Marker marker : markers) {
            float[] pos = marker.getPosition();
            Vector posVec = new Vector3d(pos[0], pos[1], pos[2]);
            double distance = cameraPosVec.plus(posVec).length();
            float r = (float) (distance * 0.01);
            r *= r;
            Log.d("####", "distance / r = " +  + distance + ", " + r);
//            marker.setRadius(r);
        }
    }

    @Override
    public int getCreationState() {
        return creationState;
    }

    @Override
    protected void bindBuffers() {
        FloatBuffer verticesBuffer = ByteBuffer.allocateDirect(vertices.length * BufferUtils.BYTES_PER_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();
        verticesBuffer.put(vertices).position(0);
        vertices = null;

        ShortBuffer indicesBuffer = ByteBuffer.allocateDirect(indices.length * BufferUtils.BYTES_PER_SHORT).order(ByteOrder.nativeOrder()).asShortBuffer();
        indicesBuffer.put(indices).position(0);
        indices = null;
        indicesBufferCapacity = indicesBuffer.capacity();

        FloatBuffer texturesBuffer = ByteBuffer.allocateDirect(textures.length * BufferUtils.BYTES_PER_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();
        texturesBuffer.put(textures).position(0);
        textures = null;

        GLES20.glGenBuffers(buffers.length, buffers, 0);
        verticesBuffHandle = buffers[0];
        indicesBuffHandle = buffers[1];
        texturesBuffHandle = buffers[2];

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, verticesBuffHandle);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, verticesBuffer.capacity() * BufferUtils.BYTES_PER_FLOAT, verticesBuffer, GLES20.GL_STATIC_DRAW);
        verticesBuffer.limit(0);

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indicesBuffHandle);
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, indicesBuffer.capacity() * BufferUtils.BYTES_PER_SHORT, indicesBuffer, GLES20.GL_STATIC_DRAW);
        indicesBuffer.limit(0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, texturesBuffHandle);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, texturesBuffer.capacity() * BufferUtils.BYTES_PER_FLOAT, texturesBuffer, GLES20.GL_STATIC_DRAW);
        texturesBuffer.limit(0);

        InputStream in = null;
        try {
            in = new FileInputStream(new File(Constant.getAbsolutePath(context, Constant.getPath(urlTexture))));
            texBuffers[0] = loadTexture(in);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void bindHandles() {
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, MODEL_VIEW_PROJECTION_HANDLE);
        texIdHandle = GLES20.glGetUniformLocation(program, TEXTURE_ID_HANDLE);

        vertexHandle = GLES20.glGetAttribLocation(program, VERTEX_HANDLE);
        texCoordHandle = GLES20.glGetAttribLocation(program, TEXTURE_COORDS_HANDLE);
    }

    @Override
    public void update(float[] view, float[] perspective) {
        for (Marker marker : markers) {
            marker.update(view, perspective);
        }
        super.update(view, perspective);
    }

    @Override
    public void draw() {
        for (Marker marker : markers) {
            marker.draw();
        }

        if (!isCreated() || !isVisible()) {
            return;
        }

        GLES20.glUseProgram(program);
        GLES20.glEnableVertexAttribArray(vertexHandle);
        GLES20.glEnableVertexAttribArray(texCoordHandle);

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, verticesBuffHandle);
        GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, 0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, texturesBuffHandle);
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texBuffers[0]);
        GLES20.glUniform1i(texIdHandle, 0);

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indicesBuffHandle);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indicesBufferCapacity, GLES20.GL_UNSIGNED_SHORT, 0);

        GLES20.glDisableVertexAttribArray(vertexHandle);
        GLES20.glDisableVertexAttribArray(texCoordHandle);
        GLES20.glUseProgram(0);

        GlUtils.printGlError("UvSphere - draw end");
    }

    @Override
    public void destroy() {
        Log.d("UvSphere", "destroy");
        super.destroy();
        markers.clear();
        GLES20.glDeleteBuffers(buffers.length, buffers, 0);
        GLES20.glDeleteTextures(texBuffers.length, texBuffers, 0);

        for (Marker marker : markers) {
            marker.destroy();
        }
    }

    public void removeMarker(Marker marker) {
        markers.remove(marker);
    }

    private void addMarker(Marker marker) {
        markers.add(marker);
    }

    public Marker addMarker(KmlPlacemark kmlPlacemark, MarkerOptions markerUrlStyle, int markerColorInteger) {
        LatLng latLng = markerUrlStyle.getPosition();
        Marker marker = new Marker(context, this);
        Log.d(TAG, "markerColorInteger: " + markerColorInteger);
        if (markerColorInteger != 0) {
            marker.setColor(markerColorInteger);
        }
        marker.setRadius(MARKER_RADIUS);
        marker.setOnClickListener(onMarkerClickListener);
        marker.setLocation(latLng, MARKER_ALTITUDE);
        marker.setName(kmlPlacemark.getProperty("name"));
        marker.setDescription(kmlPlacemark.getProperty("description"));
        marker.setLighting(markerLighting);

        String objProperty = kmlPlacemark.getProperty("obj");
        if (objProperty != null) {
            ObjModel objModel = new ObjModel(context,
                    kmlPlacemark.getProperty("title"),
                    objProperty);
            objModel.setLighting(markerObjModelLighting);
            marker.setObjModel(objModel);
        }

        addMarker(marker);
        return marker;
    }

    public boolean contain(float[] point) {
        return contain(radius + CAMERA_ALTITUDE, getPosition(), point);
    }

    @Override
    public Intersection onIntersect(Head head) {
        if (!isCreated() || !isVisible()) {
            return null;
        }
        Intersection ret;

        ArrayList<Intersection> intersections = new ArrayList<Intersection>();
        for (final Marker mark : markers) {
            Intersection intersection = mark.onIntersect(head);
            if (intersection != null) {
                intersections.add(intersection);
            }
        }
        Collections.sort(intersections);
        if (intersections.size() > 0) {
            ret = intersections.get(0);
        } else {
            ret = super.onIntersect(head);
        }

        return ret;
    }

    public void setOnMarkerClickListener(Intersection.Clickable onClickListener) {
        this.onMarkerClickListener = onClickListener;
    }

    public void setMarkerObjModelLighting(Lighting markerObjModelLighting) {
        this.markerObjModelLighting = markerObjModelLighting;
    }

    public void setMarkerLighting(Lighting markerLighting) {
        this.markerLighting = markerLighting;
    }
}
