package com.example.stechpalme.iot;

import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.app.Activity;
import android.os.Bundle;
import android.os.Parcelable;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity {

    private NfcAdapter myNFCAdapter;
    private TextView myTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myTextView = (TextView) findViewById(R.id.myTextView1);
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
        readNFC(getIntent());
    }
    private void readNFC(Intent intent) {
        String output = "";
        output += "Type: " + intent.getType();
        output += "\nAction: " + intent.getAction();
        output += "\nData: " + intent.getData();
        output += "\nDataString: " + intent.getDataString();
        Parcelable[] rawMsg = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if(rawMsg != null) {
            output += "\n\nExtraMsg\n";
            NdefMessage[] msgs = new NdefMessage[rawMsg.length];
            for(int i = 0; i<rawMsg.length; ++i) {
                msgs[i] = (NdefMessage) rawMsg[i];
            }
            for(int i = 0; i<msgs.length; ++i) {
                output += "message " + i + ": " + msgs[i].toString() + "\n";
            }
        }
        myTextView.setText(output);
    }
}
