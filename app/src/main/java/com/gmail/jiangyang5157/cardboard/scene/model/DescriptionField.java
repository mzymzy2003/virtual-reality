package com.gmail.jiangyang5157.cardboard.scene.model;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.ImageView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.gmail.jiangyang5157.app.VolleyApplication;
import com.gmail.jiangyang5157.tookit.base.data.RegularExpressionUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Yang
 * @since 8/6/2016
 */
public class DescriptionField extends TextField {
    private static final String TAG = "[DescriptionField]";

    private Event eventListener;

    public interface Event {
        void onPrepareComplete();
    }

    public DescriptionField(Context context) {
        super(context);
    }

    @Override
    public void prepare(final Ray ray) {
        Pattern pattern = Pattern.compile(RegularExpressionUtils.URL_TEMPLATE);
        Matcher matcher = pattern.matcher(content);
        boolean find = matcher.find();
        Log.d(TAG, "matcher find " + find + ": " + content);
        if (find) {
            Log.d(TAG, "start, end: " + matcher.start() + ", " + matcher.end());
            String url = content.substring(matcher.start(), matcher.end());
            String dotgif = "http://www.pineswcd.com/vertical/Sites/%7BB4CF315C-B365-47D6-A226-5F80C04C0D48%7D/uploads/tree_clipart.gif";
            ImageRequest request = new ImageRequest(url,
                    new Response.Listener<Bitmap>() {
                        @Override
                        public void onResponse(Bitmap bitmap) {
                            Log.d(TAG, "ImageRequest.onResponse: w/h: " + bitmap.getWidth() + ", " + bitmap.getHeight());
                            textureBitmap[0] = bitmap;
                            height = bitmap.getHeight();
                            buildData();
                            eventListener.onPrepareComplete();
                        }
                    }, (int) width, 0, ImageView.ScaleType.CENTER_INSIDE, Bitmap.Config.ARGB_4444,
                    new Response.ErrorListener() {
                        public void onErrorResponse(VolleyError error) {
                            Log.d(TAG, "ImageRequest.onErrorResponse: " + error.toString());
                            textureBitmap[0] = buildTextBitmap(ellipsizeString(url, MAX_TEXT_LENGTH));
                            buildData();
                            eventListener.onPrepareComplete();
                        }
                    });
            request.setRetryPolicy(new DefaultRetryPolicy(VolleyApplication.TIMEOUT_MS, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            VolleyApplication.getInstance().addToRequestQueue(request);
        } else {
            textureBitmap[0] = buildTextBitmap(ellipsizeString(content, MAX_TEXT_LENGTH));
            buildData();
            eventListener.onPrepareComplete();
        }
    }

    public void setEventListener(Event eventListener) {
        this.eventListener = eventListener;
    }
}
