package net.fktan5.lawson;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class AkikoInfoActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_akikoinfo);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.akiko_info, menu);
        return true;
    }
    
}
