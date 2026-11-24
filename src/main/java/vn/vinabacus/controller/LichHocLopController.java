package vn.vinabacus.controller;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import vn.vinabacus.repository.LichHocLopRepository.BuoiTrongTuan;
import vn.vinabacus.repository.LichHocLopRepository.BuoiCoPhong;
import vn.vinabacus.repository.LichHocLopRepository.HocVienTheoLop;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.KhoaHoc;
import vn.vinabacus.model.LichHocLop;
import vn.vinabacus.model.LopTrungTam;
import vn.vinabacus.model.MonHoc;
import vn.vinabacus.model.PhongHoc;
import vn.vinabacus.service.LichHocLopService;
import vn.vinabacus.service.DuKienKetThucLop;

public final class LichHocLopController {
    private final LichHocLopService service = new LichHocLopService(CoSo.TRUNG_TAM);

    public List<MonHoc> layMonHoc() throws SQLException { return service.layMonHoc(); }
    public List<KhoaHoc> layKhoaHoc() throws SQLException { return service.layKhoaHoc(); }
    public List<LopTrungTam> layLop() throws SQLException { return service.layLop(); }
    public List<PhongHoc> layPhongHoc() throws SQLException { return service.layPhongHoc(); }
    public List<HocVienTheoLop> layHocVienTheoLop() throws SQLException {
        return service.layHocVienTheoLop();
    }
    public List<DuKienKetThucLop.BuoiDieuChinh> layBuoiDieuChinh() throws SQLException {
        return service.layBuoiDieuChinh();
    }
    public List<LichHocLop> timKiem(String tuKhoa, String trangThai) throws SQLException {
        return service.timKiem(tuKhoa, trangThai);
    }
    public List<Integer> themNhieu(int maLop, Integer maPhong, List<BuoiTrongTuan> cacBuoi,
                                   LocalDate tuNgay, LocalDate denNgay, String trangThai)
            throws SQLException {
        return service.themNhieu(maLop, maPhong, cacBuoi, tuNgay, denNgay, trangThai);
    }

    public List<Integer> themNhieuTheoPhong(int maLop, List<BuoiCoPhong> cacBuoi,
                                           LocalDate tuNgay, LocalDate denNgay, String trangThai)
            throws SQLException {
        return service.themNhieuTheoPhong(maLop, cacBuoi, tuNgay, denNgay, trangThai);
    }

    public int them(int maLop, Integer maPhong, int thu, LocalTime batDau, LocalTime ketThuc,
                    LocalDate tuNgay, LocalDate denNgay, String trangThai) throws SQLException {
        return service.them(maLop, maPhong, thu, batDau, ketThuc, tuNgay, denNgay, trangThai);
    }
    public void sua(int maLichHoc, Integer maPhong, int thu, LocalTime batDau, LocalTime ketThuc,
                    LocalDate tuNgay, LocalDate denNgay, String trangThai) throws SQLException {
        service.sua(maLichHoc, maPhong, thu, batDau, ketThuc, tuNgay, denNgay, trangThai);
    }
    public int suaTuNgay(int maLichHoc, Integer maPhong, int thu,
                         LocalTime batDau, LocalTime ketThuc,
                         LocalDate ngayHieuLuc, LocalDate denNgay, String trangThai)
            throws SQLException {
        return service.suaTuNgay(maLichHoc, maPhong, thu, batDau,
                ketThuc, ngayHieuLuc, denNgay, trangThai);
    }

    public int huyBuoi(int maLichHoc, LocalDate ngayHoc, String lyDo)
            throws SQLException {
        return service.huyBuoi(maLichHoc, ngayHoc, lyDo);
    }

    public int themBuoiBu(int maLop, Integer maPhong, LocalDate ngayHoc,
                          int soCa, String ghiChu) throws SQLException {
        return service.themBuoiBu(maLop, maPhong, ngayHoc, soCa, ghiChu);
    }
}
