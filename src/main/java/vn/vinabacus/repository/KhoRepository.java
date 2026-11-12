package vn.vinabacus.repository;

import java.util.List;
import vn.vinabacus.model.ThongTinKho;

/** Hợp đồng đọc dữ liệu module; service không phụ thuộc cách lưu trữ. */
public interface KhoRepository {
    List<ThongTinKho> layDanhSach(int maCoSo);
}
