package vn.vinabacus.repository;

import java.util.List;
import vn.vinabacus.model.ThongTinTuyenSinh;

/** Hợp đồng đọc dữ liệu module; service không phụ thuộc cách lưu trữ. */
public interface TuyenSinhRepository {
    List<ThongTinTuyenSinh> layDanhSach(int maCoSo);
}
