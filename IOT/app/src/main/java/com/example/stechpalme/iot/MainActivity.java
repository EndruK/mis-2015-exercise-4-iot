package com.example.stechpalme.iot;

import android.nfc.NfcAdapter;
import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;


public class MainActivity extends Activity {

    private NfcAdapter myNFCAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myNFCAdapter = NfcAdapter.getDefaultAdapter(this);
        if(myNFCAdapter == null) {
            Toast t = Toast.makeText(this, "NFC is not supported by this device", Toast.LENGTH_LONG);
            t.show();
            finish();
            return;
        }
        if(!myNFCAdapter.isEnabled()) {
            Toast t = Toast.makeText(this,"NFC is disabled! Please enable :)",Toast.LENGTH_LONG);
            t.show();
        }
    }
}
