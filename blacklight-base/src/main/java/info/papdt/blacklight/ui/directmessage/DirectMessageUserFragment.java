/* 
 * Copyright (C) 2015 Peter Cai
 *
 * This file is part of BlackLight
 *
 * BlackLight is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * BlackLight is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BlackLight.  If not, see <http://www.gnu.org/licenses/>.
 */

package info.papdt.blacklight.ui.directmessage;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import info.papdt.blacklight.R;
import info.papdt.blacklight.api.remind.RemindApi;
import info.papdt.blacklight.api.remind.RemindApi.Type;
import info.papdt.blacklight.cache.directmessages.DirectMessagesUserApiCache;
import info.papdt.blacklight.support.AsyncTask;
import info.papdt.blacklight.support.Settings;
import info.papdt.blacklight.support.Utility;
import info.papdt.blacklight.support.adapter.DirectMessageUserAdapter;
import info.papdt.blacklight.ui.common.TouchPassView;
import info.papdt.blacklight.ui.main.MainActivity;
import info.papdt.blacklight.ui.statuses.UserTimeLineActivity;

public class DirectMessageUserFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, MainActivity.HeaderProvider {
	private DirectMessagesUserApiCache mApiCache;
	private RecyclerView mList;
	private LinearLayoutManager mManager;
	private View mShadow;
	private SwipeRefreshLayout mSwipeRefresh;
	private DirectMessageUserAdapter mAdapter;
	private boolean mRefreshing = false;
	private int mHeaderHeight = 0, mTranslationY = 0;
	private float mHeaderFactor = 0.0f;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Share the view
		final ViewGroup v = (ViewGroup) inflater.inflate(R.layout.home_timeline, null);
		
		// Initialize
		mList = Utility.findViewById(v, R.id.home_timeline);
		
		mManager = new LinearLayoutManager(getActivity());
		mList.setLayoutManager(mManager);
		
		mSwipeRefresh = new SwipeRefreshLayout(getActivity());

		// Move child to SwipeRefreshLayout, and add SwipeRefreshLayout to root view
		v.removeViewInLayout(mList);
		v.addView(mSwipeRefresh, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		mSwipeRefresh.addView(mList, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

		mSwipeRefresh.setOnRefreshListener(this);
		mSwipeRefresh.setColorScheme(R.color.ptr_green, R.color.ptr_orange, R.color.ptr_red, R.color.ptr_blue);

		mApiCache = new DirectMessagesUserApiCache(getActivity());
		mAdapter = new DirectMessageUserAdapter(getActivity(), mApiCache. mUsers, mList);

		// Content Margin
		if (getActivity() instanceof MainActivity) {
			View target = ((MainActivity) getActivity()).getTabsView();
			View header = new TouchPassView(getActivity(), target);
			RecyclerView.LayoutParams p = new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT,
					Utility.getDecorPaddingTop(getActivity()));
			header.setLayoutParams(p);
			mAdapter.setHeaderView(header);
			v.findViewById(R.id.action_shadow).setVisibility(View.GONE);
		}

		mList.setAdapter(mAdapter);

		mList.setOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrolled(RecyclerView view, int dx, int dy) {
				if (!mRefreshing && mManager.findLastVisibleItemPosition() >= mAdapter.getItemCount() - 5) {
					new Refresher().execute(false);
				}
				
				int deltaY = -dy;
				
				if (mManager.findFirstVisibleItemPosition() == 0) {

					if ((mTranslationY > -mHeaderHeight && deltaY < 0)
						|| (mTranslationY < 0 && deltaY > 0)) {

						mTranslationY += deltaY;
					}

					if (mTranslationY < -mHeaderHeight) {
						mTranslationY = -mHeaderHeight;
					} else if (mTranslationY > 0) {
						mTranslationY = 0;
					}

					View header = mAdapter.getHeaderView();
					mHeaderFactor = Math.abs(mTranslationY) / (float) header.getHeight();

				} else {
					mHeaderFactor = 1f;
				}
				
				((MainActivity) getActivity()).updateHeaderTranslation(mHeaderFactor);
			}
		});
		
		mApiCache.loadFromCache();
		mAdapter.notifyDataSetChangedAndClone();

		v.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				if (getActivity() instanceof MainActivity) {
					mHeaderHeight = ((MainActivity) getActivity()).getHeaderHeight();
					RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) mAdapter.getHeaderView().getLayoutParams();
					lp.height = mHeaderHeight;
					mAdapter.getHeaderView().setLayoutParams(lp);
					mSwipeRefresh.setProgressViewOffset(false, 0, (int) (lp.height * 1.2));
					mSwipeRefresh.invalidate();
					v.getViewTreeObserver().removeGlobalOnLayoutListener(this);
				}
			}
		});
		
		if (mApiCache.mUsers.getSize() == 0) {
			onRefresh();
		}
		
		return v;
	}

	@Override
	public void onRefresh() {
		if (!mRefreshing) {
			new Refresher().execute(true);
		}
	}
	
	@Override
	public float getHeaderFactor() {
		return mHeaderFactor;
	}

	/*@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if (mApiCache.mUsers == null) return;
		if (getActivity() instanceof MainActivity) {
			position--; // Count the header view in
		}

		if (position >= mApiCache.mUsers.getSize()) return;

		Intent i = new Intent();
		i.setAction(Intent.ACTION_MAIN);
		i.setClass(getActivity(), DirectMessageConversationActivity.class);
		i.putExtra("user", mApiCache.mUsers.get(position).user);
		startActivity(i);
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		if (getActivity() instanceof MainActivity) {
			position--;
		}

		Intent i = new Intent();
		i.setAction(Intent.ACTION_MAIN);
		i.setClass(getActivity(), UserTimeLineActivity.class);
		i.putExtra("user", mApiCache.mUsers.get(position).user);
		startActivity(i);

		return true;
	}*/
	
	private class Refresher extends AsyncTask<Boolean, Void, Boolean> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			Utility.clearOngoingUnreadCount(getActivity());
			
			mRefreshing = true;
			mSwipeRefresh.setRefreshing(true);
			mSwipeRefresh.invalidate();
		}
		
		@Override
		protected Boolean doInBackground(Boolean... params) {
			if (params[0]) {
				mApiCache.mUsers.getList().clear();
			}
			
			mApiCache.load(params[0]);
			mApiCache.cache();

			if (params[0]) {
				RemindApi.clearUnread(Type.Dm.str);
			}

			return params[0];
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			
			mRefreshing = false;
			mSwipeRefresh.setRefreshing(false);

			mAdapter.notifyDataSetChangedAndClone();
		}
	}
}
