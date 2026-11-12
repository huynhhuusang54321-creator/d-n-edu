package vn.vinabacus.repository;

import java.util.*;
import vn.vinabacus.data.DuLieuBoNho;
import vn.vinabacus.data.DuLieuMauManHinh;
import vn.vinabacus.data.DuLieuMauLichTuan;
import vn.vinabacus.model.DuLieuMau;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.MoTaManHinh;

/** Điểm truy cập đặc tả và dữ liệu màn hình; nguồn hiện tại là bộ nhớ demo. */
public final class KhoDuLieuManHinh {
    private KhoDuLieuManHinh() {}

    public static List<MoTaManHinh.Trang> layTrang(CoSo coSo, String menu) {
        return List.copyOf(DuLieuMauManHinh.lay(coSo, menu));
    }
    public static DuLieuMau.Bang layLichTuan() { return DuLieuMauLichTuan.lay(); }
    public static List<Map<String,String>> hocVienDiemDanhBoSung() {
        return DuLieuMauManHinh.hocVienDiemDanhBoSung();
    }
    public static Collection<MoTaManHinh.Trang> tatCaTrang() { return DuLieuMauManHinh.tatCa(); }
    public static List<LinkedHashMap<String, String>> lay(MoTaManHinh.Trang trang) {
        return DuLieuBoNho.lay(trang);
    }
    public static void them(MoTaManHinh.Trang trang, Map<String, String> giaTri) {
        DuLieuBoNho.them(trang, giaTri);
    }
    public static void sua(MoTaManHinh.Trang trang, int pos, Map<String, String> giaTri) {
        DuLieuBoNho.sua(trang, pos, giaTri);
    }
    public static void xoa(MoTaManHinh.Trang trang, int pos) { DuLieuBoNho.xoa(trang, pos); }
    public static void diemDanh(String key, String value) { DuLieuBoNho.diemDanh(key, value); }
    public static String layDiemDanh(String key) { return DuLieuBoNho.layDiemDanh(key); }
    public static int tongDiemDanh() { return DuLieuBoNho.tongDiemDanh(); }
}
