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

package com.ryan.ryanreader.account;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.ryan.ryanreader.activities.BugReportActivity;
import com.ryan.ryanreader.cache.PersistentCookieStore;
import com.ryan.ryanreader.common.RRError;
import com.ryan.ryanreader.common.UpdateNotifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 单例模式 账户管理
 * 
 * @author ryanlee
 * 
 */
public final class RedditAccountManager extends SQLiteOpenHelper {

	private List<RedditAccount> accountsCache = null;// 账户的缓存，在为空时会从数据库中检索。数据库更新数据时会通知监听器来更新这个缓存
	private RedditAccount defaultAccountCache = null;

	// 匿名用户
	private static final RedditAccount ANON = new RedditAccount("", null, null,
			10);

	private final Context context;

	private final UpdateNotifier<RedditAccountChangeListener> updateNotifier = new UpdateNotifier<RedditAccountChangeListener>() {
		@Override
		protected void notifyListener(final RedditAccountChangeListener listener) {
			listener.onRedditAccountChanged();
		}
	};

	// 数据库信息
	private static final String ACCOUNTS_DB_FILENAME = "accounts.db",
			TABLE = "accounts", FIELD_USERNAME = "username",
			FIELD_COOKIES = "cookies", FIELD_MODHASH = "modhash",
			FIELD_PRIORITY = "priority";// 优先级

	private static final int ACCOUNTS_DB_VERSION = 2;

	// 实例
	private static RedditAccountManager singleton;

	public static synchronized RedditAccountManager getInstance(
			final Context context) {
		if (singleton == null)
			singleton = new RedditAccountManager(
					context.getApplicationContext());
		return singleton;
	}

	public static synchronized RedditAccountManager getInstanceOrNull() {
		return singleton;
	}

	/**
	 * 匿名用户
	 * 
	 * @return
	 */
	public static RedditAccount getAnon() {
		return ANON;
	}

	private RedditAccountManager(final Context context) {
		super(context.getApplicationContext(), ACCOUNTS_DB_FILENAME, null,
				ACCOUNTS_DB_VERSION);
		this.context = context;
	}

	@Override
	public void onCreate(final SQLiteDatabase db) {

		final String queryString = String.format("CREATE TABLE %s ("
				+ "%s TEXT NOT NULL PRIMARY KEY ON CONFLICT REPLACE,"
				+ "%s BLOB," + "%s TEXT," + "%s INTEGER)", TABLE,
				FIELD_USERNAME, FIELD_COOKIES, FIELD_MODHASH, FIELD_PRIORITY);

		db.execSQL(queryString);

		addAccount(getAnon(), db);
	}

	@Override
	public void onUpgrade(final SQLiteDatabase db, final int oldVersion,
			final int newVersion) {

		if (oldVersion == 1 && newVersion == 2) {

			db.execSQL(String.format(
					"UPDATE %s SET %2$s=TRIM(%2$s) WHERE %2$s <> TRIM(%2$s)",
					TABLE, FIELD_USERNAME));

		} else {
			throw new RuntimeException("Invalid accounts DB update: "
					+ oldVersion + " to " + newVersion);
		}
	}

	public synchronized void addAccount(final RedditAccount account) {
		addAccount(account, null);
	}

	/**
	 * @param account
	 * @param inDb
	 *            可以复用
	 */
	private synchronized void addAccount(final RedditAccount account,
			final SQLiteDatabase inDb) {

		final SQLiteDatabase db;
		if (inDb == null)
			db = getWritableDatabase();
		else
			db = inDb;

		final ContentValues row = new ContentValues();

		row.put(FIELD_USERNAME, account.username);
		row.put(FIELD_COOKIES, account.getCookieBytes());
		row.put(FIELD_MODHASH, account.modhash);
		row.put(FIELD_PRIORITY, account.priority);

		db.insert(TABLE, null, row);

		reloadAccounts(db);// 从数据库更新数据
		updateNotifier.updateAllListeners();// 通知所有监听器

		if (inDb == null)
			db.close();
	}

	/**
	 * 获取所有账户
	 * 
	 * @return
	 */
	public synchronized ArrayList<RedditAccount> getAccounts() {

		if (accountsCache == null) {
			final SQLiteDatabase db = getReadableDatabase();
			reloadAccounts(db);
			db.close();
		}

		return new ArrayList<RedditAccount>(accountsCache);
	}

	/**
	 * 根据用户名获得账户信息
	 * 
	 * @param username
	 * @return
	 */
	public RedditAccount getAccount(String username) {

		final ArrayList<RedditAccount> accounts = getAccounts();
		RedditAccount selectedAccount = null;

		for (RedditAccount account : accounts) {
			if (!account.isAnonymous()
					&& account.username.equalsIgnoreCase(username)) {
				selectedAccount = account;
				break;
			}
		}

		return selectedAccount;
	}

	public synchronized RedditAccount getDefaultAccount() {

		if (defaultAccountCache == null) {
			final SQLiteDatabase db = getReadableDatabase();
			reloadAccounts(db);
			db.close();
		}

		return defaultAccountCache;
	}

	/**
	 * 设置默认账户
	 * 
	 * 将优先级-1.
	 * 
	 * @param newDefault
	 */
	public synchronized void setDefaultAccount(final RedditAccount newDefault) {

		final SQLiteDatabase db = getWritableDatabase();

		db.execSQL(String.format(
				"UPDATE %s SET %s=(SELECT MIN(%s)-1 FROM %s) WHERE %s=?",
				TABLE, FIELD_PRIORITY, FIELD_PRIORITY, TABLE, FIELD_USERNAME),
				new String[] { newDefault.username });

		reloadAccounts(db);
		db.close();

		updateNotifier.updateAllListeners();
	}

	/**
	 * 
	 * 重新加载账户信息
	 * 
	 * @param db
	 */
	private synchronized void reloadAccounts(final SQLiteDatabase db) {

		final String[] fields = new String[] { FIELD_USERNAME, FIELD_COOKIES,
				FIELD_MODHASH, FIELD_PRIORITY };

		final Cursor cursor = db.query(TABLE, fields, null, null, null, null,
				FIELD_PRIORITY + " ASC");

		// 更新的结果
		accountsCache = new LinkedList<RedditAccount>();
		defaultAccountCache = null;

		// TODO handle null? can this even happen?
		if (cursor != null) {

			while (cursor.moveToNext()) {

				final String username = cursor.getString(0);
				final byte[] cookies = cursor.getBlob(1);
				final String modhash = cursor.getString(2);
				final long priority = cursor.getLong(3);

				final RedditAccount account;

				try {
					account = new RedditAccount(username, modhash,
							cookies == null ? null : new PersistentCookieStore(
									cookies), priority);

				} catch (IOException e) {
					BugReportActivity.handleGlobalError(context, new RRError(
							null, null, e));
					return;
				}

				accountsCache.add(account);

				if (defaultAccountCache == null
						|| account.priority < defaultAccountCache.priority) {
					defaultAccountCache = account;
				}
			}

			cursor.close();

		} else {
			BugReportActivity.handleGlobalError(context,
					"Cursor was null after query");
		}
	}

	public void addUpdateListener(final RedditAccountChangeListener listener) {
		updateNotifier.addListener(listener);
	}

	/**
	 * 删除用户信息
	 * 
	 * @param account
	 */
	public void deleteAccount(RedditAccount account) {

		final SQLiteDatabase db = getWritableDatabase();
		db.delete(TABLE, FIELD_USERNAME + "=?",
				new String[] { account.username });
		reloadAccounts(db);
		updateNotifier.updateAllListeners();
		db.close();
	}
}
