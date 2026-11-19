package vn.vinabacus.controller;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import vn.vinabacus.repository.LichHocLopRepository.BuoiTrongTuan;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.KhoaHoc;
import vn.vinabacus.model.LichHocLop;
import vn.vinabacus.model.LopTrungTam;
import vn.vinabacus.model.MonHoc;
import vn.vinabacus.model.PhongHoc;
import vn.vinabacus.service.LichHocLopService;

public final class LichHocLopController {
    private final LichHocLopService service = new LichHocLopService(CoSo.TRUNG_TAM);

    public List<MonHoc> layMonHoc() throws SQLException { return service.layMonHoc(); }
    public List<KhoaHoc> layKhoaHoc() throws SQLException { return service.layKhoaHoc(); }
    public List<LopTrungTam> layLop() throws SQLException { return service.layLop(); }
    public List<PhongHoc> layPhongHoc() throws SQLException { return service.layPhongHoc(); }
    public List<LichHocLop> timKiem(String tuKhoa, String trangThai) throws SQLException {
        return service.timKiem(tuKhoa, trangThai);
    }
    public List<Integer> themNhieu(int maLop, List<BuoiTrongTuan> cacBuoi,
                                   LocalDate tuNgay, LocalDate denNgay, String trangThai)
            throws SQLException {
        return service.themNhieu(maLop, cacBuoi, tuNgay, denNgay, trangThai);
    }

    public int them(int maLop, int thu, LocalTime batDau, LocalTime ketThuc,
                    LocalDate tuNgay, LocalDate denNgay, String trangThai) throws SQLException {
        return service.them(maLop, thu, batDau, ketThuc, tuNgay, denNgay, trangThai);
    }
    public void sua(int maLichHoc, int thu, LocalTime batDau, LocalTime ketThuc,
                    LocalDate tuNgay, LocalDate denNgay, String trangThai) throws SQLException {
        service.sua(maLichHoc, thu, batDau, ketThuc, tuNgay, denNgay, trangThai);
    }
}
