package com.dismal.launcher3plus.model;
public class LauncherDumpProto {
    public static class UserType { public static final int LOCAL_PROFILE=0,DEFAULT=0,WORK_PROFILE=1,WORK=1; public int getNumber(){return 0;} }
    public static class ContainerType { public static final int WORKSPACE=0,HOTSEAT=1,FOLDER=2; public int getNumber(){return 0;} }
    public static class ItemType { public static final int APP_ICON=0,SHORTCUT=1,WIDGET=2,FOLDER_ICON=3,UNKNOWN_ITEMTYPE=99; }
    public static class DumpTarget {
        public static Builder newBuilder(){return new Builder();}
        public Builder toBuilder(){return new Builder();}
        public static class Builder {
            public DumpTarget build(){return new DumpTarget();}
            public Builder setType(int t){return this;}
            public Builder setContainerType(int c){return this;}
            public Builder setItemType(int i){return this;}
            public Builder setGridX(int v){return this;}
            public Builder setGridY(int v){return this;}
            public Builder setSpanX(int v){return this;}
            public Builder setSpanY(int v){return this;}
            public Builder setPageId(int v){return this;}
            public Builder setUserType(int v){return this;}
            public Builder setComponent(String v){return this;}
            public Builder setPackageName(String v){return this;}
        }
    }
    public static class LauncherImpression {
        public static Builder newBuilder(){return new Builder();}
        public Builder toBuilder(){return new Builder();}
        public byte[] toByteArray(){return new byte[0];}
        public static class Builder {
            public LauncherImpression build(){return new LauncherImpression();}
            public Builder addAllTargets(java.util.List<?> targets){return this;}
        }
    }
}
