package com.gmail.jiangyang5157.cardboard.scene.projection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.text.TextPaint;

import com.gmail.jiangyang5157.tookit.app.AppUtils;

/**
 * @author Yang
 * @since 5/8/2016
 */
public class TextField extends Panel {

    private final String text;

    public static final int DEFAULT_WIDTH = 400;
    public static final int DEFAULT_HEIGHT = 100;

    public static final int COLOR_BACKGROUND_RES_ID = com.gmail.jiangyang5157.tookit.R.color.Green;
    public static final int COLOR_TEXT_RES_ID = com.gmail.jiangyang5157.tookit.R.color.White;

    private final Earth earth;

    public TextField(Context context, Earth earth, float[] position, String text) {
        super(context, DEFAULT_WIDTH, DEFAULT_HEIGHT, position);
        this.earth = earth;
        setColor(AppUtils.getColor(context, COLOR_BACKGROUND_RES_ID));
        this.text = text;
    }

    @Override
    protected int createTexture() {
        return createTextTexture();
    }

    private int createTextTexture(){
        final int[] textureHandle = new int[1];
        GLES20.glGenTextures(1, textureHandle, 0);

        if (textureHandle[0] == 0) {
            throw new RuntimeException("Error loading texture.");
        } else {
            Bitmap bitmap = Bitmap.createBitmap((int)width, (int)height, Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(bitmap);
            bitmap.eraseColor(getColorInt());

            final float GESTURE_THRESHOLD_DP = 16.0f; // The gesture threshold expressed in dp
            final float scale = context.getResources().getDisplayMetrics().density;
            float pixels = (GESTURE_THRESHOLD_DP * scale + 0.5f); // Convert the dps to pixels, based on density scale

            TextPaint textPaint = new TextPaint();
            textPaint.setAntiAlias(true);
            textPaint.setTextSize(pixels);
            textPaint.setColor(AppUtils.getColor(context, COLOR_TEXT_RES_ID));
            int baseY = (int) ((canvas.getHeight() / 2) - ((textPaint.descent() + textPaint.ascent()) / 2));
            canvas.drawText(text, 0, baseY, textPaint);

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
            bitmap.recycle();
        }
        return textureHandle[0];
    }
}
