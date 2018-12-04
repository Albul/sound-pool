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

import com.albul.supportsoundpool.SoundPoolCompat;

public class MainActivity extends AppCompatActivity {

    public static final int MAX_VOLUME_FOR_LOG = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

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
        private int mShortSound1Id = -1;
        private int mShortSound2Id = -1;
        private int mShortSound3Id = -1;

        private SeekBar mBg1VolumeBar;
        private SeekBar mBg2VolumeBar;
        private SeekBar mBg2PitchBar;
        private SeekBar mBg3VolumeBar;
        private SeekBar mShort1VolumeBar;
        private SeekBar mShort1PitchBar;
        private SeekBar mShort2VolumeBar;
        private SeekBar mShort2PitchBar;
        private SeekBar mShort3VolumeBar;
        private SeekBar mShort3PitchBar;

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
            rootView.findViewById(R.id.load_short_sound_1).setOnClickListener(this);
            rootView.findViewById(R.id.load_short_sound_2).setOnClickListener(this);
            rootView.findViewById(R.id.load_short_sound_3).setOnClickListener(this);

            rootView.findViewById(R.id.play_bg_1).setOnClickListener(this);
            rootView.findViewById(R.id.play_bg_2).setOnClickListener(this);
            rootView.findViewById(R.id.play_bg_3).setOnClickListener(this);
            rootView.findViewById(R.id.play_short_sound_1).setOnClickListener(this);
            rootView.findViewById(R.id.play_short_sound_2).setOnClickListener(this);
            rootView.findViewById(R.id.play_short_sound_3).setOnClickListener(this);

            rootView.findViewById(R.id.resume_bg_1).setOnClickListener(this);
            rootView.findViewById(R.id.resume_bg_2).setOnClickListener(this);
            rootView.findViewById(R.id.resume_bg_3).setOnClickListener(this);
            rootView.findViewById(R.id.resume_short_sound_1).setOnClickListener(this);
            rootView.findViewById(R.id.resume_short_sound_2).setOnClickListener(this);
            rootView.findViewById(R.id.resume_short_sound_3).setOnClickListener(this);

            rootView.findViewById(R.id.pause_bg_1).setOnClickListener(this);
            rootView.findViewById(R.id.pause_bg_2).setOnClickListener(this);
            rootView.findViewById(R.id.pause_bg_3).setOnClickListener(this);
            rootView.findViewById(R.id.pause_short_sound_1).setOnClickListener(this);
            rootView.findViewById(R.id.pause_short_sound_2).setOnClickListener(this);
            rootView.findViewById(R.id.pause_short_sound_3).setOnClickListener(this);

            rootView.findViewById(R.id.stop_bg_1).setOnClickListener(this);
            rootView.findViewById(R.id.stop_bg_2).setOnClickListener(this);
            rootView.findViewById(R.id.stop_bg_3).setOnClickListener(this);
            rootView.findViewById(R.id.stop_short_sound_1).setOnClickListener(this);
            rootView.findViewById(R.id.stop_short_sound_2).setOnClickListener(this);
            rootView.findViewById(R.id.stop_short_sound_3).setOnClickListener(this);

            rootView.findViewById(R.id.unload_bg_1).setOnClickListener(this);
            rootView.findViewById(R.id.unload_bg_2).setOnClickListener(this);
            rootView.findViewById(R.id.unload_bg_3).setOnClickListener(this);
            rootView.findViewById(R.id.unload_short_sound_1).setOnClickListener(this);
            rootView.findViewById(R.id.unload_short_sound_2).setOnClickListener(this);
            rootView.findViewById(R.id.unload_short_sound_3).setOnClickListener(this);

            mSoundPoolCompat = new SoundPoolCompat(5, 100_000);


            mSoundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
            mSoundID = mSoundPool.load(getContext(), R.raw.sec_tick_bird_goldfinch, 1);


            mBg1VolumeBar = rootView.findViewById(R.id.bg1_volume);
            mBg1VolumeBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
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

            mBg2VolumeBar = rootView.findViewById(R.id.bg2_volume);
            mBg2VolumeBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
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

