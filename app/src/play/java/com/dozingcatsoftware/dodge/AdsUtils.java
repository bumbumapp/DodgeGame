package com.dozingcatsoftware.dodge;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

public class AdsUtils {

    public static RewardedAd rewardedAd;
    public static boolean ISREWARDED = false;
    private static boolean rewardedLoading;

    /** Anchored adaptive banner (only after consent allows ad requests). */
    public static void showGoogleBannerAd(Activity activity, ViewGroup container) {
        container.post(() -> BannerHelper.load(activity, container));
    }

    public static void loadRewardAds(Context context) {
        if (rewardedAd != null || rewardedLoading || !AdsConsent.canRequestAds(context)) return;
        rewardedLoading = true;
        RewardedAd.load(context.getApplicationContext(), context.getString(R.string.admob_rewarded_id),
                new AdRequest.Builder().build(), new RewardedAdLoadCallback() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        rewardedLoading = false;
                        rewardedAd = null;
                    }

                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        rewardedLoading = false;
                        rewardedAd = ad;
                    }
                });
    }

    /** Result of a rewarded ad: {@code earned} is true when the player watched it to the reward. */
    public interface RewardCallback {
        void onClosed(boolean earned);
    }

    public static boolean isRewardedReady() {
        return rewardedAd != null;
    }

    /**
     * Shows the preloaded rewarded ad. Returns false (and never calls {@code callback}) if none is ready;
     * otherwise {@code callback} runs once when the ad is dismissed or fails to show.
     */
    public static boolean showRewarded(Activity activity, RewardCallback callback) {
        final RewardedAd ad = rewardedAd;
        if (ad == null) {
            return false;
        }
        rewardedAd = null;
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                boolean earned = ISREWARDED;
                ISREWARDED = false;
                callback.onClosed(earned);
                loadRewardAds(activity);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                Log.e("TAG", "Ad failed to show fullscreen content.");
                callback.onClosed(false);
                loadRewardAds(activity);
            }
        });
        ad.show(activity, rewardItem -> ISREWARDED = true);
        return true;
    }
}
