package vn.vinabacus.model;

import java.math.BigDecimal;

/** Khoa hoc Trung tam; hoc phi luu bang DECIMAL(18,2) trong SQL Server. */
public record KhoaHoc(int maKhoaHoc, int maCoSo, int maMonHoc,
                      String tenMonHoc, String tenKhoaHoc, int tongSoBuoi,
                      BigDecimal hocPhiMacDinh, boolean trangThai) {
}
