package com.dozingcatsoftware.dodge;

import static org.junit.Assert.assertFalse;

import android.app.Activity;

import org.junit.Test;

public class DodgePreferencesTest {
    @Test
    public void missingPickerDataIsIgnored() {
        assertFalse(DodgePreferences.hasImageResult(Activity.RESULT_OK, null));
    }

    @Test
    public void cancelledPickerIsIgnored() {
        assertFalse(DodgePreferences.hasImageResult(Activity.RESULT_CANCELED, null));
    }
}
