package com.gmail.jiangyang5157.cardboard.scene.projection;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * @author Yang
 * @since 5/1/2016
 */
public class Point extends GLModel {

    private float pointSize;

    private final int[] buffers = new int[1];

    public Point(Context context, int vertexShaderRawResource, int fragmentShaderRawResource, String hex) {
        this(context, vertexShaderRawResource, fragmentShaderRawResource, hex2color(hex));
    }

    public Point(Context context, int vertexShaderRawResource, int fragmentShaderRawResource, float[] color) {
        super(context, vertexShaderRawResource, fragmentShaderRawResource);
        this.color = color.clone();
    }

    @Override
    protected void initializeHandle() {
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, MODEL_VIEW_PROJECTION_HANDLE);
        colorHandle = GLES20.glGetUniformLocation(program, COLOR_HANDLE);
        pointSizeHandle = GLES20.glGetUniformLocation(program, POINT_SIZE_HANDLE);

        vertexHandle = GLES20.glGetAttribLocation(program, VERTEX_HANDLE);
    }

    @Override
    protected void buildArrays() {
        vertices = new float[0];
    }

    @Override
    protected void bindBuffers() {
        verticesBuffer = ByteBuffer.allocateDirect(vertices.length * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();
        verticesBuffer.put(vertices).position(0);
        vertices = null;

        GLES20.glGenBuffers(buffers.length, buffers, 0);
        verticesBuffHandle = buffers[0];

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, verticesBuffHandle);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, verticesBuffer.capacity() * BYTES_PER_FLOAT, verticesBuffer, GLES20.GL_STATIC_DRAW);
        verticesBuffer.limit(0);
        verticesBuffer = null;
    }

    @Override
    public void draw() {
        super.draw();

        GLES20.glUseProgram(program);
        GLES20.glEnableVertexAttribArray(vertexHandle);

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);
        GLES20.glUniform4fv(colorHandle, 1, color, 0);
        GLES20.glUniform1f(pointSizeHandle, pointSize);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, verticesBuffHandle);
        GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, 0);

        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1);

        GLES20.glDisableVertexAttribArray(vertexHandle);
        GLES20.glUseProgram(0);

        checkGlEsError("Point - draw end");
    }

    @Override
    public void destroy() {
        Log.d("Point", "destroy");
        GLES20.glDeleteBuffers(buffers.length, buffers, 0);
    }

    public void setPointSize(float pointSize) {
        this.pointSize = pointSize;
    }
}
