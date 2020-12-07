package com.zhang.autotouch;

import android.app.Application;
import android.content.Context;

public class AppApplication extends Application {
    public static Context app;
    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
    }
}
