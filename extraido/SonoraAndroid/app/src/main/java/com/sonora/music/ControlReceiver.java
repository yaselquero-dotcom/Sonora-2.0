package com.sonora.music;
import android.content.*;
public class ControlReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context context, Intent intent) {
  Intent svc=new Intent(context,MusicService.class); if (android.os.Build.VERSION.SDK_INT >= 26) context.startForegroundService(svc); else context.startService(svc);
  // Notification controls are handled by a tiny service command broadcast through the running service.
  Intent command=new Intent("com.sonora.music.SERVICE_COMMAND"); command.setPackage(context.getPackageName()); command.putExtra(MusicService.CMD,intent.getStringExtra(MusicService.CMD)); context.sendBroadcast(command);
 }
}
