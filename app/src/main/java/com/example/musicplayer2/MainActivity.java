package com.example.musicplayer2;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.pm.PackageManager;
import android.icu.text.Transliterator;
import android.media.MediaPlayer;
import android.media.audiofx.EnvironmentalReverb;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    private static final int REQUEST_PERMISSIONS = 12345;
    private static final int PERMISSIONS_COUNT = 1;

    @SuppressLint("NewApi")
    private boolean arePermissionsDenied() {
        for(int i=0; i<PERMISSIONS_COUNT; i++) {
            if(checkSelfPermission(PERMISSIONS[i]) != PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(arePermissionsDenied()) {
            ((ActivityManager) (this.getSystemService(ACTIVITY_SERVICE))).clearApplicationUserData();
            recreate();
        }
        else {
            onResume();
        }
    }

    private boolean isMusicPlayerInit;
    private List<String> musicFilesList;
    private void addMusicFileFrom(String dirPath){
        final File musicDir = new File(dirPath);
        if(!musicDir.exists()) {
            musicDir.mkdir();
            return;
        }
        final File[] files = musicDir.listFiles();
        for(File file: files) {
            final String path= file.getAbsolutePath();
            if(path.endsWith(".mp3")) {
                musicFilesList.add(path);
            }
        }
    }
    private void fillMusicList() {
        musicFilesList.clear();
        addMusicFileFrom(String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)));
        addMusicFileFrom(String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)));
    }
    private MediaPlayer mp;
    private boolean musicPlaying = false;
    private int playMusicFile(String path) {
        mp = new MediaPlayer();
        try {
            mp.setDataSource(path);
            mp.prepare();
            mp.start();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return mp.getDuration();
    }


    private int songPosition;

    @Override
    protected void onResume() {
        super.onResume();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && arePermissionsDenied()) {
            requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS);
            return;
        }

        if(!isMusicPlayerInit) {
            final ListView listview = findViewById(R.id.listView);
            final TextAdapter textAdapter = new TextAdapter();
            musicFilesList = new ArrayList<>();
            fillMusicList();
            textAdapter.setData(musicFilesList);
            listview.setAdapter(textAdapter);
            final SeekBar seekbar = findViewById(R.id.seekBar);
            seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                int songProgress;
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    songProgress = progress;
//                    mp.seekTo(songProgress);
//                    seekBar.setProgress(songProgress);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    mp.seekTo(songProgress);
//                    seekBar.setProgress(songProgress);
                }
            });
            listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if(musicPlaying) {
                        mp.stop();
                    }
                    final String musicFilePath = musicFilesList.get(position);
                    final int songDuration = playMusicFile(musicFilePath);
                    musicPlaying = true;
                    seekbar.setMax(songDuration);
                    seekbar.setVisibility(View.VISIBLE);
                    new Thread() {
                        public void run() {
                            songPosition = 0;
                            while(songPosition < songDuration) {
                                            try {
                                                Thread.sleep(1000);
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                            songPosition+=1000;
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    seekbar.setProgress(mp.getCurrentPosition());
                                    }
                                });
                            }
                        }
                    }.start();
                }
            });

            isMusicPlayerInit = true;
        }

    }

    class TextAdapter extends BaseAdapter {
        private List<String> data = new ArrayList<>();
        void setData(List<String> mData) {
            data.clear();
            data.addAll(mData);
            notifyDataSetChanged();
        }
        @Override
        public int getCount() {
            return data.size();
        }
        @Override
        public String getItem(int position) {
            return null;
        }
        @Override
        public long getItemId(int position) {
            return 0;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).
                        inflate(R.layout.item, parent, false);
                convertView.setTag(new ViewHolder((TextView) convertView.findViewById(R.id.myItem)));
            }
            ViewHolder holder = (ViewHolder) convertView.getTag();
            final String item = data.get(position);
            holder.info.setText(item.substring(item.lastIndexOf('/') + 1));
            return convertView;
        }
        class ViewHolder {
            TextView info;
            ViewHolder(TextView mInfo) {
                info = mInfo;
            }
        }
    }

}
