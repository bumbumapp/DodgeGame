package com.dozingcatsoftware.dodge;

import android.app.Application;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        EdgeToEdgeInsets.install(this);
    }
}
