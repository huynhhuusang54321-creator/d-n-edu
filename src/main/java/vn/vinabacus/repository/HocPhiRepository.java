package vn.vinabacus.repository;

import java.util.List;
import vn.vinabacus.model.ThongTinHocPhi;

/** Hợp đồng đọc dữ liệu module; service không phụ thuộc cách lưu trữ. */
public interface HocPhiRepository {
    List<ThongTinHocPhi> layDanhSach(int maCoSo);
}
