package com.dozingcatsoftware.dodge;

import static com.dozingcatsoftware.dodge.AdsUtils.ISREWARDED;
import static com.dozingcatsoftware.dodge.AdsUtils.mInterstitialAd;
import static com.dozingcatsoftware.dodge.AdsUtils.rewardedAd;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.dozingcatsoftware.dodge.model.Field;
import com.dozingcatsoftware.dodge.model.LevelManager;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.firebase.crashlytics.FirebaseCrashlytics;


import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;

public class DodgeMain extends Activity implements Field.Delegate {

	Field field;
	LevelManager levelManager;
	ImageButton icMore;
	FieldView fieldView;
	View menuView;
	View pausedMenuView;
	TextView levelText;
	TextView livesText;
	TextView statusText;
	TextView bestLevelText, bestLevelHighScore;
	TextView bestFreePlayLevelText, bestFreePlayHighScoreText;
	Button continueFreePlayButton;
	View bestFreePlayLevelView;
	View bestLevelView;
	MenuItem endGameMenuItem;
	MenuItem preferencesMenuItem;
	Timer timer;
	int seconds;
	int elapsedSeconds;
	TextView time, score;
	int scoreInt = 1;
	boolean paused = true;
	private static final int ACTIVITY_PREFERENCES = 1;

	Handler messageHandler;

	int lives = 3;

	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);
		FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);
		messageHandler = new Handler() {
			public void handleMessage(Message m) {
				processMessage(m);
			}
		};
		levelManager = new LevelManager();

		field = new Field();
		field.setDelegate(this);
		field.setLevelManager(levelManager);
		field.setMaxBullets(levelManager.numberOfBulletsForCurrentLevel());

		levelText = (TextView) findViewById(R.id.levelText);
		time = findViewById(R.id.timeText);
		score = findViewById(R.id.score);
		icMore = findViewById(R.id.ic_more);
		icMore.setEnabled(false);
		livesText = (TextView) findViewById(R.id.livesText);
		statusText = (TextView) findViewById(R.id.statusText);
		AdsUtils.loadGoogleInterstitialAd(this);
		AdsUtils.loadRewardAds(this);
//		rewardAd=new RewardAd(this,getString(R.string.reward_id));
//		LoadRewardAds.loadRewardAd(rewardAd,this);
		// uncomment to clear high scores
        /*
        setBestLevel(true, 0);
        setBestLevel(false, 0);
        */
		icMore.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				pauseGame();
			}
		});
		bestLevelText = (TextView) findViewById(R.id.bestLevelText);
		bestFreePlayHighScoreText = findViewById(R.id.highscore);
		bestLevelHighScore = findViewById(R.id.bestScoreText);
		bestFreePlayLevelText = (TextView) findViewById(R.id.bestFreePlayLevelText);
		continueFreePlayButton = (Button) findViewById(R.id.continueFreePlayButton);
		bestLevelView = findViewById(R.id.bestLevelView);
		bestFreePlayLevelView = findViewById(R.id.bestFreePlayLevelView);

		Button newGameButton = (Button) findViewById(R.id.newGameButton);
		newGameButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				doNewGame();
			}
		});

		Button freePlayButton = (Button) findViewById(R.id.freePlayButton);
		freePlayButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				doFreePlay(1);
			}
		});

		continueFreePlayButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				doFreePlay(bestLevel(true));
			}
		});

		AdView bannerView = findViewById(R.id.hw_banner_view);
		AdsUtils.showGoogleBannerAd(this,bannerView);
