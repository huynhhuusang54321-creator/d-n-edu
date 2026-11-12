package vn.vinabacus.repository;

import java.util.List;
import vn.vinabacus.model.ThongTinHocVien;

/** Hợp đồng đọc dữ liệu module; service không phụ thuộc cách lưu trữ. */
public interface HocVienRepository {
    List<ThongTinHocVien> layDanhSach(int maCoSo);
}
