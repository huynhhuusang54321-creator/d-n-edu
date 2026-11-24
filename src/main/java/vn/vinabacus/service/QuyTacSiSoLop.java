package vn.vinabacus.service;

/** Muốn nhận học viên cần cả giới hạn riêng của lớp lẫn phòng đủ sức chứa. */
public final class QuyTacSiSoLop {
    private QuyTacSiSoLop() { }

    public record KetQua(int gioiHan, int conCho, String trangThai) { }

    public static KetQua danhGia(Integer siSoToiDa, Integer sucChuaPhong,
                                  int soDaGiuCho) {
        if (soDaGiuCho < 0)
            throw new IllegalArgumentException("Số chỗ đã giữ không hợp lệ");
        if (siSoToiDa == null || siSoToiDa <= 0)
            return new KetQua(0, 0, "Chưa đặt sĩ số tối đa");
        if (sucChuaPhong == null)
            return new KetQua(0, 0, "Chưa gán phòng học");
        if (sucChuaPhong <= 0)
            return new KetQua(0, 0, "Sức chứa phòng không hợp lệ");
        if (siSoToiDa > sucChuaPhong)
            return new KetQua(0, 0, "Sĩ số lớp vượt sức chứa phòng");
        int gioiHan = siSoToiDa;
        int conCho = Math.max(0, gioiHan - soDaGiuCho);
        return new KetQua(gioiHan, conCho,
                conCho == 0 ? "Đủ sĩ số" : "Đang nhận");
    }
}
