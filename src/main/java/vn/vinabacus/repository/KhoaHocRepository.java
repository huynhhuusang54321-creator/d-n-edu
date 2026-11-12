package vn.vinabacus.repository;

import java.util.List;
import vn.vinabacus.model.ThongTinKhoaHoc;

/** Hợp đồng đọc dữ liệu module; service không phụ thuộc cách lưu trữ. */
public interface KhoaHocRepository {
    List<ThongTinKhoaHoc> layDanhSach(int maCoSo);
}
