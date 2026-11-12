package vn.vinabacus.repository;

import java.util.List;
import vn.vinabacus.model.ThongTinTienAn;

/** Hợp đồng đọc dữ liệu module; service không phụ thuộc cách lưu trữ. */
public interface TienAnRepository {
    List<ThongTinTienAn> layDanhSach(int maCoSo);
}
