/*
 *  Copyright 2015 Erkan Molla
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.em.batterywidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.widget.RemoteViews;

public class BatteryWidget extends AppWidgetProvider {

    static String TAG = "BatteryWidget";
    /**
     * Get the number of widgets that have been enabled and placed on the home screen.
     *
     * @param context The application context.
     */
    public static int getNumberOfWidgets(final Context context) {
        ComponentName componentName = new ComponentName(context, BatteryWidget.class);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] activeWidgetIds = appWidgetManager.getAppWidgetIds(componentName);
        if (activeWidgetIds != null) {
            return activeWidgetIds.length;
        } else {
            return 0;
        }
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        context.startService(new Intent(context, MonitorService.class));
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager widgetManager, int[] widgetIds) {
        super.onUpdate(context, widgetManager, widgetIds);
        // ensure service is running
        context.startService(new Intent(context, MonitorService.class));
        // update the widgets
        Intent updateIntent = new Intent(context, UpdateService.class);
        updateIntent.setAction(UpdateService.ACTION_WIDGET_UPDATE);
        updateIntent.putExtra(UpdateService.EXTRA_WIDGET_IDS, widgetIds);
        context.startService(updateIntent);
    }

    @Override
    public void onDeleted(Context context, int[] widgetIds) {
        super.onDeleted(context, widgetIds);
        if (getNumberOfWidgets(context) == 0) {
            // stop monitoring if there are no more widgets on screen
            context.stopService(new Intent(context, MonitorService.class));
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions){

        super.onAppWidgetOptionsChanged(context,appWidgetManager,appWidgetId,newOptions);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            int maxWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
            int maxHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);

            int size = Math.min(maxHeight,maxWidth);
            Log.i(TAG, "min size="+size);
            int pxSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,size,context.getResources().getDisplayMetrics());

            RemoteViews remoteViews = new RemoteViews(context.getPackageName(),R.layout.widget_view);
            float newSize = refitText("MMMM",pxSize);

            remoteViews.setTextViewTextSize(R.id.batterytext, TypedValue.COMPLEX_UNIT_PX,newSize);

            appWidgetManager.updateAppWidget(appWidgetId,remoteViews);
        }

    }
    private float refitText(String text, int textWidth)
    {
        if (textWidth <= 0)
            return 0;

        float hi = 100;
        float lo = 2;
        final float threshold = 0.5f; // How close we have to be

        Paint testPaint = new Paint();

        while((hi - lo) > threshold) {
            float size = (hi+lo)/2;
            testPaint.setTextSize(size);
            if(testPaint.measureText(text) >= textWidth)
                hi = size; // too big
            else
                lo = size; // too small
        }
        // Use lo so that we undershoot rather than overshoot
        return lo;
    }
}
