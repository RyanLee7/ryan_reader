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
import android.widget.AdapterView;
import org.holoeverywhere.app.AlertDialog;
import org.holoeverywhere.app.Dialog;
import org.holoeverywhere.app.DialogFragment;
import org.holoeverywhere.widget.ListView;
import com.ryan.ryanreader.R;

import com.ryan.ryanreader.account.RedditAccountChangeListener;
import com.ryan.ryanreader.account.RedditAccountManager;
import com.ryan.ryanreader.activities.SessionChangeListener;
import com.ryan.ryanreader.adapters.SessionListAdapter;
import com.ryan.ryanreader.cache.CacheEntry;
import com.ryan.ryanreader.common.General;

import java.net.URI;
import java.util.UUID;

public class SessionListDialog extends DialogFragment implements RedditAccountChangeListener {

	private URI url;
	private UUID current;
	private SessionChangeListener.SessionChangeType type;

	private ListView lv;

	// Workaround for HoloEverywhere bug?
	private volatile boolean alreadyCreated = false;

	public static SessionListDialog newInstance(final URI url, final UUID current, final SessionChangeListener.SessionChangeType type) {

		final SessionListDialog dialog = new SessionListDialog();

		final Bundle args = new Bundle(3);
		args.putString("url", url.toString());
		if(current != null) args.putString("current", current.toString());
		args.putString("type", type.name());
		dialog.setArguments(args);

		return dialog;
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		url = General.uriFromString(getArguments().getString("url"));

		if(getArguments().containsKey("current")) {
			current = UUID.fromString(getArguments().getString("current"));
		} else {
			current = null;
		}

		type = SessionChangeListener.SessionChangeType.valueOf(getArguments().getString("type"));
	}

	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState) {

		if(alreadyCreated) return getDialog();
		alreadyCreated = true;

		super.onCreateDialog(savedInstanceState);

		final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(getSupportActivity().getString(R.string.options_past));

		final Context context = getSupportActivity();

		lv = new ListView(context);
		builder.setView(lv);

		lv.setAdapter(new SessionListAdapter(context, url, current));

		RedditAccountManager.getInstance(context).addUpdateListener(this);

		lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> adapterView, View view, int position, final long id) {

				final CacheEntry ce = (CacheEntry) lv.getItemAtPosition(position);

				if(ce == null) {
					((SessionChangeListener) getSupportActivity()).onSessionRefreshSelected(type);

				} else {
					((SessionChangeListener) getSupportActivity()).onSessionSelected(ce.session, type);
				}

				dismiss();
			}
		});

		builder.setNeutralButton(getSupportActivity().getString(R.string.dialog_close), null);

		return builder.create();
	}

	public void onRedditAccountChanged() {
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			public void run() {
				lv.setAdapter(new SessionListAdapter(getSupportActivity(), url, current));
			}
		});
	}
}
