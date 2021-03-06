package com.wtk.mobile.WtkRtc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.wtk.mobile.jni.CommonParams;
import com.wtk.mobile.jni.WtkMediaJNIKit;

import org.webrtc.ContextUtils;
import org.webrtc.voiceengine.WebRtcAudioRecord;

public class MainActivity extends AppCompatActivity implements OnClickListener {
    private String TAG="MainActivity";
    private Button mDialButton,mAnswerButton,mHangupButton;
    private Button mIsSpeakerButton,mIsHoldButton,mIsMuteButton;
    private Button mVideoButton,mVideoConfButton,mApi2Button;
    private SharedPreferences m_sp = null;
    private int regID = 0;
    private int isCaller = 0;
    private String host = null;
    private String port = null;
    private String userid = null;
    private String password = null;
    private EditText editDest;

    private String mStateInfo = "Idle";
    private TextView txtState;
    private int mCurrentSpeaker = 0;
    private int mCurrentMute = 0;
    private int mCurrentHold = 0;

    private CallReceiver receiver = null;
    //Android
    private AudioManager audioManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSharedPreferences();

        editDest = findViewById(R.id.edit_number);

        mDialButton = findViewById(R.id.btn_dial);
        mAnswerButton = findViewById(R.id.btn_answer);
        mHangupButton = findViewById(R.id.btn_hangup);

        mIsSpeakerButton = findViewById(R.id.btn_is_speaker);
        mIsHoldButton = findViewById(R.id.btn_is_hold);
        mIsMuteButton = findViewById(R.id.btn_is_mute);

        mVideoButton = findViewById(R.id.btn_video);
        mVideoConfButton = findViewById(R.id.btn_video_conf);
        mApi2Button = findViewById(R.id.btn_test_api2);

        mDialButton.setOnClickListener(this);
        mAnswerButton.setOnClickListener(this);
        mHangupButton.setOnClickListener(this);

        mIsSpeakerButton.setOnClickListener(this);
        mIsHoldButton.setOnClickListener(this);
        mIsMuteButton.setOnClickListener(this);

        mVideoButton.setOnClickListener(this);
        mVideoConfButton.setOnClickListener(this);
        mApi2Button.setOnClickListener(this);

        txtState = findViewById(R.id.txtState);
        txtState.setText(mStateInfo);

        setActivityMode(CommonParams.STATEIDEL);

        ContextUtils.initialize(MainActivity.this);

        WtkMediaJNIKit.getInstance().SetBroadCast(MainActivity.this);

