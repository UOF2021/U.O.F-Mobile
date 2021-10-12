package com.uos.uos_mobile.other;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.uos.uos_mobile.activity.IntroActivity;
import com.uos.uos_mobile.activity.LobbyActivity;
import com.uos.uos_mobile.activity.OrderListActivity;
import com.uos.uos_mobile.activity.UosActivity;
import com.uos.uos_mobile.manager.SQLiteManager;
import com.uos.uos_mobile.manager.SharedPreferenceManager;

import java.util.Map;

public class UosFcmService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d("UOS_MOBILE_FCM", "From: " + remoteMessage.getFrom());

        if (remoteMessage.getData().size() > 0) {
            Map<String, String> recvData = remoteMessage.getData();
            String companyName = recvData.get("company_name");
            String orderNumber = recvData.get("order_number");

            SQLiteManager sqLiteManager = new SQLiteManager(getApplicationContext());
            sqLiteManager.openDatabase();
            sqLiteManager.setOrderState(Integer.valueOf(orderNumber), Global.SQLite.ORDER_STATE_PREPARED);
            sqLiteManager.closeDatabase();

            final UosActivity lobbyActivity = UosActivity.get(LobbyActivity.class);

            if (lobbyActivity != null) {
                lobbyActivity.runOnUiThread(() -> {
                    ((LobbyActivity) lobbyActivity).updateList();
                    ((LobbyActivity) lobbyActivity).moveToOrderNumber(Integer.valueOf(orderNumber));
                });
            } else {
                final UosActivity orderListActivity = UosActivity.get(OrderListActivity.class);

                if (orderListActivity != null) {
                    orderListActivity.runOnUiThread(() -> {
                        ((OrderListActivity) orderListActivity).doUpdateOrderScreen();
                    });
                }
            }

            Intent intent = new Intent(getApplicationContext(), IntroActivity.class);
            intent.setData(Uri.parse(orderNumber));
            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder notification = new NotificationCompat.Builder(this, Global.Notification.CHANNEL_ID)
                    .setSmallIcon(com.uos.uos_mobile.R.mipmap.ic_launcher)
                    .setContentTitle(companyName + "에서 주문하신 상품이 준비되었습니다")
                    .setContentText("카운터에서 상품을 수령해주세요")
                    .setFullScreenIntent(pendingIntent, true)
                    .setAutoCancel(true)
                    .setGroup(Global.Notification.GROUP_ID)
                    .setSmallIcon(com.uos.uos_mobile.R.mipmap.ic_uos_logo_round);

            NotificationCompat.Builder notificationGroup = new NotificationCompat.Builder(this, Global.Notification.CHANNEL_ID)
                    .setSmallIcon(com.uos.uos_mobile.R.mipmap.ic_launcher)
                    .setGroup(Global.Notification.GROUP_ID)
                    .setAutoCancel(true)
                    .setSmallIcon(com.uos.uos_mobile.R.mipmap.ic_uos_logo_round)
                    .setGroupSummary(true);

            NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);

            SharedPreferenceManager.open(getApplicationContext(), Global.SharedPreference.APP_DATA);
            int notificationNumber = SharedPreferenceManager.load(Global.SharedPreference.LAST_NOTIFICATION_NUMBER, 0) + 1;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel notificationChannel = new NotificationChannel(Global.Notification.CHANNEL_ID, Global.Notification.CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
                notificationManagerCompat.createNotificationChannel(notificationChannel);
            }

            notificationManagerCompat.notify(notificationNumber, notification.build());
            notificationManagerCompat.notify(0, notificationGroup.build());
            SharedPreferenceManager.save(Global.SharedPreference.LAST_NOTIFICATION_NUMBER, notificationNumber);
            SharedPreferenceManager.close();
        }

        if (remoteMessage.getNotification() != null) {
            Log.d("UOS_MOBILE_FCM", "Message Notification Body: " + remoteMessage.getNotification().getBody());
            Toast.makeText(getApplicationContext(), remoteMessage.getNotification().getBody(), Toast.LENGTH_SHORT).show();
        }
    }
}
