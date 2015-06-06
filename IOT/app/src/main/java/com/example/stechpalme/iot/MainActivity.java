package com.example.stechpalme.iot;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.app.Activity;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.os.Parcelable;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.Charset;


/*
TODO: 3rd part of ex.1
 */
public class MainActivity extends Activity {

    private NfcAdapter myNFCAdapter;
    private TextView myTextView;
    private PendingIntent myPendingIntent;
    private IntentFilter[] myIntentList;
    private String[][] myTechList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setUpNFC();
        readNFC(getIntent());
    }
    public void onPause() {
        super.onPause();
        myNFCAdapter.disableForegroundDispatch(this);
    }

    public void onResume() {
        super.onResume();
        myNFCAdapter.enableForegroundDispatch(this, myPendingIntent, myIntentList, myTechList);
    }

    public void onNewIntent(Intent intent) {
        setUpNFC();
        readNFC(intent);
    }
    private void setUpNFC() {
        myTextView = (TextView) findViewById(R.id.myTextView1);
        myNFCAdapter = NfcAdapter.getDefaultAdapter(this);
        myPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        myTechList = new String[][] {
                new String[] {NfcA.class.getName()},
                new String[] {Ndef.class.getName()},
                new String[] {MifareUltralight.class.getName()}};
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
        defineFilters();
    }
    private void defineFilters() {
        IntentFilter ndefFilter = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            ndefFilter.addDataType("*/*");
            ndefFilter.addDataScheme("http");
        }catch (IntentFilter.MalformedMimeTypeException e) {
            e.printStackTrace();
        }
        IntentFilter mifareFilter = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        myIntentList = new IntentFilter[] {ndefFilter, mifareFilter};
    }
    private void readNFC(Intent intent) {
        String output = "";
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if(tag != null) {
            output += "Tag ID: " + bytesToHexString(tag.getId());
            output += "\n\nTechnology:\n";
            String[] list = tag.getTechList();
            for (String el : list) {
                output += el + "\n";
            }
            Parcelable[] rawMsg = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMsg != null) {
                output += "\nNDEF Messages:";
                NdefMessage[] msgs = new NdefMessage[rawMsg.length];
                for (int i = 0; i < rawMsg.length; ++i) {
                    msgs[i] = (NdefMessage) rawMsg[i];
                }
                for (int i = 0; i < msgs.length; ++i) {
                    for (int j = 0; j < msgs[i].getRecords().length; ++j) {
                        byte[] payload = msgs[i].getRecords()[j].getPayload();
                        String parsedPayload = "";
                        String type = msgs[i].getRecords()[j].toMimeType();
                        if (type != null) {
                            try {
                                parsedPayload = parsePayload(payload);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            type = "no type";
                            try {
                                parsedPayload = msgs[i].getRecords()[j].toUri().toString();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        output += "\nMessage #" + j + " (Mime:" + type + "):\n" + parsedPayload;
                    }
                }
            }
            MifareUltralight mifare = MifareUltralight.get(tag);
            if (mifare != null) {
                try {
                    mifare.connect();
                    byte[] payload = mifare.readPages(4);
                    System.out.println(parsePayload(payload));
                    //String tmp = new String(payload, Charset.forName("US-ASCII"));
                    //System.out.println(tmp);

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast t = Toast.makeText(this, "could not connect to mifare ultralight", Toast.LENGTH_LONG);
                    t.show();
                } finally {
                    try {
                        mifare.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast t = Toast.makeText(this,"could not cloase mifare ultralight",Toast.LENGTH_LONG);
                        t.show();
                    }
                }
            }
        }
        else {
            output = "Please Scan a NFC Tag";
        }
        myTextView.setText(output);
    }
    // from http://stackoverflow.com/questions/14607425/read-data-from-nfc-tag
    private String parsePayload(byte[] input) throws Exception {
        String utf8 = "UTF-8";
        String utf16 = "UTF-16";
        String enc = ((input[0] & 0200) == 0) ? utf8 : utf16;
        int langCodeLength = input[0] & 0077;
        String text = new String(input, langCodeLength + 1, input.length - langCodeLength - 1, enc);
        return text;
    }
    //from http://stackoverflow.com/questions/6060312/how-do-you-read-the-unique-id-of-an-nfc-tag-on-android
    private String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("0x");
        if (src == null || src.length <= 0) {
            return null;
        }

        char[] buffer = new char[2];
        for (int i = 0; i < src.length; i++) {
            buffer[0] = Character.forDigit((src[i] >>> 4) & 0x0F, 16);
            buffer[1] = Character.forDigit(src[i] & 0x0F, 16);
            stringBuilder.append(buffer);
        }

        return stringBuilder.toString();
    }
}
