package com.justin.library;

import java.io.File;

public interface OnCompressListener {

    /**
     * Called when a compression has began.
     *
     */
    void onStart();

    /**
     * Called when a compression has been successfully done.
     *
     * @param file The file than has bean saved in sd card.
     */
    void onSuccess(File file);

    /**
     * Called when a compression couldn't been finished.
     *
     * @param e the error
     */
    void onError(Throwable e);
}
