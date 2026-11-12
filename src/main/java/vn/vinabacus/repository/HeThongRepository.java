package vn.vinabacus.repository;

import java.util.List;
import vn.vinabacus.model.ThongTinHeThong;

/** Hợp đồng đọc dữ liệu module; service không phụ thuộc cách lưu trữ. */
public interface HeThongRepository {
    List<ThongTinHeThong> layDanhSach(int maCoSo);
}