//        Button aboutButton = (Button)findViewById(R.id.aboutButton);
//        aboutButton.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                doAbout();
//            }
//        });

		Button preferencesButton = findViewById(R.id.preferencesButton);
		preferencesButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				gotoPreferences();
			}
		});

		Button continueGameButton = findViewById(R.id.continueGameButton);
		continueGameButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				resumeGame();
			}
		});

		Button endGameButton = findViewById(R.id.endGameButton);
		endGameButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				doGameOver();
			}
		});

		menuView = findViewById(R.id.menuView);
		pausedMenuView = findViewById(R.id.pausedMenuView);
		showMenu();

		fieldView = (FieldView) findViewById(R.id.fieldView);
		fieldView.setField(field);
		fieldView.setMessageHandler(messageHandler);

		updateFromPreferences();
	}

	class RemindTask extends TimerTask {
		public void run() {
			if (!paused) {
				seconds++;
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						int minutes = seconds / 60;
						int remainingSeconds = seconds % 60;
						String timeString = String.format("%02d:%02d", minutes, remainingSeconds);
						time.setText(timeString);
						if (scoreInt <= 0) {
							doGameOver();
						}
					}
				});
			}
		}
	}

	public void pauseTimer() {
		paused = true;
		if (timer != null) {
			timer.cancel();
		}
		elapsedSeconds = seconds; // store the elapsed time
	}

	public void resumeTimer() {
		paused = false;
		icMore.setEnabled(true);
		timer = new Timer(); // create a new timer
		seconds = elapsedSeconds; // set the elapsed time as the new starting time
		timer.schedule(new RemindTask(), 0, 1000); // schedule a new RemindTask
	}

	public void startTimer() {
		icMore.setEnabled(true);
		paused = false;
		seconds = 0;
		if (timer != null) {
			timer.cancel();
		}
		timer = new Timer(); // create a new timer
		timer.schedule(new RemindTask(), 0, 1000); // schedule a new RemindTask
	}

	@Override
	public void onPause() {
		super.onPause();
		pauseTimer();
		fieldView.stop();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (isGameInProgress()) {
			pauseGame();
			// We can't draw immediately because the FieldView won't be able to lock its canvas.
			// Drawing after a short delay seems to be fine.
			messageHandler.postDelayed(new Runnable() {
				public void run() {
					fieldView.drawField();
				}
			}, 50);
		} else {
			fieldView.start();
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// When a game is in progress, pause rather than exit when the back button is pressed.
		// This prevents accidentally quitting the game.
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (isGameInProgress() && pausedMenuView.getVisibility() != View.VISIBLE) {
				pauseGame();
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	/**
	 * Called when preferences activity completes, updates background image and bullet flash settings.
	 */
	void updateFromPreferences() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		boolean flash = prefs.getBoolean(DodgePreferences.FLASHING_COLORS_KEY, false);
		fieldView.setFlashingBullets(flash);

		boolean tilt = prefs.getBoolean(DodgePreferences.TILT_CONTROL_KEY, true);
		fieldView.setTiltControlEnabled(tilt);

		boolean showFPS = prefs.getBoolean(DodgePreferences.SHOW_FPS_KEY, false);
		fieldView.setShowFPS(showFPS);

		Bitmap backgroundBitmap = null;
		if (prefs.getBoolean(DodgePreferences.USE_BACKGROUND_KEY, false)) {
			try {
				File backgroundImageFile = getFileStreamPath(DodgePreferences.BACKGROUND_IMAGE_FILENAME);
				if (backgroundImageFile.isFile()) {
					// Load image scaled to the screen size (the FieldView size would be better but it's not available in onCreate)
					WindowManager windowManager = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
					Display display = windowManager.getDefaultDisplay();

					backgroundBitmap = AndroidUtils.scaledBitmapFromFileWithMinimumSize(backgroundImageFile, display.getWidth(), display.getHeight());
				}
			} catch (Throwable ex) {
				// we shouldn't get out of memory errors, but it's possible with weird images
				android.util.Log.i("DodgeMain", "Failed to read background image", ex);
				backgroundBitmap = null;
			}
		}
		fieldView.setBackgroundBitmap(backgroundBitmap);
	}

	private void gotoPreferences() {
		Intent settingsActivity = new Intent(getBaseContext(), DodgePreferences.class);
		startActivityForResult(settingsActivity, ACTIVITY_PREFERENCES);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		endGameMenuItem = menu.add(R.string.end_game);
		preferencesMenuItem = menu.add(R.string.preferences);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		if (item == endGameMenuItem) {
			doGameOver();
		} else if (item == preferencesMenuItem) {
			gotoPreferences();
		}
		return true;
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);

		switch (requestCode) {
			case ACTIVITY_PREFERENCES:
				updateFromPreferences();
				break;
		}
	}

	boolean inFreePlay() {
		return (lives < 0);
	}

	// methods to store and retrieve the highest normal and free play levels reached, using SharedPreferences
	int bestLevel(boolean freePlay) {
		String key = (freePlay) ? "BestFreePlayLevel" : "BestLevel";
		return getPreferences(MODE_PRIVATE).getInt(key, 0);
	}

	void setBestLevel(boolean freePlay, int val) {
		String key = (freePlay) ? "BestFreePlayLevel" : "BestLevel";
		SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
		editor.putInt(key, val);
		editor.commit();
	}

	int bestScore(boolean freePlay) {
		String key = (freePlay) ? "BestFreePlayScore" : "BestScore";
		return getPreferences(MODE_PRIVATE).getInt(key, 0);
	}

	void setMaxScore(boolean freePlay, int val) {
		String key = (freePlay) ? "BestFreePlayScore" : "BestScore";
		SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
		editor.putInt(key, val);
		editor.commit();
	}

	void recordCurrentLevel(int scoreInt) {
		if (bestScore(inFreePlay()) < scoreInt) {
			setMaxScore(inFreePlay(), scoreInt);
		}
		if (levelManager.getCurrentLevel() > bestLevel(inFreePlay())) {
			setBestLevel(inFreePlay(), levelManager.getCurrentLevel());
		}
	}

	void updateBestLevelFields() {
		int bestNormal = bestLevel(false);
		int bestNormalScore = bestScore(false);
		bestLevelText.setText((bestNormal > 1) ? String.valueOf(bestNormal) : getString(R.string.score_none));
		bestLevelHighScore.setText(bestNormalScore > 0 ? String.valueOf(bestNormalScore) : getString(R.string.score_none));

		int bestFree = bestLevel(true);
		int bestFreeScore = bestScore(true);
		bestFreePlayLevelText.setText((bestFree > 1) ? String.valueOf(bestFree) : getString(R.string.score_none));
		bestFreePlayHighScoreText.setText(bestFreeScore > 0 ? String.valueOf(bestFreeScore) : getString(R.string.score_none));

		continueFreePlayButton.setEnabled(bestFree > 1);
		menuView.forceLayout();
	}


	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	void processMessage(Message m) {
		String action = m.getData().getString("event");
		String scoreText = score.getText().toString();
		if ("goal".equals(action)) {
			scoreInt = 100 - seconds * levelManager.getCurrentLevel() + (levelManager.getCurrentLevel()) * 100;
			if (scoreInt <= 0) {
				score.setText("Score:" + 0);
			} else {
				score.setText("Score:" + scoreInt);

			}

			levelManager.setCurrentLevel(1 + levelManager.getCurrentLevel());
			recordCurrentLevel(scoreInt);
			startTimer();
			synchronized (field) {
				field.setMaxBullets(levelManager.numberOfBulletsForCurrentLevel());
			}
			updateScore();
		} else if ("death".equals(action)) {
			scoreInt = Integer.parseInt(scoreText.substring(scoreText.indexOf(":") + 1));
			if (lives > 0) lives--;
			synchronized (field) {

				if (lives == 0) {
					if (rewardedAd != null) {
						pauseGame();
						AlertDialog.Builder builder = new AlertDialog.Builder(DodgeMain.this, R.style.CustomAlertDialog);
						builder.setTitle("")
								.setCancelable(false)
								.setMessage("If you want to contunie you must watch a video")
								.setPositiveButton("OK", new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {
										rewardAdShow(dialog);
										dialog.dismiss();
									}
								})
								.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {
										dialog.dismiss();
										field.removeDodger();
										doGameOver();
									}
								});

						builder.show();


					} else {
						field.removeDodger();
						doGameOver();
					}
				} else {
					fieldView.startDeathAnimation(field.getDodger().getPosition());
					field.createDodger();
				}
			}
			updateScore();
		}
	}

	private void rewardAdShow(DialogInterface dialog) {
		Activity activityContext = DodgeMain.this;
		rewardedAd.show(activityContext, new OnUserEarnedRewardListener() {
			@Override
			public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
				ISREWARDED=true;
			}
		});

		rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {

			@Override
			public void onAdDismissedFullScreenContent() {
				if (ISREWARDED){
					fieldView.startDeathAnimation(field.getDodger().getPosition());
					field.createDodger();
					ISREWARDED=false;

				}else {
					field.removeDodger();
					doGameOver();
				}
				AdsUtils.loadRewardAds(DodgeMain.this);
			}

			@Override
			public void onAdFailedToShowFullScreenContent(AdError adError) {
				// Called when ad fails to show.
				Log.e("TAG", "Ad failed to show fullscreen content.");
				rewardedAd = null;
			}

			@Override
			public void onAdImpression() {
				// Called when an impression is recorded for an ad.
				Log.d("TAG", "Ad recorded an impression.");
			}

			@Override
			public void onAdShowedFullScreenContent() {
				// Called when ad is shown.
				Log.d("TAG", "Ad showed fullscreen content.");
			}
		});

	}

	boolean isGameInProgress() {
		return field.getDodger() != null;
	}

	void pauseGame() {
		pauseTimer();
		fieldView.stop();
		showMenu();
	}

	void resumeGame() {
		resumeTimer();
		fieldView.start();
		hideMenu();
	}

	void showMenu() {
		if (isGameInProgress()) {
			field.stop();
			menuView.setVisibility(View.GONE);
			pausedMenuView.setVisibility(View.VISIBLE);
			pausedMenuView.requestFocus();
		}
		else {
			updateBestLevelFields();
			menuView.setVisibility(View.VISIBLE);
			pausedMenuView.setVisibility(View.GONE);
			menuView.requestFocus();
		}
	}

	void hideMenu() {
		menuView.setVisibility(View.GONE);
		pausedMenuView.setVisibility(View.GONE);
	}

	void updateScore() {
		levelText.setText(getString(R.string.level_prefix) + " " + levelManager.getCurrentLevel());
		livesText.setText(getString(R.string.lives_prefix) + " " + ((lives>=0) ? ""+lives : getString(R.string.free_play_lives)));
	}

	void doGameOver() {
		timer.cancel();
		field.removeDodger();
		fieldView.start();
		scoreInt=1;
		statusText.setText(getString(R.string.game_over_message));
		icMore.setEnabled(false);
		if(Globals.TIMER_FINISHED && mInterstitialAd != null){
			mInterstitialAd.show(DodgeMain.this);
			mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback(){
				@Override
				public void onAdDismissedFullScreenContent() {
					Globals.TIMER_FINISHED=false;
					Timers.timer().start();
					mInterstitialAd = null;
					AdsUtils.loadGoogleInterstitialAd(DodgeMain.this);
					showMenu();
				}
			});
		}else{
			Log.d("INTERS","sbhsdk");
			showMenu();

		}

	}

	void startGameAtLevelWithLives(int startLevel, int numLives) {
		score.setText("Score:100");
		startTimer();
		levelManager.setCurrentLevel(startLevel);
		field.setMaxBullets(levelManager.numberOfBulletsForCurrentLevel());
		field.createDodger();
		menuView.setVisibility(View.INVISIBLE);
		lives = numLives;
		updateScore();
	}

	void doNewGame() {
		startGameAtLevelWithLives(1, 3);
	}

	void doFreePlay(int startLevel) {
		startGameAtLevelWithLives(startLevel, -1);
	}

	void doAbout() {
		Intent aboutIntent = new Intent(this, DodgeAbout.class);
		this.startActivity(aboutIntent);
	}

	void sendMessage(Map params) {
		Bundle b = new Bundle();
		for(Object key : params.keySet()) {
			b.putString((String)key, (String)params.get(key));
		}
		Message m = messageHandler.obtainMessage();
		m.setData(b);
		messageHandler.sendMessage(m);
	}

	// Field.Delegate methods
	// these occur in a separate thread, so use Handler
	public void dodgerHitByBullet(Field theField) {
		Map params = new HashMap();
		params.put("event", "death");
		sendMessage(params);
	}

	public void dodgerReachedGoal(Field theField) {
		Map params = new HashMap();
		params.put("event", "goal");
		sendMessage(params);
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		finishAffinity();
	}
}