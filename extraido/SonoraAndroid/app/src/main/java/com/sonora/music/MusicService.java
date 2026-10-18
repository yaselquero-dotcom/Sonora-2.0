package com.sonora.music;
import android.media.audiofx.Equalizer;
import android.app.*;
import android.content.*;
import android.media.*;
import android.net.Uri;
import android.os.*;
import androidx.core.app.NotificationCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import java.util.*;

public class MusicService extends Service {
    public static final String ACTION = "com.sonora.music.CONTROL";
    public static final String CMD = "cmd";
    private final IBinder binder = new LocalBinder();
    private MediaPlayer player;
    private MediaSessionCompat session;
    private final ArrayList<Uri> queue = new ArrayList<>();
    private final ArrayList<String> names = new ArrayList<>();
    private int index = -1;
    private Equalizer equalizer;
    private final BroadcastReceiver commandReceiver = new BroadcastReceiver() { public void onReceive(Context c, Intent i) { String cmd=i.getStringExtra(CMD); if("toggle".equals(cmd)) toggle(); else if("next".equals(cmd)) next(); else if("prev".equals(cmd)) previous(); } };
    private boolean shuffle = false, repeat = false;

    public class LocalBinder extends Binder { public MusicService getService() { return MusicService.this; } }
    @Override public void onCreate() {
        super.onCreate();
        createChannel();
        registerReceiver(commandReceiver, new IntentFilter("com.sonora.music.SERVICE_COMMAND"));
        session = new MediaSessionCompat(this, "SONORA");
        session.setCallback(new MediaSessionCompat.Callback() {
            @Override public void onPlay() { play(); }
            @Override public void onPause() { pause(); }
            @Override public void onSkipToNext() { next(); }
            @Override public void onSkipToPrevious() { previous(); }
            @Override public void onSeekTo(long pos) { seek((int)pos); }
        });
        session.setActive(true);
    }
    @Override public IBinder onBind(Intent i) { return binder; }
    @Override public int onStartCommand(Intent intent, int flags, int startId) { startForeground(40, notification()); return START_STICKY; }
    public void setQueue(List<Uri> uris, List<String> titles) {
        queue.clear(); queue.addAll(uris); names.clear(); names.addAll(titles);
        if (index < 0 && !queue.isEmpty()) index = 0;
    }
    public ArrayList<String> getNames() { return new ArrayList<>(names); }
    public int getIndex() { return index; }
    public boolean isPlaying() { return player != null && player.isPlaying(); }
    public int duration() { try { return player == null ? 0 : player.getDuration(); } catch(Exception e) { return 0; } }
    public int position() { try { return player == null ? 0 : player.getCurrentPosition(); } catch(Exception e) { return 0; } }
    public int audioSessionId() { return player == null ? 0 : player.getAudioSessionId(); }
    public void playIndex(int i) {
        if (i < 0 || i >= queue.size()) return;
        index = i;
        try {
            if (player != null) { player.release(); player = null; }
            if (equalizer != null) { equalizer.release(); equalizer = null; }
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
            player.setDataSource(this, queue.get(index));
            player.setOnPreparedListener(mp -> { mp.start(); startForeground(40, notification()); updateState(); });
            player.setOnCompletionListener(mp -> { if (repeat) playIndex(index); else next(); });
            player.setOnErrorListener((mp, what, extra) -> { next(); return true; });
            player.prepareAsync();
        } catch (Exception e) { android.util.Log.e("SONORA", "No se pudo abrir el audio", e); }
    }
    public void play() { if (player == null) { if (!queue.isEmpty()) playIndex(Math.max(0,index)); return; } try { player.start(); startForeground(40, notification()); updateState(); } catch(Exception ignored){} }
    public void pause() { if (player != null) player.pause(); updateState(); startForeground(40, notification()); }
    public void toggle() { if (isPlaying()) pause(); else play(); }
    public void next() { if (queue.isEmpty()) return; int n = shuffle && queue.size()>1 ? new Random().nextInt(queue.size()) : (index+1)%queue.size(); playIndex(n); }
    public void previous() { if (player != null && position()>3000) { seek(0); return; } if (queue.isEmpty()) return; playIndex((index-1+queue.size())%queue.size()); }
    public void seek(int p) { if(player!=null) try { player.seekTo(Math.max(0,Math.min(p,player.getDuration()))); } catch(Exception ignored){} }
    public void setShuffle(boolean b) { shuffle=b; }
    public void setRepeat(boolean b) { repeat=b; }
    public boolean getRepeat() { return repeat; }
    public boolean getShuffle() { return shuffle; }
    public short[] equalizerLevels() { if(equalizer==null) return new short[0]; short[] out=new short[equalizer.getNumberOfBands()]; for(short i=0;i<out.length;i++) out[i]=equalizer.getBandLevel(i); return out; }
    public short equalizerMin() { return equalizer==null?0:equalizer.getBandLevelRange()[0]; }
    public short equalizerMax() { return equalizer==null?0:equalizer.getBandLevelRange()[1]; }
    public int bandCount() { return equalizer==null?0:equalizer.getNumberOfBands(); }
    public int centerFreq(int band) { return equalizer==null?0:equalizer.getCenterFreq((short)band); }
    public void setBand(int band, short level) { if(equalizer!=null) try { equalizer.setBandLevel((short)band,level); } catch(Exception ignored){} }
    public void enableEqualizer(boolean on) { try { if(player==null) return; if(equalizer==null) equalizer=new Equalizer(0,player.getAudioSessionId()); equalizer.setEnabled(on); } catch(Exception e) { android.util.Log.w("SONORA","Equalizador no disponible",e); } }
    private void updateState() {
        if(session==null) return;
        long actions=PlaybackStateCompat.ACTION_PLAY|PlaybackStateCompat.ACTION_PAUSE|PlaybackStateCompat.ACTION_PLAY_PAUSE|PlaybackStateCompat.ACTION_SKIP_TO_NEXT|PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS|PlaybackStateCompat.ACTION_SEEK_TO;
        session.setPlaybackState(new PlaybackStateCompat.Builder().setActions(actions).setState(isPlaying()?PlaybackStateCompat.STATE_PLAYING:PlaybackStateCompat.STATE_PAUSED,position(),1f).build());
        if(index>=0 && index<names.size()) session.setMetadata(new MediaMetadataCompat.Builder().putString(MediaMetadataCompat.METADATA_KEY_TITLE,names.get(index)).putString(MediaMetadataCompat.METADATA_KEY_ARTIST,"SONORA · Música local").putLong(MediaMetadataCompat.METADATA_KEY_DURATION,duration()).build());
    }
    private Notification notification() {
        String title=index>=0&&index<names.size()?names.get(index):"SONORA";
        android.app.PendingIntent prev=action("prev",1), toggle=action("toggle",2), next=action("next",3);
        return new NotificationCompat.Builder(this,"sonora_music").setContentTitle(title).setContentText("SONORA · Reproducción local").setSmallIcon(android.R.drawable.ic_media_play).setVisibility(NotificationCompat.VISIBILITY_PUBLIC).setOnlyAlertOnce(true).setOngoing(isPlaying()).addAction(new androidx.core.app.NotificationCompat.Action(android.R.drawable.ic_media_previous,"Anterior",prev)).addAction(new androidx.core.app.NotificationCompat.Action(isPlaying()?android.R.drawable.ic_media_pause:android.R.drawable.ic_media_play,isPlaying()?"Pausar":"Reproducir",toggle)).addAction(new androidx.core.app.NotificationCompat.Action(android.R.drawable.ic_media_next,"Siguiente",next)).setStyle(new androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(session.getSessionToken()).setShowActionsInCompactView(0,1,2)).build();
    }
    private android.app.PendingIntent action(String cmd,int id) { Intent i=new Intent(this,ControlReceiver.class).setAction(ACTION).putExtra(CMD,cmd); return android.app.PendingIntent.getBroadcast(this,id,i,android.app.PendingIntent.FLAG_UPDATE_CURRENT|android.app.PendingIntent.FLAG_IMMUTABLE); }
    private void createChannel() { if(Build.VERSION.SDK_INT>=26) { NotificationChannel c=new NotificationChannel("sonora_music","SONORA reproducción",NotificationManager.IMPORTANCE_LOW); getSystemService(NotificationManager.class).createNotificationChannel(c); } }
    @Override public void onDestroy() { if(player!=null) player.release(); if(equalizer!=null) equalizer.release(); if(session!=null) { session.setActive(false); session.release(); } try { unregisterReceiver(commandReceiver); } catch(Exception ignored){} super.onDestroy(); }
}