        audioManager =
                (AudioManager) ContextUtils.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public void onClick(View paramView){
        switch (paramView.getId())
        {
            case R.id.btn_dial:
                String calleeId = editDest.getText().toString();
                String userIdPass = userid + ":" + password;
                String cmd;
                isCaller = 1;
                if(calleeId.contains("80")) {
                    //for call center
                    cmd = "/nortp";
                } else if(calleeId.contains("777")) {
                    //for conference
                    cmd = "/mixer";
                } else {
                    //normal call
                    cmd = "/forward";
                }
                String ext = "Test-Ext";
                Log.d(TAG, "IaxDial params is " +calleeId+",host="+host+",userIdPass="+userIdPass+",cmd="+cmd+",ext="+ext);
                WtkMediaJNIKit.getInstance().IaxDial(calleeId,host,userIdPass,cmd,ext);
                mStateInfo = "Make new call to "+calleeId+"...";
                setActivityMode(CommonParams.STATERINGIN);
                break;
            case R.id.btn_answer:
                WtkMediaJNIKit.getInstance().IaxAnswer();
                mStateInfo = "Speaking with "+WtkMediaJNIKit.getInstance().getCallNumber()+"...";
                setActivityMode(CommonParams.STATETALKING);
                break;
            case R.id.btn_hangup:
                WtkMediaJNIKit.getInstance().IaxHangup();
                mStateInfo = "Idle";
                setActivityMode(CommonParams.STATEIDEL);
                break;
            case R.id.btn_is_speaker:
                mCurrentSpeaker = (mCurrentSpeaker+1)%2;
                if (mCurrentSpeaker == 0) {
                    mIsSpeakerButton.setText("Speaker");
                    audioManager.setSpeakerphoneOn(false);
                } else {
                    mIsSpeakerButton.setText("Headphone");
                    audioManager.setSpeakerphoneOn(true);
                }
                break;
            case R.id.btn_is_hold:
                mCurrentHold = (mCurrentHold+1)%2;
                if(mCurrentHold == 0) {
                    mIsHoldButton.setText("Hold");
                    WtkMediaJNIKit.getInstance().IaxSetHold(mCurrentHold);
                } else {
                    mIsHoldButton.setText("UnHold");
                    WtkMediaJNIKit.getInstance().IaxSetHold(mCurrentHold);
                }
                break;
            case R.id.btn_is_mute:
                mCurrentMute = (mCurrentMute+1)%2;
                if(mCurrentMute == 0) {
                    mIsMuteButton.setText("Mute");
                    WebRtcAudioRecord.setMicrophoneMute(false);
                } else {
                    mIsMuteButton.setText("UnMute");
                    WebRtcAudioRecord.setMicrophoneMute(true);
                }
                break;
            case R.id.btn_video:
                Intent MainInterface = new Intent(MainActivity.this, VideoActivity.class);
                startActivity(MainInterface);
                break;
            case R.id.btn_video_conf:
                Intent VideoConf = new Intent(MainActivity.this, VideoConfActivity.class);
                startActivity(VideoConf);
                break;
            case R.id.btn_test_api2:
                WtkMediaJNIKit.getInstance().CtrlConference(3);
                break;
        }
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction("iax.new.call");
        filter.addAction("iax.hangup");
        filter.addAction("iax.answer");
        filter.addAction("iax.transfer.rs");
        filter.addAction("iax.transfer.nat");
        filter.addAction("iax.transfer.p2p");
        filter.addAction("iax.video.start");
        filter.addAction("iax.video.stop");
        receiver = new CallReceiver();
        registerReceiver(receiver, filter);
    }
    private void getSharedPreferences()
    {
        m_sp = getSharedPreferences("setting", Context.MODE_PRIVATE);
        if(m_sp == null)
            return;

        regID = m_sp.getInt("iax_regid", 0);
        host = m_sp.getString("iax_host", null);
        port = m_sp.getString("iax_port", null);
        userid = m_sp.getString("iax_user", null);
        password = m_sp.getString("iax_pass", null);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(receiver);
        Log.d(TAG, "IaxUnRegister, ret = " + regID);
        WtkMediaJNIKit.getInstance().IaxUnRegister(regID);
        WtkMediaJNIKit.getInstance().IaxShutdown();
        super.onDestroy();
    }
    private void setActivityMode(int mode)
    {
        switch(mode)
        {
            case CommonParams.STATEIDEL:
                mDialButton.setEnabled(true);
                mAnswerButton.setEnabled(false);
                mHangupButton.setEnabled(false);
                mIsHoldButton.setEnabled(false);
                mIsSpeakerButton.setEnabled(false);
                mIsMuteButton.setEnabled(false);
                mVideoButton.setEnabled(false);
                mVideoConfButton.setEnabled(false);
                txtState.setText(mStateInfo);
                //Init some init params
                isCaller = 0;
                mCurrentHold = 0;
                mIsHoldButton.setText("Hold");
                mCurrentMute = 0;
                mIsMuteButton.setText("Mute");
                mCurrentSpeaker = 0;
                mIsSpeakerButton.setText("Speaker");
                break;
            case CommonParams.STATERINGIN:
                mDialButton.setEnabled(false);
                if(isCaller == 1)
                    mAnswerButton.setEnabled(false);
                else
                    mAnswerButton.setEnabled(true);
                mHangupButton.setEnabled(true);
                mIsHoldButton.setEnabled(false);
                mIsSpeakerButton.setEnabled(false);
                mIsMuteButton.setEnabled(false);
                txtState.setText(mStateInfo);
                break;
            case CommonParams.STATETALKING:
                mDialButton.setEnabled(false);
                mAnswerButton.setEnabled(false);
                mHangupButton.setEnabled(true);
                mIsHoldButton.setEnabled(true);
                mIsSpeakerButton.setEnabled(true);
                mIsMuteButton.setEnabled(true);
                String calleeId = editDest.getText().toString();
                if(calleeId.contains("777")||(calleeId.contains("80")))
                    mVideoConfButton.setEnabled(true);
                else
                    mVideoButton.setEnabled(true);
                txtState.setText(mStateInfo);
                break;
        }
    }
    private class CallReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if("iax.hangup".equals(action)) {
                mStateInfo = "Idle";
                setActivityMode(CommonParams.STATEIDEL);
                String calleeId = editDest.getText().toString();
                if(calleeId.contains("777")||(calleeId.contains("80")))
                    WtkMediaJNIKit.getInstance().StopVideoConf();
                else
                    WtkMediaJNIKit.getInstance().StopVideoPlayout();

                WtkMediaJNIKit.getInstance().StopAudioPlayout();
            }
            else if("iax.video.start".equals(action))
            {
                //Intent MainInterface = new Intent(MainActivity.this, VideoActivity.class);
               // startActivity(MainInterface);
            }
            else if("iax.video.stop".equals(action))
            {
                //setActivityMode(CommonParams.STATETALKING);
            }
            else if("iax.new.call".equals(action)) {
                mStateInfo = "Received new call from "+WtkMediaJNIKit.getInstance().getCallNumber()+", answer or hangup...";
                setActivityMode(CommonParams.STATERINGIN);
            }
            else if("iax.answer".equals(action))
            {
                mStateInfo = "Speaking with "+WtkMediaJNIKit.getInstance().getCallNumber()+"...";
                setActivityMode(CommonParams.STATETALKING);
                WtkMediaJNIKit.getInstance().StartAudioPlayout();
            }
            else if("iax.transfer.rs".equals(action))
            {
                mStateInfo = "Speaking with "+WtkMediaJNIKit.getInstance().getCallNumber()+"..., and already relay server transfer";
                setActivityMode(CommonParams.STATETALKING);
            }
            else if("iax.transfer.nat".equals(action))
            {
                mStateInfo = "Speaking with "+WtkMediaJNIKit.getInstance().getCallNumber()+"..., and already nat transfer";
                setActivityMode(CommonParams.STATETALKING);
            }
            else if("iax.transfer.p2p".equals(action))
            {
                mStateInfo = "Speaking with "+WtkMediaJNIKit.getInstance().getCallNumber()+"..., and already p2p transfer";
                setActivityMode(CommonParams.STATETALKING);
            }
            else
            {
                mStateInfo = "Idle";
                setActivityMode(CommonParams.STATEIDEL);
            }
        }
    }
}