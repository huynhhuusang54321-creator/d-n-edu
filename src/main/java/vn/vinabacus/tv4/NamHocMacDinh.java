package vn.vinabacus.tv4;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;
import java.util.prefs.Preferences;
import vn.vinabacus.model.NamHocMamNon;

/** Năm học mặc định trên máy đang dùng; không can thiệp cấu trúc SQL hoặc tài khoản khác. */
public final class NamHocMacDinh {
    private static final String KEY="mamnon.namhoc.macdinh";
    private static final Preferences PREF=Preferences.userNodeForPackage(NamHocMacDinh.class);
    private static final PropertyChangeSupport EVENTS=new PropertyChangeSupport(NamHocMacDinh.class);
    private NamHocMacDinh(){}
    public static int maDaChon(){return PREF.getInt(KEY,0);}
    public static void chon(int ma){
        int before=maDaChon();
        if(ma>0)PREF.putInt(KEY,ma);else PREF.remove(KEY);
        EVENTS.firePropertyChange("namHoc",before,ma);
    }
    public static NamHocMamNon chonHieuLuc(List<NamHocMamNon> years){
        if(years==null||years.isEmpty())return null;
        int chosen=maDaChon();
        for(NamHocMamNon y:years)if(y.maNamHoc()==chosen)return y;
        // "Được tạo mới nhất": mã IDENTITY lớn nhất, không phải ngày bắt đầu muộn nhất.
        return years.stream().max(java.util.Comparator.comparingInt(NamHocMamNon::maNamHoc)).orElse(null);
    }
    public static void langNghe(PropertyChangeListener listener){EVENTS.addPropertyChangeListener("namHoc",listener);}
    public static void ngungNghe(PropertyChangeListener listener){EVENTS.removePropertyChangeListener("namHoc",listener);}
}
