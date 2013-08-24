package net.fktan5.lawson;

import android.app.ActionBar;
import android.content.Context;
import android.os.Bundle;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import net.fktan5.lawson.model.AkikoTweet;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_activity);

        ActionBar actionbar = getActionBar();
        actionbar.setTitle("HOME akiko");

        List<AkikoTweet> tweets = new ArrayList<AkikoTweet>();
        for(int i=0; i < 30; i++){
            AkikoTweet tweet = new AkikoTweet();
            tweet.setmTweet(getString(R.string.dummy1));
            tweets.add(tweet);
        }

        AkikoTweetListAdapter adapter = new AkikoTweetListAdapter(this, tweets);

        ListView akikoTweetListView = (ListView)findViewById(R.id.akikos_tweet_list);
        akikoTweetListView.setAdapter(adapter);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    public class AkikoTweetListAdapter extends BaseAdapter{
        Context context;
        List<AkikoTweet> akikoTweets;
        public AkikoTweetListAdapter(Context context, List<AkikoTweet> items){
            super();
            this.context = context;
            akikoTweets = items;
        }

        @Override
        public int getCount() {
            return akikoTweets.size();
        }

        @Override
        public Object getItem(int i) {
            return akikoTweets.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            AkikoTweet tweet = akikoTweets.get(i);
            ViewHolder viewHolder;
            if(view == null){
                TextView textview;
                LayoutInflater inflater = LayoutInflater.from(context);
                view = inflater.inflate(R.layout.akiko_tweet, null);

                viewHolder = new ViewHolder();
                viewHolder.textTweet = (TextView)view.findViewById(R.id.akiko_tweet_text);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder)view.getTag();
            }

            viewHolder.textTweet.setText(tweet.getmTweet());
//            ((TextView)view.findViewById(R.id.akiko_tweet_text)).setText(tweet.getmTweet());
            return view;
        }
    }

    private static class ViewHolder{
        TextView textTweet;

    }
}
