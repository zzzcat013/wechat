/**
 * wechatdonal
 */
package im;

import im.model.IMMessage;
import im.model.Notice;

import java.util.List;

import tools.DateUtil;
import tools.Logger;
import bean.JsonMessage;
import bean.UserInfo;

import com.donal.wechat.R;
import com.nostra13.universalimageloader.core.DisplayImageOptions;

import config.CommonValue;
import config.FriendManager;
import config.MessageManager;
import config.NoticeManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * wechat
 *
 * @author donal
 *
 */
public class Chating extends AChating{
	private MessageListAdapter adapter = null;
	private EditText messageInput = null;
	private Button messageSendBtn = null;
	private ListView listView;
	private int recordCount;
	private UserInfo user;// 聊天人
	private String to_name;
	private Notice notice;
	
	private int firstVisibleItem;
	private int currentPage = 1;
	private int objc;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.chating);
		init();
		user = FriendManager.getInstance(context).getFriend(to.split("@")[0]);
	}
	
	private void init() {

		listView = (ListView) findViewById(R.id.chat_list);
		listView.setCacheColorHint(0);
		adapter = new MessageListAdapter(Chating.this, getMessages(),
				listView);
		listView.setAdapter(adapter);
		
		listView.setOnScrollListener(new OnScrollListener() {
			
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				switch (scrollState) {
				case SCROLL_STATE_FLING:
					break;
				case SCROLL_STATE_IDLE:
					if (firstVisibleItem == 0) {
						int num = addNewMessage(++currentPage);
						if (num > 0) {
							adapter.refreshList(getMessages());
							listView.setSelection(num-1);
						}
					}
					break;
				case SCROLL_STATE_TOUCH_SCROLL:
					closeInput();
					break;
				}
			}
			
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
				Chating.this.firstVisibleItem = firstVisibleItem;
			}
		});

		messageInput = (EditText) findViewById(R.id.chat_content);
		messageInput.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				listView.setSelection(getMessages().size()-1);
			}
		});
		messageSendBtn = (Button) findViewById(R.id.chat_sendbtn);
		messageSendBtn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				String message = messageInput.getText().toString();
				if ("".equals(message)) {
					Toast.makeText(Chating.this, "不能为空",
							Toast.LENGTH_SHORT).show();
				} else {

					try {
						sendMessage(message);
						messageInput.setText("");
					} catch (Exception e) {
						showToast("信息发送失败");
						messageInput.setText(message);
					}
					closeInput();
				}
				listView.setSelection(getMessages().size()-1);
			}
		});
	}

	@Override
	protected void receiveNotice(Notice notice) {
		this.notice = notice;
	}
	
	@Override
	protected void receiveNewMessage(IMMessage message) {
		
	}

	@Override
	protected void refreshMessage(List<IMMessage> messages) {
		adapter.refreshList(messages);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		recordCount = MessageManager.getInstance(context)
				.getChatCountWithSb(to);
		adapter.refreshList(getMessages());
		listView.setSelection(getMessages().size()-1);
	}
	
	private class MessageListAdapter extends BaseAdapter {

		class ViewHoler {
			TextView timeTV;
			
			RelativeLayout leftLayout;
			ImageView leftAvatar;
			TextView leftNickname;
			TextView leftText;
			
			RelativeLayout rightLayout;
			RelativeLayout rightFrame;
			ImageView rightAvatar;
			TextView rightNickname;
			TextView rightText;
			ProgressBar rightProgress;
		}
		
		private List<IMMessage> items;
		private Context context;
		private ListView adapterList;
		private LayoutInflater inflater;

		DisplayImageOptions options;
		
		public MessageListAdapter(Context context, List<IMMessage> items,
				ListView adapterList) {
			this.context = context;
			this.items = items;
			this.adapterList = adapterList;
			inflater = LayoutInflater.from(context);
			options = new DisplayImageOptions.Builder()
			.showImageOnLoading(R.drawable.avatar_placeholder)
			.showImageForEmptyUri(R.drawable.avatar_placeholder)
			.showImageOnFail(R.drawable.avatar_placeholder)
			.cacheInMemory(true)
			.cacheOnDisc(true)
			.bitmapConfig(Bitmap.Config.RGB_565)
			.build();
		}

		public void refreshList(List<IMMessage> items) {
			this.items = items;
			this.notifyDataSetChanged();
			
		}

		@Override
		public int getCount() {
			return items == null ? 0 : items.size();
		}

		@Override
		public Object getItem(int position) {
			return items.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHoler cell = null;
			if (convertView == null) {
				cell = new ViewHoler();
				convertView = inflater.inflate(R.layout.listviewcell_chat_normal, null);
				cell.timeTV = (TextView) convertView.findViewById(R.id.textview_time);
				cell.leftLayout = (RelativeLayout) convertView.findViewById(R.id.layout_left);
				cell.leftAvatar = (ImageView) convertView.findViewById(R.id.image_portrait_l);
				cell.leftNickname = (TextView) convertView.findViewById(R.id.textview_name_l);
				cell.leftText = (TextView) convertView.findViewById(R.id.textview_content_l);
						
				cell.rightLayout = (RelativeLayout) convertView.findViewById(R.id.layout_right);
				cell.rightFrame = (RelativeLayout) convertView.findViewById(R.id.layout_content_r);
				cell.rightAvatar = (ImageView) convertView.findViewById(R.id.image_portrait_r);
				cell.rightNickname = (TextView) convertView.findViewById(R.id.textview_name_r);
				cell.rightText = (TextView) convertView.findViewById(R.id.textview_content_r);
				cell.rightProgress = (ProgressBar) convertView.findViewById(R.id.view_progress_r);
				convertView.setTag(cell);
			}
			else {
				cell = (ViewHoler) convertView.getTag();
			}
			IMMessage message = items.get(position);
			cell.leftLayout.setVisibility(message.getMsgType() == 0? View.VISIBLE:View.INVISIBLE);
			cell.rightLayout.setVisibility(message.getMsgType() == 0? View.INVISIBLE:View.VISIBLE);
			String content = message.getContent();
			imageLoader.displayImage(CommonValue.BASE_URL+user.userHead, cell.leftAvatar, options);
			imageLoader.displayImage(CommonValue.BASE_URL+ appContext.getLoginUserHead(), cell.rightAvatar, options);
			try {
				JsonMessage msg = JsonMessage.parse(content);
				cell.leftText.setText(msg.text);
				cell.rightText.setText(msg.text);
			} catch (Exception e) {
				cell.leftText.setText(content);
				cell.rightText.setText(content);
				
			}
			String currentTime = message.getTime();
			String previewTime = (position - 1) >= 0 ? items.get(position-1).getTime() : "0";
			try {
				long time1 = Long.valueOf(currentTime);
				long time2 = Long.valueOf(previewTime);
				if ((time1-time2) >= 5 * 60 ) {
					cell.timeTV.setVisibility(View.VISIBLE);
					cell.timeTV.setText(DateUtil.wechat_time(message.getTime()));
				}
				else {
					cell.timeTV.setVisibility(View.GONE);
				}
			} catch (Exception e) {
				Logger.i(e);
			}
			return convertView;
		}

	}
	
	@Override
	public void onBackPressed() {
		NoticeManager.getInstance(context).updateStatusByFrom(to, Notice.READ);
		super.onBackPressed();
	}
}
