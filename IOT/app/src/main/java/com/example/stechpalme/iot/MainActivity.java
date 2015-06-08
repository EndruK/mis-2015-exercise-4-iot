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
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.Charset;


/*
TODO: 3rd part of ex.1
 */
public class MainActivity extends Activity {

    private NfcAdapter myNFCAdapter;
    private TextView myTextView;
    private TextView raw;
    private PendingIntent myPendingIntent;
    private IntentFilter[] myIntentList;
    private String[][] myTechList;
    private Tag myTag;

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
        raw = (TextView) findViewById(R.id.myTextView2);
        raw.setVisibility(View.INVISIBLE);
        myNFCAdapter = NfcAdapter.getDefaultAdapter(this);
        //to stay on top and catch all nfc intents
        myPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        //our nfc tags only had these tech supports
        myTechList = new String[][] {
                new String[] {NfcA.class.getName()},
                new String[] {Ndef.class.getName()},
                new String[] {MifareUltralight.class.getName()}};
        //if there is no nfc adapter on the device
        if(myNFCAdapter == null) {
            Toast t = Toast.makeText(this, "NFC is not supported by this device", Toast.LENGTH_LONG);
            t.show();
            finish();
            return;
        }
        //if the nfc adapter is disabled
        if(!myNFCAdapter.isEnabled()) {
            Toast t = Toast.makeText(this,"NFC is disabled! Please enable :)",Toast.LENGTH_LONG);
            t.show();
        }
        defineFilters();
    }
    private void defineFilters() {
        //create ndef filter for all data types and urls
        IntentFilter ndefFilter = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            ndefFilter.addDataType("*/*");
            ndefFilter.addDataScheme("http");
        }catch (IntentFilter.MalformedMimeTypeException e) {
            e.printStackTrace();
        }
        //create mifare filter for the mifare technology
        IntentFilter mifareFilter = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        myIntentList = new IntentFilter[] {ndefFilter, mifareFilter};
    }
    private void readNFC(Intent intent) {
        String output = "";
        myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if(myTag != null) {
            //read NFC basic content
            output += readTag(myTag);
            //read ndef formatted content
            output += readNDEF(intent) + "\n";
        }
        else {
            output = "Please Scan a NFC Tag";
        }
        myTextView.setText(output);
    }
    //handle the "show RAW data" button click
    public void handleButton(View view) {
        raw.setMovementMethod(new ScrollingMovementMethod());
        //only when the tag is available (because mifare has to connect)
        if(myTag != null) {
            String result = readMifare(myTag);
            if(!result.equals("")) {
                raw.setVisibility(View.VISIBLE);
                raw.setText(result);
            }
        }
        else {
            Toast t = Toast.makeText(this,"no NFC tag nearby",Toast.LENGTH_LONG);
            t.show();
        }
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
    private String readNDEF(Intent intent) {
        String output = "";
        //get the ndef messages
        Parcelable[] rawMsg = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (rawMsg != null) {
            output += "\nNDEF Messages:";
            //parse the ndef formatted messages
            NdefMessage[] msgs = new NdefMessage[rawMsg.length];
            for (int i = 0; i < rawMsg.length; ++i) {
                msgs[i] = (NdefMessage) rawMsg[i];
            }
            //iterate over all ndef formatted messages
            for (int i = 0; i < msgs.length; ++i) {
                //iterate over all payloads
                for (int j = 0; j < msgs[i].getRecords().length; ++j) {
                    byte[] payload = msgs[i].getRecords()[j].getPayload();
                    String parsedPayload = "";
                    //get the MIME-Type of the payload
                    String type = msgs[i].getRecords()[j].toMimeType();
                    //when there is a MIME-Type we have to convert the payload
                    if (type != null) {
                        try {
                            parsedPayload = parsePayload(payload);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    //or we have something else - in our case it was a URI
                    else {
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
        return output;
    }
    //get the basic NFC content (ID and technologies)
    private String readTag(Tag tag) {
        String output = "";
        output += "Tag ID: " + bytesToHexString(tag.getId());
        output += "\n\nTechnology:\n";
        String[] list = tag.getTechList();
        for (String el : list) {
            output += el + "\n";
        }
        return output;
    }
    //read the Mifare RAW data
    private String readMifare(Tag tag) {
        String output = "";
        MifareUltralight mifare = MifareUltralight.get(tag);
        if (mifare != null) {
            try {
                //if button is pressed the smartphone has to actual scan the NFC tag
                mifare.connect();
                int type = mifare.getType();
                int len;
                //there are different types of mifare ultralight chips with different lengths
                if(type == mifare.TYPE_ULTRALIGHT_C) len = 44;
                else len = 16;
                String ascii = "";
                String hex = "";
                //iterate over the chip pages
                for(int i=0; i<len; i=i+4) {
                    byte[]b = mifare.readPages(i);
                    //convert content to SCII
                    ascii += new String(b,Charset.forName("US-ASCII")) + "\n";
                    //convert content to Hex
                    hex += bytesToHexString(b) + "\n";
                }
                output += "ASCII:\n" + ascii + "\nHEX:\n" + hex;
            } catch (Exception e) {
                e.printStackTrace();
                Toast t = Toast.makeText(this, "could not connect to mifare ultralight", Toast.LENGTH_LONG);
                t.show();
                raw.setVisibility(View.INVISIBLE);
            } finally {
                try {
                    mifare.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast t = Toast.makeText(this,"could not cloase mifare ultralight",Toast.LENGTH_LONG);
                    t.show();
                    raw.setVisibility(View.INVISIBLE);
                }
            }
        }
        return output;
    }
}
