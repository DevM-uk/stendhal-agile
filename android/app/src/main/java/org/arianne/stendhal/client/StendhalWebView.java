/***************************************************************************
 *                     Copyright © 2022 - Arianne                          *
 ***************************************************************************
 ***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
package org.arianne.stendhal.client;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

import org.arianne.stendhal.client.sound.MusicPlayer;


public class StendhalWebView {

	// FIXME: page should be set to character select if focus is lost

	private static DebugLog logger = DebugLog.get();
	private static Notifier notifier = Notifier.get();

	private static StendhalWebView instance;

	private boolean testing = false;
	private static PageId currentPage;
	private Boolean debugging;
	private final String defaultServer = "https://stendhalgame.org/";
	private String clientUrlSuffix = "client";

	private final Context ctx;
	private WebView clientView;
	private ImageView splash;
	//private InputMethodManager imm;

	//private String currentHTML;

	private final int doubleTapThreshold = 300;
	private long timestampTouchUp = 0;
	private long timestampTouchUpPrev = 0;
	private int tapCount = 0;
	// denotes previous touch was remapped to mouse event
	private boolean touchOverridden = false;

	public enum PageId {
		TITLE,
		WEBCLIENT,
		OTHER;
	}


	public static StendhalWebView get() {
		return instance;
	}

	public StendhalWebView(final Context ctx) {
		instance = this;
		this.ctx = ctx;
		final AppCompatActivity mainActivity = (AppCompatActivity) ctx;

		// FIXME: need to manually initialize WebView to override onCreateInputConnection
		clientView = (WebView) mainActivity.findViewById(R.id.clientWebView);
		clientView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
		/*
		clientView = new WebView(mainActivity) {
			@Override
			public InputConnection onCreateInputConnection(final EditorInfo outAttrs) {
				outAttrs.inputType = InputType.TYPE_NULL;
				return new BaseInputConnection(getView(), false);
			}
		};
		*/

		splash = (ImageView) mainActivity.findViewById(R.id.splash);
		splash.setBackgroundColor(android.graphics.Color.TRANSPARENT);

		if (debugEnabled()) {
			// make WebView debuggable for debug builds
			clientView.setWebContentsDebuggingEnabled(true);
		}

		final WebSettings viewSettings = clientView.getSettings();

		viewSettings.setJavaScriptEnabled(true);

		// keep elements in position in portrait mode
		viewSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);// <-- SINGLE_COLUMN deprecated
		viewSettings.setLoadWithOverviewMode(true);
		viewSettings.setUseWideViewPort(true);

		// disable zoom
		viewSettings.setSupportZoom(false);
		viewSettings.setBuiltInZoomControls(false);
		viewSettings.setDisplayZoomControls(false);

		/*
		clientView.addJavascriptInterface(new JSInterface() {
			@Override
			protected void onFire() {
				currentHTML = getHTML();
			}
		}, "JSI");
		*/

		initWebViewClient();
		initTouchHandler();
		initKeyboardHandler();

		loadTitleScreen();
	}

	private void loadUrl(final String url) {
		clientView.loadUrl(url);
	}

	/**
	 * Shows initial splash screen.
	 */
	public void loadTitleScreen() {
		splash.setImageResource(R.drawable.splash);
		loadUrl("about:blank");
		Menu.get().show();
	}

	private void initWebViewClient() {
		clientView.setWebViewClient(new WebViewClient() {
			/* handle changing URLs */
			@Override
			public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
				if (!isInternalUrl(url)) {
					// FIXME: should we ask for confirmation?
					((Activity) ctx).startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
					return true;
				}

				view.loadUrl(checkClientUrl(url));
				return false;
			}

			@Override
			public void onPageStarted(final WebView view, final String url, final Bitmap favicon) {
				DebugLog.debug("loading URL: " + url);

				MusicPlayer.stopMusic();

				super.onPageStarted(view, url, favicon);
			}

			@Override
			public void onPageFinished(final WebView view, final String url) {
				super.onPageFinished(view, url);

				if (isClientUrl(url)) {
					currentPage = PageId.WEBCLIENT;
				} else if (url.equals("") || url.equals("about:blank")) {
					currentPage = PageId.TITLE;
					if (PreferencesActivity.getBoolean("title_music", true)) {
						playTitleMusic();
					}
				} else {
					currentPage = PageId.OTHER;
				}
				Menu.get().updateButtons();

				DebugLog.debug("page id: " + currentPage);
			}
		});
	}

	private void initTouchHandler() {
		clientView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(final View view, final MotionEvent event) {
				if (touchOverridden) {
					// reset touch event state
					touchOverridden = false;
					return false;
				}

				final MotionEvent.PointerProperties[] props = {new MotionEvent.PointerProperties()};
				event.getPointerProperties(0, props[0]);

				if (isGameActive() && PreferencesActivity.getBoolean("map_touches", false)
						&& (props[0].toolType == MotionEvent.TOOL_TYPE_FINGER
							|| props[0].toolType == MotionEvent.TOOL_TYPE_STYLUS)) {
					Integer action = event.getAction();

					/*
					switch (action) {
						case MotionEvent.ACTION_DOWN:
							action = MotionEvent.ACTION_BUTTON_PRESS;
							break;
						case MotionEvent.ACTION_UP:
							action = MotionEvent.ACTION_BUTTON_RELEASE;
							break;
					}
					*/

					DebugLog.debug("mapping touch to mouse event");

					props[0].toolType = MotionEvent.TOOL_TYPE_MOUSE;

					final MotionEvent.PointerCoords[] coords = {new MotionEvent.PointerCoords()};
					event.getPointerCoords(0, coords[0]);

					touchOverridden = true;

					view.dispatchTouchEvent(MotionEvent.obtain(event.getDownTime(),
						event.getEventTime(), event.getAction(), event.getPointerCount(), props,
						coords, event.getMetaState(), MotionEvent.BUTTON_PRIMARY,
						event.getXPrecision(), event.getYPrecision(), event.getDeviceId(),
						event.getEdgeFlags(), event.getSource(), event.getFlags()));

					return true; // consume old event
				}

				return false;
			}
		});
	}

	private void initKeyboardHandler() {
		//imm = (InputMethodManager) ((Activity) ctx).getSystemService(Context.INPUT_METHOD_SERVICE);

		clientView.setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(final View view, final int keyCode, final KeyEvent event) {
				// FIXME: cannot catch soft keyboard events without overriding WebView.onCreateInputConnection

				return false;
			}
		});
	}

	/**
	 * Checks if a URL is a link to one of the web clients.
	 *
	 * @param url
	 *     URL to be checked.
	 * @return
	 *     <code>true</code> if url links to "client" or "testclient".
	 */
	private boolean isClientUrl(final String url) {
		final String custom_client = PreferencesActivity.getString("client_url").trim();
		if (!custom_client.equals("")) {
			return url.contains(custom_client);
		}

		return url.contains("stendhalgame.org/client/")
			|| url.contains("stendhalgame.org/testclient/");
	}

	/**
	 * Formats client URL for currently selected server.
	 *
	 * @param url
	 *     URL to be checked.
	 * @return
	 *     URL to be loaded.
	 */
	private String checkClientUrl(String url) {
		if (isClientUrl(url)) {
			String replaceSuffix = "/testclient/";
			if (testing) {
				replaceSuffix = "/client/";
			}

			url = url.replace(replaceSuffix, "/" + clientUrlSuffix + "/");
		}

		return url;
	}

	/**
	 * Checks if requested URL is whitelisted to be opened within WebView client.
	 *
	 * @param url
	 *     URL to be checked.
	 * @return
	 *     <code>true</code> if URL is under domain stendhalgame.org or localhost.
	 */
	private boolean isInternalUrl(final String url) {
		final String domain = getDomain(url);
		final String cs = checkCustomServer();

		if (cs != null) {
			return domain.startsWith(getDomain(cs));
		} else {
			return domain.startsWith("stendhalgame.org") || domain.startsWith("localhost");
		}
	}

	private String getDomain(final String url) {
		return url.replaceAll("^https://", "").replaceAll("^http://", "")
			.replaceAll("^www\\.", "");
	}

	private String checkCustomServer() {
		final String cs = PreferencesActivity.getString("client_url", "").trim();

		if (cs.equals("")) {
			return null;
		}

		return cs;
	}

	/**
	 * Opens a message dialog for user to choose between main & test servers.
	 */
	private void selectServer() {
		final AlertDialog.Builder builder = new AlertDialog.Builder((Activity) ctx);
		builder.setMessage("Select a server");

		builder.setPositiveButton("Main", new DialogInterface.OnClickListener() {
			public void onClick(final DialogInterface dialog, final int id) {
				testing = false;
				dialog.cancel();
				onSelectServer();
			}
		});

		builder.setNegativeButton("Testing", new DialogInterface.OnClickListener() {
			public void onClick(final DialogInterface dialog, final int id) {
				testing = true;
				clientUrlSuffix = "testclient";
				dialog.cancel();
				onSelectServer();
			}
		});

		final AlertDialog selectServer = builder.create();
		selectServer.show();
	}

	private void onSelectServer() {
		// remove splash image
		splash.setImageResource(android.R.color.transparent);

		final String custom_page = checkCustomServer();
		if (custom_page != null) {
			logger.debug("Connecing to custom page: " + custom_page);

			loadUrl(custom_page);
		} else {
			// initial page
			loadUrl(defaultServer + "account/mycharacters.html");

			if (testing) {
				logger.debug("Connecting to test server");
			} else {
				logger.debug("Connecting to main server");

				notifier.showMessage("CAUTION: This software is in early development and not recommended"
					+ " for use on the main server. Proceed with caution.", false);
			}
		}

		currentPage = PageId.OTHER;
	}

	/**
	 * Attempts to connect to client host.
	 */
	public void loadLogin() {
		if (debugEnabled()) {
			// debug builds support choosing between main & test server
			selectServer();
		} else {
			onSelectServer();
		}
	}

	public static boolean onTitleScreen() {
		return currentPage == PageId.TITLE;
	}

	public static boolean isGameActive() {
		return currentPage == PageId.WEBCLIENT;
	}

	public static PageId getCurrentPageId() {
		return currentPage;
	}

	public static void playTitleMusic(String musicId) {
		if (musicId == null) {
			musicId = PreferencesActivity.getString("song_list");
		}

		int id = R.raw.title_01;
		switch (musicId) {
			case "title_02":
				id = R.raw.title_02;
				break;
			case "title_03":
				id = R.raw.title_03;
				break;
			case "title_04":
				id = R.raw.title_04;
				break;
			case "title_05":
				id = R.raw.title_05;
				break;
		}

		DebugLog.debug("playing music: " + musicId);

		MusicPlayer.playMusic(id, true);
	}

	public static void playTitleMusic() {
		playTitleMusic(null);
	}

	/**
	 * Reloads current page.
	 */
	public void reload() {
		clientView.reload();
	}

	/**
	 * Checks if this is a debug build.
	 *
	 * @return
	 *     <code>true</code> if debug flag set.
	 */
	private boolean debugEnabled() {
		if (debugging != null) {
			return debugging;
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			debugging = (((Activity) ctx).getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
		} else {
			debugging = false;
		}

		return debugging;
	}
}
