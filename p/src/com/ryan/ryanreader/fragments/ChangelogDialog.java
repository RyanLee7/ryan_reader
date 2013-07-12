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
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.text.Html;
import android.view.View;

import org.holoeverywhere.widget.LinearLayout;
import org.holoeverywhere.widget.TextView;
import com.ryan.ryanreader.R;

import com.ryan.ryanreader.common.General;
import com.ryan.ryanreader.views.list.ListSectionHeader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public final class ChangelogDialog extends PropertiesDialog {

	public static ChangelogDialog newInstance() {
		return new ChangelogDialog();
	}

	@Override
	protected String getTitle(Context context) {
		return context.getString(R.string.title_changelog);
	}

	@Override
	protected void prepare(Context context, LinearLayout items) {

		final int outerPaddingPx = General.dpToPixels(context, 12);

		// 设置items里的控件与父控件边界的距离
		items.setPadding(outerPaddingPx, 0, outerPaddingPx, outerPaddingPx);

		try {
			final BufferedReader br = new BufferedReader(new InputStreamReader(
					context.getAssets().open("changelog.txt")));

			int curVersionCode = -1;
			String curVersionName = null;

			boolean firstInList = true;

			String line;
			// 按格式解析文件
			// 38/1.7.4
			// Bugfix for user profile dialog crash
			// Minor appearance improvement
			while ((line = br.readLine()) != null) {

				if (line.length() == 0) {

					curVersionCode = -1;
					curVersionName = null;

				} else if (curVersionName == null) {

					final String[] lineSplit = line.split("/");
					curVersionCode = Integer.parseInt(lineSplit[0]);
					curVersionName = lineSplit[1];

					final ListSectionHeader header = new ListSectionHeader(
							context);
					header.reset(curVersionName);
					header.setColor(Color.rgb(0x00, 0x99, 0xCC));
					items.addView(header);

				} else {

					final LinearLayout bulletItem = new LinearLayout(context);
					final int paddingPx = General.dpToPixels(context, 6);
					bulletItem.setPadding(paddingPx, paddingPx, paddingPx, 0);

					final TextView bullet = new TextView(context);
					bullet.setText("•  ");
					bulletItem.addView(bullet);

					final TextView text = new TextView(context);
					text.setText(line);
					bulletItem.addView(text);

					items.addView(bulletItem);
				}

			}

			// 加入注册
			final LinearLayout accessItem = new LinearLayout(context);
			final int paddingPx = General.dpToPixels(context, 6);
			accessItem.setPadding(paddingPx, paddingPx, paddingPx, 0);

			final TextView bullet = new TextView(context);
			bullet.setText("•  ");
			accessItem.addView(bullet);

			final TextView text = new TextView(context);
			String redditLink = "<a href=\"#\"> 访问reddit.com	</a>";
			text.setText(Html.fromHtml(redditLink));
			text.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					Intent intent = new Intent();
					intent.setAction("android.intent.action.VIEW");
					Uri content_url = Uri.parse("http://www.reddit.com");
					intent.setData(content_url);
					startActivity(intent);
				}
			});

			accessItem.addView(text);

			items.addView(accessItem);

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
