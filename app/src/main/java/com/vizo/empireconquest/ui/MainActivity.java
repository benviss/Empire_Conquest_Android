package com.vizo.empireconquest.ui;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.vizo.empireconquest.R;

public class MainActivity extends Activity implements View.OnClickListener{



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        for (int id : CLICKABLES) {
            findViewById(id).setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.button_start_game) {
            Intent intent = new Intent(MainActivity.this, GameMapActivity.class);
            startActivity(intent);
        }
    }






    final static int[] CLICKABLES = {
            R.id.button_start_game
    };

}
