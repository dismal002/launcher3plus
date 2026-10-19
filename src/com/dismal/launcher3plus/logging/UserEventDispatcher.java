package com.dismal.launcher3plus.logging;
import android.content.Intent;
import android.view.View;
import com.dismal.launcher3plus.DropTarget;
import com.dismal.launcher3plus.ItemInfo;
import com.dismal.launcher3plus.userevent.LauncherLogProto.Target;
import com.dismal.launcher3plus.util.ComponentKey;
import java.util.List;

public class UserEventDispatcher {

    public interface LogContainerProvider {
        void fillInLogContainerData(View v, ItemInfo info,
                Target.Builder target, Target.Builder targetParent);
    }

    public static UserEventDispatcher newInstance(android.content.Context ctx,
            boolean landscape, boolean multiWindow) { return new UserEventDispatcher(); }

    public void logAppLaunch(View v, Intent intent) {}
    public void logActionTip(int actionType, int viewType) {}
    public void logActionOnItem(int action, int dir, int itemType) {}
    public void logActionOnItem(int action, int dir, int itemType, int gridX, int gridY) {}
    public void logActionOnControl(int action, int controlType) {}
    public void logActionOnControl(int action, int controlType, View itemView) {}
    public void logActionOnControl(int action, int controlType, int parentContainerType) {}
    public void logActionOnContainer(int action, int dir, int containerType) {}
    public void logActionOnContainer(int action, int dir, int containerType, int pageIndex) {}
    public void logActionCommand(int command, int containerType) {}
    public void logActionCommand(int command, View itemView, int containerType) {}
    public void logActionCommand(int command, int containerType, int pageIndex) {}
    public void logDeepShortcutsOpen(View icon) {}
    public void logDragNDrop(DropTarget.DragObject dragObj, View dropTargetAsView) {}
    public void logNotificationFired(String packageName, int notificationCount) {}
    public void logNotificationLaunch(View v, android.app.PendingIntent intent) {}
    public void logActionTapOutside(com.dismal.launcher3plus.userevent.LauncherLogProto.Target t) {}
    public void dispatchUserEvent(com.dismal.launcher3plus.userevent.LauncherLogProto.LauncherEvent e, Object o) {}
    public void logOverviewReorder() {}
    public void logTaskStackSizeChanged(int taskStackSize, int taskViewCount) {}
    public void setPredictedApps(boolean enabled, List<ComponentKey> apps) {}
    public void setPredictedApps(List<ComponentKey> apps) {}
    public void setElapsedContainerMillis(long ms) {}
    public void resetElapsedContainerMillis() {}
    public void resetElapsedSessionMillis() {}
    public void resetActionDurationMillis() {}
    public void startSession() {}
    public void initSession() {}
    public boolean isSessionActive() { return false; }
}
