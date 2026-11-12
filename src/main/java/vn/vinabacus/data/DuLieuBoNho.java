package vn.vinabacus.data;

import java.util.*;
import vn.vinabacus.model.MoTaManHinh;

/** Nguồn lưu dữ liệu demo theo phiên chạy; chưa có kết nối SQL Server. */
public final class DuLieuBoNho {
    private static final Map<String, List<LinkedHashMap<String, String>>> DU_LIEU = new LinkedHashMap<>();
    private static final Map<String, String> DIEM_DANH = new LinkedHashMap<>();

    private DuLieuBoNho() {}

    private static List<LinkedHashMap<String, String>> duLieu(MoTaManHinh.Trang trang) {
        Objects.requireNonNull(trang, "Trang không được null");
        return DU_LIEU.computeIfAbsent(trang.khoa(), key -> {
            List<LinkedHashMap<String, String>> rows = new ArrayList<>();
            for (String[] values : trang.mau()) {
                LinkedHashMap<String, String> row = new LinkedHashMap<>();
                for (int i = 0; i < trang.cot().length; i++) {
                    row.put(trang.cot()[i], i < values.length ? values[i] : "");
                }
                rows.add(row);
            }
            return rows;
        });
    }

    /** Trả về bản sao để người gọi không thay đổi dữ liệu qua kết quả đọc. */
    public static synchronized List<LinkedHashMap<String, String>> lay(MoTaManHinh.Trang trang) {
        List<LinkedHashMap<String, String>> result = new ArrayList<>();
        for (Map<String, String> row : duLieu(trang)) result.add(new LinkedHashMap<>(row));
        return result;
    }

    public static synchronized void them(MoTaManHinh.Trang trang, Map<String, String> giaTri) {
        Objects.requireNonNull(giaTri, "Giá trị không được null");
        List<LinkedHashMap<String, String>> rows = duLieu(trang);
        LinkedHashMap<String, String> row = new LinkedHashMap<>();
        for (String key : trang.cot()) row.put(key, giaTri.getOrDefault(key, ""));
        giaTri.forEach(row::putIfAbsent);
        if (trang.cot().length > 0) {
            String id = trang.cot()[0];
            if (row.getOrDefault(id, "").isBlank()) row.put(id, taoMa(rows, trang));
        }
        rows.add(row);
    }

    private static String taoMa(List<LinkedHashMap<String, String>> rows, MoTaManHinh.Trang trang) {
        String name = trang.tab().replaceAll("[^A-Za-z]", "").toUpperCase(Locale.ROOT);
        String prefix = name.length() > 2 ? name.substring(0, 2) : "VB";
        Set<String> ids = new HashSet<>();
        String key = trang.cot()[0];
        for (Map<String, String> row : rows) ids.add(row.get(key));
        int next = rows.size() + 1;
        String candidate;
        do { candidate = prefix + String.format(Locale.ROOT, "%04d", next++); }
        while (ids.contains(candidate));
        return candidate;
    }

    public static synchronized void sua(MoTaManHinh.Trang trang, int pos, Map<String, String> giaTri) {
        duLieu(trang).get(pos).putAll(giaTri);
    }
    public static synchronized void xoa(MoTaManHinh.Trang trang, int pos) { duLieu(trang).remove(pos); }
    public static synchronized void diemDanh(String key, String value) { DIEM_DANH.put(key, value); }
    public static synchronized String layDiemDanh(String key) { return DIEM_DANH.get(key); }
    public static synchronized int tongDiemDanh() { return DIEM_DANH.size(); }
}
