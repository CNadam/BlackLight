/* 
 * Copyright (C) 2014 Peter Cai
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

package info.papdt.blacklight.ui.friendships;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import info.papdt.blacklight.R;
import info.papdt.blacklight.api.friendships.FriendsApi;
import info.papdt.blacklight.model.UserListModel;
import info.papdt.blacklight.support.AsyncTask;
import info.papdt.blacklight.support.Utility;
import info.papdt.blacklight.support.adapter.UserAdapter;
import info.papdt.blacklight.ui.main.MainActivity;
import info.papdt.blacklight.ui.statuses.UserTimeLineActivity;

public class FriendsFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
	private String mUid;
	protected UserListModel mUsers;
	private int mNextCursor = 0;
	private boolean mRefreshing = false;
	protected boolean mNeedHeader = true;
	
	private RecyclerView mList;
	private LinearLayoutManager mManager;
	private UserAdapter mAdapter;
	private SwipeRefreshLayout mSwipeRefresh;
    private boolean mIsFriends;
	
	public FriendsFragment() {
		init();
	}
	
	public FriendsFragment(String uid, boolean friends) {
		Bundle args = new Bundle();
		args.putCharSequence("uid", uid);
        args.putBoolean("isFriends", friends);
		setArguments(args);
		init();
	}
	
	private void init() {
		if (getArguments() != null) {
			mUid = getArguments().getCharSequence("uid").toString();
            mIsFriends = getArguments().getBoolean("isFriends");
		} else {
			mUid = null;
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Share the layout of Home Time Line
		ViewGroup v = (ViewGroup) inflater.inflate(R.layout.home_timeline, null);
		
		// Initialize
		mList = Utility.findViewById(v, R.id.home_timeline);
		mManager = new LinearLayoutManager(getActivity());
		mList.setLayoutManager(mManager);

		// Init
		mUsers = new UserListModel();
		mSwipeRefresh = new SwipeRefreshLayout(getActivity());
		mAdapter = new UserAdapter(getActivity(), mUsers, mList);

		// Content Margin
		if (getActivity() instanceof MainActivity && mNeedHeader) {
			View header = new View(getActivity());
			RecyclerView.LayoutParams p = new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT,
					Utility.getDecorPaddingTop(getActivity()));
			header.setLayoutParams(p);
			mAdapter.setHeaderView(header);
			mSwipeRefresh.setProgressViewOffset(false, 0, (int) (p.height * 1.2));
		}

		mList.setAdapter(mAdapter);
		
		// Move child to SwipeRefreshLayout, and add SwipeRefreshLayout to root view
		v.removeViewInLayout(mList);
		v.addView(mSwipeRefresh, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		mSwipeRefresh.addView(mList, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

		mSwipeRefresh.setOnRefreshListener(this);
		mSwipeRefresh.setColorScheme(R.color.ptr_green, R.color.ptr_orange, R.color.ptr_red, R.color.ptr_blue);
		mList.setOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrolled(RecyclerView view, int dx, int dy) {
				if (!mRefreshing && mManager.findLastVisibleItemPosition() >= mAdapter.getItemCount() - 5) {
					new Refresher().execute(false);
				}
			}
		});
		
		v.findViewById(R.id.action_shadow).bringToFront();

		onRefresh();
		
		return v;
	}

	@Override
	public void onRefresh() {
		if (!mRefreshing) {
			new Refresher().execute(true);
		}
	}

	/*@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if (getActivity() instanceof MainActivity) {
			position--; // Count the header view in
		}

		Intent i = new Intent();
		i.setAction(Intent.ACTION_MAIN);
		i.setClass(getActivity(), UserTimeLineActivity.class);
		i.putExtra("user", mUsers.get(position));
		startActivity(i);
	}*/
	
	protected void doRefresh(boolean param) {
		if (param) {
			mNextCursor = 0;
			mUsers.getList().clear();
		}

        UserListModel usr;
        if(mIsFriends) usr = FriendsApi.getFriendsOf(mUid, 50, mNextCursor);
        else usr = FriendsApi.getFollowersOf(mUid, 50, mNextCursor);

		if (usr != null) {
			int nextCursor = Integer.parseInt(usr.next_cursor);
			if (param || mNextCursor != 0) {
				mNextCursor = nextCursor;
				mUsers.addAll(param, usr);
			}
		}
		
	}
	
	private class Refresher extends AsyncTask<Boolean, Void, Boolean> {
		@Override
		protected void onPreExecute() {
			mRefreshing = true;
			mSwipeRefresh.setRefreshing(true);
			mSwipeRefresh.invalidate();
		}
		
		@Override
		protected Boolean doInBackground(Boolean... params) {
			doRefresh(params[0]);
			
			return params[0];
		}

		@Override
		protected void onPostExecute(Boolean result) {
			mAdapter.notifyDataSetChangedAndClone();
			
			mRefreshing = false;
			mSwipeRefresh.setRefreshing(false);
		}
	}
}
