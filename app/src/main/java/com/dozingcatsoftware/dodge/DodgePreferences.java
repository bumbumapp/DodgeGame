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
        Preference privacy = findPreference("privacy_options");
        if (privacy != null) {
            if (AdsConsent.isPrivacyOptionsRequired(this)) {
                privacy.setOnPreferenceClickListener(preference -> {
                    AdsConsent.showPrivacyOptions(DodgePreferences.this);
                    return true;
                });
            } else {
                android.preference.PreferenceGroup about =
                        (android.preference.PreferenceGroup) findPreference("about_category");
                if (about != null) about.removePreference(privacy);
            }
        }
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
		// System photo picker where available (no storage permission needed), document picker otherwise.
		Intent i;
		if (android.os.Build.VERSION.SDK_INT >= 33) {
			i = new Intent(android.provider.MediaStore.ACTION_PICK_IMAGES);
		} else {
			i = new Intent(Intent.ACTION_GET_CONTENT);
			i.addCategory(Intent.CATEGORY_OPENABLE);
		}
		i.setType("image/*");
		try {
			startActivityForResult(i, ACTIVITY_SELECT_IMAGE);
		} catch (android.content.ActivityNotFoundException e) {
			Log.w("DodgePreferences", "No image picker available", e);
		}
	}
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) { 
        super.onActivityResult(requestCode, resultCode, intent);

        switch(requestCode) {
            case ACTIVITY_SELECT_IMAGE:
                if (hasImageResult(resultCode, intent)) {
                    // retrieve selected image URI and make sure image background is enabled
                    Uri imageUri = intent.getData();
                    // imageUri is likely to be a "content" URI which for which we only have
                    // temporary access. We need to copy the file to private storage in order to
                    // read it later.
                    try (OutputStream output = openFileOutput(
                                DodgePreferences.BACKGROUND_IMAGE_FILENAME, Context.MODE_PRIVATE);
                         InputStream input = getContentResolver().openInputStream(imageUri)) {
                        if (input == null) throw new java.io.FileNotFoundException(String.valueOf(imageUri));
                        byte[] buffer = new byte[32768];
                        int bytesRead;
                        long totalBytes = 0;
                        while ((bytesRead = input.read(buffer)) > 0) {
                            output.write(buffer, 0, bytesRead);
                            totalBytes += bytesRead;
                        }
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
    
    /** The picker may return RESULT_OK with no data (e.g. provider died); that used to crash with an NPE. */
    static boolean hasImageResult(int resultCode, Intent data) {
        return resultCode == RESULT_OK && data != null && data.getData() != null;
    }
}
