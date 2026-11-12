package vn.vinabacus.repository;

import java.util.List;
import vn.vinabacus.model.ThongTinLopHoc;

/** Hợp đồng đọc dữ liệu module; service không phụ thuộc cách lưu trữ. */
public interface LopHocRepository {
    List<ThongTinLopHoc> layDanhSach(int maCoSo);
}
