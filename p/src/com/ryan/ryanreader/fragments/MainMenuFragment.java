/*******************************************************************************
 * This file is part of RedReader.
 *
 * RedReader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RedReader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RedReader.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package com.ryan.ryanreader.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import org.apache.http.StatusLine;
import org.holoeverywhere.LayoutInflater;
import org.holoeverywhere.app.Fragment;
import org.holoeverywhere.widget.LinearLayout;
import org.holoeverywhere.widget.ListView;
import com.ryan.ryanreader.R;

import com.ryan.ryanreader.account.RedditAccount;
import com.ryan.ryanreader.account.RedditAccountManager;
import com.ryan.ryanreader.activities.BugReportActivity;
import com.ryan.ryanreader.adapters.MainMenuAdapter;
import com.ryan.ryanreader.adapters.MainMenuSelectionListener;
import com.ryan.ryanreader.cache.CacheManager;
import com.ryan.ryanreader.cache.CacheRequest;
import com.ryan.ryanreader.cache.RequestFailureType;
import com.ryan.ryanreader.common.General;
import com.ryan.ryanreader.common.RRError;
import com.ryan.ryanreader.reddit.APIResponseHandler;
import com.ryan.ryanreader.reddit.RedditAPI;
import com.ryan.ryanreader.reddit.things.RedditSubreddit;
import com.ryan.ryanreader.views.liststatus.ErrorView;
import com.ryan.ryanreader.views.liststatus.LoadingView;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class MainMenuFragment extends Fragment implements
		MainMenuSelectionListener {

	private MainMenuAdapter adapter;

	private LinearLayout notifications;
	private LoadingView loadingView;

	private boolean force;

	/**
	 * FRONTPAGE 首页 CUSTOM 查找 ALL 所有话题
	 * 
	 * @author ryanlee
	 * 
	 */
	public static enum MainMenuAction {
		FRONTPAGE, PROFILE, INBOX, LIKED, SAVED, HIDDEN, CUSTOM, ALL
	}

	/**
	 * 返回MainMenuFragment的一个实例
	 * 
	 * @param force
	 * @return
	 */
	public static MainMenuFragment newInstance(final boolean force) {

		final MainMenuFragment f = new MainMenuFragment();

		// Bundle的长度为1
		final Bundle bundle = new Bundle(1);
		bundle.putBoolean("force", force);
		f.setArguments(bundle);

		/*
		 * public void setArguments(Bundle args)
		 * 
		 * 该方法给该Fragment对象提供构建参数。它只。在Fragment对象被绑定到它Activity对象之前被调用，
		 * 也就是说在构建该Fragment对象之后，应该立即调用。该方法提供的参数会在Fragment对象销毁和创建期间被保留。
		 */
		return f;
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		force = getArguments().getBoolean("force");
	}

	/*
	 * 返回这个Fragment的布局
	 */
	@Override
	public View onCreateView(final LayoutInflater inflater,
			final ViewGroup container, final Bundle savedInstanceState) {

		// TODO load menu position?

		final Context context;

		// 优先使用容器
		if (container != null) {
			context = container.getContext(); // TODO just use the inflater's
												// context in every case?
		} else {
			context = inflater.getContext();
		}

		// 获得默认账户
		final RedditAccount user = RedditAccountManager.getInstance(context)
				.getDefaultAccount();

		final LinearLayout outer = new LinearLayout(context);

		// 垂直
		outer.setOrientation(LinearLayout.VERTICAL);

		notifications = new LinearLayout(context);
		notifications.setOrientation(LinearLayout.VERTICAL);

		loadingView = new LoadingView(context, R.string.download_waiting, true,
				true);

		final ListView lv = new ListView(context);
		lv.setDivider(null);

		// listview尾部添加一个进度条
		lv.addFooterView(notifications);

		final int paddingPx = General.dpToPixels(context, 8);
		lv.setPadding(paddingPx, 0, paddingPx, 0);

		adapter = new MainMenuAdapter(context, user, this);
		lv.setAdapter(adapter);

		lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(final AdapterView<?> adapterView,
					final View view, final int position, final long id) {
				adapter.clickOn(position);
			}
		});

		final AtomicReference<APIResponseHandler.SubredditResponseHandler> accessibleSubredditResponseHandler = new AtomicReference<APIResponseHandler.SubredditResponseHandler>(
				null);

		final APIResponseHandler.SubredditResponseHandler responseHandler = new APIResponseHandler.SubredditResponseHandler(
				context) {

			@Override
			protected void onDownloadNecessary() {
				new Handler(Looper.getMainLooper()).post(new Runnable() {
					public void run() {
						notifications.addView(loadingView);
					}
				});
			}

			@Override
			protected void onDownloadStarted() {
				loadingView.setIndeterminate(R.string.download_subreddits);
			}

			@Override
			protected void onSuccess(final List<RedditSubreddit> result,
					final long timestamp) {

				if (result.size() == 0) {
					// Just get the defaults instead
					new Handler(Looper.getMainLooper()).post(new Runnable() {
						public void run() {
							notifications.removeView(loadingView);
							RedditAPI.getUserSubreddits(
									CacheManager.getInstance(context),
									accessibleSubredditResponseHandler.get(),
									RedditAccountManager.getAnon(),
									force ? CacheRequest.DownloadType.FORCE
											: CacheRequest.DownloadType.IF_NECESSARY,
									force, context);
						}
					});

				} else {

					adapter.setSubreddits(result);

					if (loadingView != null)
						loadingView.setDone(R.string.download_done);
				}
			}

			@Override
			protected void onCallbackException(final Throwable t) {
				BugReportActivity.handleGlobalError(context, t);
			}

			@Override
			protected void onFailure(final RequestFailureType type,
					final Throwable t, final StatusLine status,
					final String readableMessage) {

				if (loadingView != null)
					loadingView.setDone(R.string.download_failed);
				final RRError error = General.getGeneralErrorForFailure(
						context, type, t, status);

				new Handler(Looper.getMainLooper()).post(new Runnable() {
					public void run() {
						notifications.addView(new ErrorView(
								getSupportActivity(), error));
					}
				});
			}

			@Override
			protected void onFailure(final APIFailureType type) {

				if (loadingView != null)
					loadingView.setDone(R.string.download_failed);
				final RRError error = General.getGeneralErrorForFailure(
						context, type);

				new Handler(Looper.getMainLooper()).post(new Runnable() {
					public void run() {
						notifications.addView(new ErrorView(
								getSupportActivity(), error));
					}
				});
			}
		};

		accessibleSubredditResponseHandler.set(responseHandler);

		RedditAPI.getUserSubreddits(CacheManager.getInstance(context),
				responseHandler, user, force ? CacheRequest.DownloadType.FORCE
						: CacheRequest.DownloadType.IF_NECESSARY, force,
				context);

		outer.addView(lv);
		lv.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;

		return outer;
	}

	@Override
	public void onSaveInstanceState(final Bundle outState) {
		// TODO save menu position?
	}

	public void onSelected(final MainMenuAction type, final String name) {
		((MainMenuSelectionListener) getSupportActivity()).onSelected(type,
				name);
	}

	public void onSelected(final RedditSubreddit subreddit) {
		((MainMenuSelectionListener) getSupportActivity())
				.onSelected(subreddit);
	}
}
