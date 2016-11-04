package com.gmail.jiangyang5157.cardboard.net;

import android.support.annotation.NonNull;

import com.android.volley.VolleyError;
import com.gmail.jiangyang5157.cardboard.vr.AssetFile;

import java.util.HashSet;
import java.util.Map;

/**
 * @author Yang
 * @since 10/1/2016
 */

public class MultiFilePrepare extends NetRequest {

    private HashSet<AssetFile> assetFileSet;

    private int todo = 0;

    public interface PrepareListener {
        void onStart();

        void onComplete(HashSet<AssetFile> assetFileSet);
    }

    private final MultiFilePrepare.PrepareListener prepareListener;

    public MultiFilePrepare(@NonNull HashSet<AssetFile> assetFileSet, @NonNull MultiFilePrepare.PrepareListener prepareListener) {
        this.assetFileSet = assetFileSet;
        this.prepareListener = prepareListener;
    }

    @Override
    public void start() {
        prepareListener.onStart();

        todo += assetFileSet.size();
        for (AssetFile assetFile : assetFileSet) {
            if (assetFile.isReady()) {
                complete(assetFile);
            } else {
                new Downloader(assetFile, responseListener).start();
            }
        }
    }

    private void complete(AssetFile assetFile) {
        todo--;
        if (todo <= 0) {
            prepareListener.onComplete(assetFileSet);
        }
    }

    private Downloader.ResponseListener responseListener = new Downloader.ResponseListener() {
        @Override
        public boolean onStart(Map<String, String> headers) {
            prepareListener.onStart();
            return true;
        }

        @Override
        public void onComplete(AssetFile assetFile, Map<String, String> headers) {
            complete(assetFile);
        }

        @Override
        public void onError(AssetFile assetFile, VolleyError volleyError) {
            complete(assetFile);
        }
    };
}
