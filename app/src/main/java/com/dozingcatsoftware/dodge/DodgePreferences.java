package com.dozingcatsoftware.dodge;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.prefs.Preferences;

public class DodgePreferences extends PreferenceActivity {
	
	private static final int ACTIVITY_SELECT_IMAGE = 1;
	
	public static final String USE_BACKGROUND_KEY = "useBackgroundImage";
	public static final String BACKGROUND_IMAGE_FILENAME = "background_image";
	public static final String FLASHING_COLORS_KEY = "flashingColors";
	public static final String TILT_CONTROL_KEY = "tiltControl";
	public static final String SHOW_FPS_KEY = "showFPS";

	BackgroundImagePreference selectBackgroundPref;
    ToolbarPreference toolbarPreference;
    Preference sourceCode,licence;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
        licence=(Preference)findPreference("app_licence");
        licence.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent=new Intent(Intent.ACTION_VIEW,Uri.parse("https://github.com/dozingcat/dodge-android/blob/master/COPYING"));
                startActivity(intent);
                return true;
            }
        });
        sourceCode=(Preference)findPreference("source_code");
        sourceCode.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent=new Intent(Intent.ACTION_VIEW,Uri.parse("https://github.com/bumbumapp/DodgeGame"));
                startActivity(intent);
                return true;
            }
        });
		toolbarPreference=(ToolbarPreference) findPreference("backs");
	    selectBackgroundPref = (BackgroundImagePreference)findPreference("selectBackgroundImage");
		selectBackgroundPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				selectBackgroundImage();
				return true;
			}
		});

        toolbarPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
              startActivity(new Intent(DodgePreferences.this,DodgeMain.class));
                return true;
            }
        });



	}
	/** Starts the Gallery (or other image picker) activity to select an image */
	void selectBackgroundImage() {
		Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI); 
		startActivityForResult(i, ACTIVITY_SELECT_IMAGE); 
	}
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) { 
        super.onActivityResult(requestCode, resultCode, intent);

        switch(requestCode) {
            case ACTIVITY_SELECT_IMAGE:
                if (resultCode == RESULT_OK) {
                    // retrieve selected image URI and make sure image background is enabled
                    Uri imageUri = intent.getData();
                    // imageUri is likely to be a "content" URI which for which we only have
                    // temporary access. We need to copy the file to private storage in order to
                    // read it later.
                    try {
                        OutputStream output = openFileOutput(
                                DodgePreferences.BACKGROUND_IMAGE_FILENAME, Context.MODE_PRIVATE);
                        InputStream input = getContentResolver().openInputStream(imageUri);
                        byte[] buffer = new byte[32768];
                        int bytesRead;
                        long totalBytes = 0;
                        while ((bytesRead = input.read(buffer)) > 0) {
                            output.write(buffer, 0, bytesRead);
                            totalBytes += bytesRead;
                        }
                        output.close();
                        Log.i("DodgePreferences",
                                "Successfully wrote background image, size: " + totalBytes);
                    }
                    catch (Exception ex) {
                        Log.i("DodgePreferences", "Error writing background image: ", ex);
                    }

                    CheckBoxPreference useImagePref = (CheckBoxPreference)findPreference("useBackgroundImage");
                    useImagePref.setChecked(true);
                    selectBackgroundPref.updateBackgroundImage();
                }
                break;
        }
    }
    
}
