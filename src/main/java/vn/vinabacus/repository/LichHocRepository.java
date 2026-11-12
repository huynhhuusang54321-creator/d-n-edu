package vn.vinabacus.repository;

import java.util.List;
import vn.vinabacus.model.ThongTinLichHoc;

/** Hợp đồng đọc dữ liệu module; service không phụ thuộc cách lưu trữ. */
public interface LichHocRepository {
    List<ThongTinLichHoc> layDanhSach(int maCoSo);
}
