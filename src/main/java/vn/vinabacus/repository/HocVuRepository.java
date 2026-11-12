package vn.vinabacus.repository;

import java.util.List;
import vn.vinabacus.model.ThongTinHocVu;

/** Hợp đồng đọc dữ liệu module; service không phụ thuộc cách lưu trữ. */
public interface HocVuRepository {
    List<ThongTinHocVu> layDanhSach(int maCoSo);
}
