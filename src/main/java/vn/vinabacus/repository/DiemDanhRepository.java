package vn.vinabacus.repository;

import java.util.List;
import vn.vinabacus.model.ThongTinDiemDanh;

/** Hợp đồng đọc dữ liệu module; service không phụ thuộc cách lưu trữ. */
public interface DiemDanhRepository {
    List<ThongTinDiemDanh> layDanhSach(int maCoSo);
}
