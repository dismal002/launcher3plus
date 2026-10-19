package com.dismal.launcher3plus.logging;
import android.view.View;
import com.dismal.launcher3plus.ItemInfo;
import com.dismal.launcher3plus.userevent.LauncherLogProto.*;
public class LoggerUtils {
    public static Action buildTouchAction(int t) { return new Action(); }
    public static Action buildCommandAction(int c) { return new Action(); }
    public static Action newTouchAction(int t) { return new Action(); }
    public static Action newCommandAction(int c) { return new Action(); }
    public static Target buildTarget(int t) { return new Target(); }
    public static Target newTarget(int t) { return new Target(); }
    public static Target.Builder buildTargetBuilder(int t) { return Target.newBuilder(); }
    public static Target.Builder newTargetBuilder(int t) { return Target.newBuilder(); }
    public static Target newContainerTarget(int c) { return new Target(); }
    public static Target newItemTarget(View v) { return new Target(); }
    public static Target newItemTarget(ItemInfo info) { return new Target(); }
    public static LauncherEvent newLauncherEvent(Action a, Target... targets) { return new LauncherEvent(); }
    public static String getActionStr(Action a) { return ""; }
    public static String getTargetStr(Target t) { return ""; }
    public static <T> String getFieldName(int val, Class<T> c) { return String.valueOf(val); }
    public static View getDropTargetAsView(View v) { return v; }
}
