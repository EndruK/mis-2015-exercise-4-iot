package com.example.stechpalme.iot;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.app.Activity;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.os.Bundle;
import android.os.Parcelable;
import android.widget.TextView;
import android.widget.Toast;

/*
TODO: 3rd part of ex.1
TODO: TAG ID?
TODO: technology?
TODO: Data type/MIME Type of NDEF DATA
 */
public class MainActivity extends Activity {

    private NfcAdapter myNFCAdapter;
    private TextView myTextView;
    private IntentFilter[] myFilters;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myTextView = (TextView) findViewById(R.id.myTextView1);
        myNFCAdapter = NfcAdapter.getDefaultAdapter(this);
        //pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
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
        IntentFilter myFilter = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            myFilter.addDataType("*/*");
            myFilters = new IntentFilter[] {myFilter};
        } catch(Exception e) {
            e.printStackTrace();
        }



        String output = "";
        output += "Type: " + intent.getType();
        output += "\nData: " + intent.getData();
        output += "\nDataString: " + intent.getDataString();
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if(tag != null) {
            output += "\nExtraTag:\n";
            String[] list = tag.getTechList();
            for(String el : list) {
                output += el + "\n";
            }
        }
        Parcelable[] rawMsg = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

        if(rawMsg != null) {
            output += "\n\nExtraMsg:";
            NdefMessage[] msgs = new NdefMessage[rawMsg.length];
            for(int i = 0; i<rawMsg.length; ++i) {
                msgs[i] = (NdefMessage) rawMsg[i];
            }
            for(int i = 0; i<msgs.length; ++i) {
                for(int j=0; j<msgs[i].getRecords().length; ++j) {
                    if (!intent.getData().equals("http://")) {
                        try {
                            byte[] payload = msgs[i].getRecords()[j].getPayload();
                            output += "\nmessage " + j + ": " + parsePayload(payload);
                            System.out.println(parsePayload(payload));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        output += "\nmessage " + j + ": " + msgs[i].getRecords()[j].getPayload().toString();
                        System.out.println(msgs[i].getRecords()[j].getPayload().toString());
                    }
                }
            }
        }

        if(myNFCAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            Tag tmp = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            MifareClassic mfc = MifareClassic.get(tmp);
            byte[] data = null;
            try {
                mfc.connect();
                int sectors = mfc.getSectorCount();
                for(int i=0; i<sectors; ++i) {
                    if(mfc.authenticateSectorWithKeyA(i,MifareClassic.KEY_DEFAULT)) {
                        int blocks = mfc.getBlockCount();
                        int index = 0;
                        for(int j=0; j<blocks; ++j) {
                            index = mfc.sectorToBlock(i);
                            data = mfc.readBlock(index);
                            index++;
                        }
                    }
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
            if(data != null) {
                output += "\n\nbytes:\n" + data.toString();
                output += "\n\nhex:\n" + bytesToHexString(data);
            }
            else {
                Toast t = Toast.makeText(this,"hmm leider nein",Toast.LENGTH_LONG);
                t.show();
            }
        }

        myTextView.setText(output);
    }
    // from http://stackoverflow.com/questions/14607425/read-data-from-nfc-tag
    private String parsePayload(byte[] input) throws Exception {
        String utf8 = "UTF-8";
        String utf16 = "UTF-16";
        String enc = ((input[0] & 0200) == 0) ? utf8 : utf16;
        int langCodeLength = input[0] & 0077;
        String langCode = new String(input, 1, langCodeLength, "US-ASCII");
        String text = new String(input, langCodeLength + 1, input.length - langCodeLength - 1, enc);
        return text;
    }
    private String bytesToHexString(byte[] input) {
        String output = "";
        for(byte b : input) {
            output += String.format("%02x",b);
        }
        return output;
    }
}
