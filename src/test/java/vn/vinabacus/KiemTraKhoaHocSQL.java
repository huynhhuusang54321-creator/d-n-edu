
package vn.vinabacus;

import vn.vinabacus.data.DuLieuKhoaHoc;
import vn.vinabacus.model.ThongTinKhoaHoc;

public class KiemTraKhoaHocSQL {

    public static void main(String[] args) {

        // Thay 2 bang MaCoSo thuc te cua Trung tam
        int maCoSo = 2;

        DuLieuKhoaHoc duLieu = new DuLieuKhoaHoc();

        try {
            var danhSach = duLieu.layDanhSach(maCoSo);

            System.out.println(
                "So khoa hoc: " + danhSach.size()
            );

            for (ThongTinKhoaHoc kh : danhSach) {
                System.out.println(
                    kh.ma() + " - " +
                    kh.ten() + " - " +
                    kh.trangThai()
                );
            }

        } catch (Exception e) {
            System.out.println("Loi lay danh sach khoa hoc!");
            e.printStackTrace();
        }
    }
}
