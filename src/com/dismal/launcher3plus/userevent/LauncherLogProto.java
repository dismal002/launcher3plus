package com.dismal.launcher3plus.userevent;
public class LauncherLogProto {
    public static class Action {
        public enum Touch { TAP(0),LONGPRESS(1),DRAGDROP(2),SWIPE(3),FLING(4),PINCH(5); private final int n; Touch(int n){this.n=n;} public int getNumber(){return n;} public static Touch forNumber(int n){for(Touch v:values())if(v.n==n)return v;return TAP;} }
        public enum Command { HOME_INTENT(0),BACK(1),RECENTS(2),ENTRY(3),CANCEL(4),CONFIRM(5); private final int n; Command(int n){this.n=n;} public int getNumber(){return n;} public static Command forNumber(int n){for(Command v:values())if(v.n==n)return v;return HOME_INTENT;} }
        public enum Direction { NONE(0),UP(1),DOWN(2),LEFT(3),RIGHT(4); private final int n; Direction(int n){this.n=n;} public int getNumber(){return n;} public static Direction forNumber(int n){for(Direction v:values())if(v.n==n)return v;return NONE;} }
        public enum Type { TOUCH(0),COMMAND(1),TIP(2); private final int n; Type(int n){this.n=n;} public int getNumber(){return n;} public static Type forNumber(int n){for(Type v:values())if(v.n==n)return v;return TOUCH;} }
        public int type; public int touch; public int command; public int direction;
        public Type getType(){return Type.TOUCH;} public Touch getTouch(){return Touch.TAP;} public Command getCommand(){return Command.HOME_INTENT;}
        public static Builder newBuilder(){return new Builder();}
        public static class Builder { public Action build(){return new Action();} public Builder setTouch(Touch t){return this;} public Builder setCommand(Command c){return this;} public Builder setDirection(Direction d){return this;} public Builder setType(Type t){return this;} }
    }
    public static class Target {
        public enum Type { ITEM(0),CONTROL(1),CONTAINER(2); private final int n; Type(int n){this.n=n;} public int getNumber(){return n;} public static Type forNumber(int n){for(Type v:values())if(v.n==n)return v;return ITEM;} }
        public int type,itemType,containerType,controlType,pageIndex,gridX,gridY,spanX,spanY,packageNameHash,componentHash,intentHash,rank;
        public Type getType(){return Type.ITEM;} public int getItemType(){return itemType;} public int getContainerType(){return containerType;} public int getControlType(){return controlType;} public int getPageIndex(){return pageIndex;} public int getGridX(){return gridX;} public int getGridY(){return gridY;} public int getSpanX(){return spanX;} public int getSpanY(){return spanY;} public int getPackageNameHash(){return packageNameHash;} public int getComponentHash(){return componentHash;} public int getIntentHash(){return intentHash;}
        public Builder toBuilder(){return new Builder();}
        public static Builder newBuilder(){return new Builder();}
        public static class Builder { private Target t=new Target(); public Target build(){return t;} public Builder setType(Type v){t.type=v.getNumber();return this;} public Builder setItemType(int v){t.itemType=v;return this;} public Builder setContainerType(int v){t.containerType=v;return this;} public Builder setControlType(int v){t.controlType=v;return this;} public Builder setPageIndex(int v){t.pageIndex=v;return this;} public Builder setGridX(int v){t.gridX=v;return this;} public Builder setGridY(int v){t.gridY=v;return this;} public Builder setSpanX(int v){t.spanX=v;return this;} public Builder setSpanY(int v){t.spanY=v;return this;} public Builder setRank(int v){t.rank=v;return this;} public Builder setPackageNameHash(int v){t.packageNameHash=v;return this;} public Builder setComponentHash(int v){t.componentHash=v;return this;} public Builder setIntentHash(int v){t.intentHash=v;return this;} }
    }
    public static class ContainerType { public static final int WORKSPACE=0,HOTSEAT=1,FOLDER=2,ALLAPPS=3,WIDGETS=4,OVERVIEW=5,PREDICTION=6,SEARCHRESULT=7,DEEPSHORTCUTS=8,PINITEM=9; }
    public static class ItemType { public static final int APP_ICON=0,SHORTCUT=1,WIDGET=2,FOLDER_ICON=3,DEEPSHORTCUT=4,SEARCHRESULT=5,EDITTEXT=6,PREDICTION=7,NOTIFICATION=8; }
    public static class ControlType { public static final int UNSET=0,ALL_APPS_BUTTON=1,WIDGETS_BUTTON=2,WALLPAPER_BUTTON=3,SETTINGS_BUTTON=4,REMOVE_TARGET=5,UNINSTALL_TARGET=6,APPINFO_TARGET=7,DISMISS_NOTIFICATION=8; }
    public static class LauncherEvent { public static Builder newBuilder(){return new Builder();} public static class Builder { public LauncherEvent build(){return new LauncherEvent();} public Builder setAction(Action a){return this;} public Builder addSrcTarget(Target t){return this;} public Builder addDestTarget(Target t){return this;} public Builder setElapsedContainerMillis(long ms){return this;} public Builder setElapsedSessionMillis(long ms){return this;} } }
}