            mBg2PitchBar = rootView.findViewById(R.id.bg2_pitch);
            mBg2PitchBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mSoundPoolCompat.setRate(mBg2Id, getPitch(seekBar));
                }
            });

            mBg3VolumeBar = rootView.findViewById(R.id.bg3_volume);
            mBg3VolumeBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
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

            mShort1VolumeBar = rootView.findViewById(R.id.short_sound_1_volume);
            mShort1VolumeBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mSoundPoolCompat.setVolume(mShortSound1Id, getVolume(seekBar), getVolume(seekBar));
                }
            });
            mShort1PitchBar = rootView.findViewById(R.id.short_sound_1_pitch);
            mShort1PitchBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mSoundPoolCompat.setRate(mShortSound1Id, getPitch(seekBar));
                }
            });

            mShort2VolumeBar = rootView.findViewById(R.id.short_sound_2_volume);
            mShort2VolumeBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mSoundPoolCompat.setVolume(mShortSound2Id, getVolume(seekBar), getVolume(seekBar));
                }
            });
            mShort2PitchBar = rootView.findViewById(R.id.short_sound_2_pitch);
            mShort2PitchBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mSoundPoolCompat.setRate(mShortSound2Id, getPitch(seekBar));
                }
            });

            mShort3VolumeBar = rootView.findViewById(R.id.short_sound_3_volume);
            mShort3VolumeBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mSoundPoolCompat.setVolume(mShortSound3Id, getVolume(seekBar), getVolume(seekBar));
                }
            });

            mShort3PitchBar = rootView.findViewById(R.id.short_sound_3_pitch);
            mShort3PitchBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mSoundPoolCompat.setRate(mShortSound3Id, getPitch(seekBar));
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
                    mBg3Id = mSoundPoolCompat.load(Environment.getExternalStorageDirectory().getAbsolutePath() + "/PAWA_Kowal.mp3");
                    break;
                case R.id.load_short_sound_1:
                    mShortSound1Id = mSoundPoolCompat.load(v.getContext(), R.raw.voice_female_retain);
                    break;
                case R.id.load_short_sound_2:
                    mShortSound2Id = mSoundPoolCompat.load(v.getContext(), R.raw.sec_tick_bird_goldfinch);
                    break;
                case R.id.load_short_sound_3:
                    mShortSound3Id = mSoundPoolCompat.load(v.getContext(), R.raw.sec_tick_cricket);
                    break;

                case R.id.play_bg_1:
                    mSoundPoolCompat.play(mBg1Id, getVolume(mBg1VolumeBar), getVolume(mBg1VolumeBar), -1, 1);
                    break;
                case R.id.play_bg_2:
                    mSoundPoolCompat.play(mBg2Id, getVolume(mBg2VolumeBar), getVolume(mBg2VolumeBar), -1, getPitch(mBg2PitchBar));
                    break;
                case R.id.play_bg_3:
                    mSoundPoolCompat.play(mBg3Id, getVolume(mBg3VolumeBar), getVolume(mBg3VolumeBar), -1, 1);
                    break;
                case R.id.play_short_sound_1:
                    mSoundPoolCompat.play(mShortSound1Id, getVolume(mShort1VolumeBar), getVolume(mShort1VolumeBar), 0, getPitch(mShort1PitchBar));
                    break;
                case R.id.play_short_sound_2:
                    mSoundPoolCompat.play(mShortSound2Id, getVolume(mShort2VolumeBar), getVolume(mShort2VolumeBar), 0, getPitch(mShort2PitchBar));
                    break;
                case R.id.play_short_sound_3:
                    mSoundPoolCompat.play(mShortSound3Id, getVolume(mShort3VolumeBar), getVolume(mShort3VolumeBar), 0, getPitch(mShort3PitchBar));
                    break;

                case R.id.resume_bg_1:
                    mSoundPoolCompat.resume(mBg1Id);
                    break;
                case R.id.resume_bg_2:
                    mSoundPoolCompat.resume(mBg2Id);
                    break;
                case R.id.resume_bg_3:
                    mSoundPoolCompat.resume(mBg3Id);
                    break;
                case R.id.resume_short_sound_1:
                    mSoundPoolCompat.resume(mShortSound1Id);
                    break;
                case R.id.resume_short_sound_2:
                    mSoundPoolCompat.resume(mShortSound2Id);
                    break;
                case R.id.resume_short_sound_3:
                    mSoundPoolCompat.resume(mShortSound3Id);
                    break;

                case R.id.pause_bg_1:
                    mSoundPoolCompat.pause(mBg1Id);
                    break;
                case R.id.pause_bg_2:
                    mSoundPoolCompat.pause(mBg2Id);
                    break;
                case R.id.pause_bg_3:
                    mSoundPoolCompat.pause(mBg3Id);
                    break;
                case R.id.pause_short_sound_1:
                    mSoundPoolCompat.pause(mShortSound1Id);
                    break;
                case R.id.pause_short_sound_2:
                    mSoundPoolCompat.pause(mShortSound2Id);
                    break;
                case R.id.pause_short_sound_3:
                    mSoundPoolCompat.pause(mShortSound3Id);
                    break;

                case R.id.stop_bg_1:
                    mSoundPoolCompat.stop(mBg1Id);
                    break;
                case R.id.stop_bg_2:
                    mSoundPoolCompat.stop(mBg2Id);
                    break;
                case R.id.stop_bg_3:
                    mSoundPoolCompat.stop(mBg3Id);
                    break;
                case R.id.stop_short_sound_1:
                    mSoundPoolCompat.stop(mShortSound1Id);
                    break;
                case R.id.stop_short_sound_2:
                    mSoundPoolCompat.stop(mShortSound2Id);
                    break;
                case R.id.stop_short_sound_3:
                    mSoundPoolCompat.stop(mShortSound3Id);
                    break;

                case R.id.unload_bg_1:
                    mSoundPoolCompat.unload(mBg1Id);
                    mBg1Id = -1;
                    break;
                case R.id.unload_bg_2:
                    mSoundPoolCompat.unload(mBg2Id);
                    mBg2Id = -1;
                    break;
                case R.id.unload_bg_3:
                    mSoundPoolCompat.unload(mBg3Id);
                    mBg3Id = -1;
                    break;
                case R.id.unload_short_sound_1:
                    mSoundPoolCompat.unload(mShortSound1Id);
                    mShortSound1Id = -1;
                    break;
                case R.id.unload_short_sound_2:
                    mSoundPoolCompat.unload(mShortSound2Id);
                    mShortSound2Id = -1;
                    break;
                case R.id.unload_short_sound_3:
                    mSoundPoolCompat.unload(mShortSound3Id);
                    mShortSound3Id = -1;
                    break;
            }
        }

        private float getVolume(final SeekBar seekBar) {
            return resolveLogVolume(seekBar.getProgress());
        }

        private float getPitch(final SeekBar seekBar) {
            return seekBar.getProgress() / 100F;
        }
    }
}