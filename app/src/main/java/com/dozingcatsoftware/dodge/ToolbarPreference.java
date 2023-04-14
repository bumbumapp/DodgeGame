package com.dozingcatsoftware.dodge;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

public class ToolbarPreference extends Preference {

   ImageView imageView;
   Context context;
    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        imageView=view.findViewById(R.id.backc);
        startActivity();

    }

     void startActivity() {
         imageView.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View view) {
                 context.startActivity(new Intent(context,DodgeMain.class));
             }
         });
    }

    public ToolbarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context=context;
    }


}
