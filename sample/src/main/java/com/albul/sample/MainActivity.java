package com.albul.sample;

import android.Manifest;
import android.app.Fragment;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

import com.albul.sample.R;
import com.albul.supportsoundpool.SoundPoolCompat;

public class MainActivity extends AppCompatActivity {

    public static final int MAX_VOLUME_FOR_LOG = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                                .add(R.id.container, new PlaceholderFragment()).commit();
        }

    }


    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment implements OnClickListener {

        private int mBg1Id = -1;
        private int mBg2Id = -1;
        private int mBg3Id = -1;
        private int mPhaseTransitionId = -1;
        private int mMetronomeId = -1;

        private SeekBar mBg1Seekbar;
        private SeekBar mBg2Seekbar;
        private SeekBar mBg3Seekbar;
        private SeekBar mPhaseTransitionSeekbar;
        private SeekBar mMetronomeSeekbar;

        SoundPool mSoundPool;
        SoundPoolCompat mSoundPoolCompat;


        private int mSoundID;


        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            // set listeners on buttons
            rootView.findViewById(R.id.load_bg_1).setOnClickListener(this);
            rootView.findViewById(R.id.load_bg_2).setOnClickListener(this);
            rootView.findViewById(R.id.load_bg_3).setOnClickListener(this);
            rootView.findViewById(R.id.phase_transition).setOnClickListener(this);
            rootView.findViewById(R.id.metronome).setOnClickListener(this);
            rootView.findViewById(R.id.play).setOnClickListener(this);
            rootView.findViewById(R.id.resume).setOnClickListener(this);
            rootView.findViewById(R.id.pause).setOnClickListener(this);
            rootView.findViewById(R.id.stop).setOnClickListener(this);
            rootView.findViewById(R.id.unload).setOnClickListener(this);

            mSoundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
            mSoundPoolCompat = new SoundPoolCompat(5, 100_000);


            mSoundID = mSoundPool.load(getContext(), R.raw.sec_tick_bird_goldfinch, 1);


            mBg1Seekbar = rootView.findViewById(R.id.bg1_volume);
            mBg1Seekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mSoundPoolCompat.setVolume(mBg1Id, getVolume(seekBar), getVolume(seekBar));
                }
            });

            mBg2Seekbar = rootView.findViewById(R.id.bg2_volume);
            mBg2Seekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mSoundPoolCompat.setVolume(mBg2Id, getVolume(seekBar), getVolume(seekBar));
                }
            });

            mBg3Seekbar = rootView.findViewById(R.id.bg3_volume);
            mBg3Seekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mSoundPoolCompat.setVolume(mBg3Id, getVolume(seekBar), getVolume(seekBar));
                }
            });

            mPhaseTransitionSeekbar = rootView.findViewById(R.id.phase_transition_volume);
            mPhaseTransitionSeekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mSoundPoolCompat.setVolume(mPhaseTransitionId, getVolume(seekBar), getVolume(seekBar));
                }
            });

            mMetronomeSeekbar = rootView.findViewById(R.id.metronome_volume);
            mMetronomeSeekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mSoundPoolCompat.setVolume(mMetronomeId, getVolume(seekBar), getVolume(seekBar));
                }
            });

            return rootView;
        }

        public static float resolveLogVolume(final int volume) {
            return (float) (1 - (Math.log(MAX_VOLUME_FOR_LOG - volume) / Math.log(MAX_VOLUME_FOR_LOG)));
        }

        @Override
        public void onClick(View v) {
            int id = v.getId();
            switch (id) {
                case R.id.load_bg_1:
                    mBg1Id = mSoundPoolCompat.load(v.getContext(), R.raw.bg_sea_retain);
                    break;

                case R.id.load_bg_2:
                    mBg2Id = mSoundPoolCompat.load(v.getContext(), R.raw.bg_sunrise_inhale);
                    break;

                case R.id.load_bg_3:
                    ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                    mBg3Id = mSoundPoolCompat.load(Environment.getExternalStorageDirectory().getAbsolutePath() + "/PAWA_Kowal.mp3");
                    break;

                case R.id.phase_transition:
                    mPhaseTransitionId = mSoundPoolCompat.load(v.getContext(), R.raw.voice_female_retain);
                    break;

                case R.id.metronome:
                    mMetronomeId = mSoundPoolCompat.load(v.getContext(), R.raw.sec_tick_bird_goldfinch);
                    break;

                case R.id.play:
                    mSoundPoolCompat.play(mBg1Id, getVolume(mBg1Seekbar), getVolume(mBg1Seekbar), -1, 1);
                    mSoundPoolCompat.play(mBg2Id, getVolume(mBg2Seekbar), getVolume(mBg2Seekbar), -1, 1);
                    mSoundPoolCompat.play(mBg3Id, getVolume(mBg3Seekbar), getVolume(mBg3Seekbar), -1, 1);
                    mSoundPoolCompat.play(mPhaseTransitionId, getVolume(mPhaseTransitionSeekbar), getVolume(mPhaseTransitionSeekbar), 2, 2);
                    mSoundPoolCompat.play(mMetronomeId, getVolume(mMetronomeSeekbar), getVolume(mMetronomeSeekbar), 0, 1);
//                    mSoundPool.play(mSoundID, 1, 1, 1, 0, 1f);
                    break;

                case R.id.resume:
                    mSoundPoolCompat.resume(mBg1Id);
                    mSoundPoolCompat.resume(mBg2Id);
                    mSoundPoolCompat.resume(mBg3Id);
                    mSoundPoolCompat.resume(mPhaseTransitionId);
                    mSoundPoolCompat.resume(mMetronomeId);
                    break;

                case R.id.pause:
                    mSoundPoolCompat.pause(mBg1Id);
                    mSoundPoolCompat.pause(mBg2Id);
                    mSoundPoolCompat.pause(mBg3Id);
                    mSoundPoolCompat.pause(mPhaseTransitionId);
                    mSoundPoolCompat.pause(mMetronomeId);
                    break;

                case R.id.stop:
                    mSoundPoolCompat.stop(mBg1Id);
                    mSoundPoolCompat.stop(mBg2Id);
                    mSoundPoolCompat.stop(mBg3Id);
                    mSoundPoolCompat.stop(mPhaseTransitionId);
                    mSoundPoolCompat.stop(mMetronomeId);
                    break;

                case R.id.unload:
                    mSoundPoolCompat.unload(mBg1Id);
                    mBg1Id = -1;
                    mSoundPoolCompat.unload(mBg2Id);
                    mBg2Id = -1;
                    mSoundPoolCompat.unload(mBg3Id);
                    mBg3Id = -1;
                    mSoundPoolCompat.unload(mPhaseTransitionId);
                    mPhaseTransitionId = -1;
                    mSoundPoolCompat.unload(mMetronomeId);
                    mMetronomeId = -1;
                    break;
            }
        }

        private float getVolume(final SeekBar seekBar) {
            return resolveLogVolume(seekBar.getProgress());
        }
    }
}