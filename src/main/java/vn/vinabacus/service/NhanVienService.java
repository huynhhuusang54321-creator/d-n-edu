package vn.vinabacus.service;

import java.sql.SQLException;
import java.util.List;
import java.time.LocalDate;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.ChiTietGiaoVienTrungTam;
import vn.vinabacus.model.NhanVien;
import vn.vinabacus.repository.CoSoRepository;
import vn.vinabacus.repository.NhanVienRepository;

public class NhanVienService {

    private final NhanVienRepository nhanVienRepository;
    private final CoSoRepository coSoRepository;

    public NhanVienService() {
        this(new NhanVienRepository(), new CoSoRepository());
    }

    NhanVienService(NhanVienRepository nhanVienRepository,
                   CoSoRepository coSoRepository) {
        this.nhanVienRepository = java.util.Objects.requireNonNull(nhanVienRepository);
        this.coSoRepository = java.util.Objects.requireNonNull(coSoRepository);
    }

    public List<NhanVien> layDanhSach(CoSo coSo)
            throws SQLException {

        int maCoSo = coSoRepository.timMaCoSo(coSo);

        return nhanVienRepository.timTheoCoSo(maCoSo);
    }

    public int themNhanVien(
            CoSo coSo,
            String hoTen,
            LocalDate ngaySinh,
            String gioiTinh,
            String soDienThoai,
            String email) throws SQLException {

        // Kiem tra co so
        if (coSo == null) {
            throw new IllegalArgumentException(
                    "Vui long chon co so!"
            );
        }

        // Kiem tra ho ten
        if (hoTen == null || hoTen.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Ho ten khong duoc de trong!"
            );
        }

        hoTen = hoTen.trim();

        if (hoTen.length() > 150) {
            throw new IllegalArgumentException(
                    "Ho ten khong duoc qua 150 ky tu!"
            );
        }

        // Kiem tra gioi tinh
        if (gioiTinh != null && !gioiTinh.isBlank()) {
            if (!gioiTinh.equals("NAM")
                    && !gioiTinh.equals("NU")
                    && !gioiTinh.equals("KHAC")) {
                throw new IllegalArgumentException(
                        "Gioi tinh khong hop le!"
                );
            }
        } else {
            gioiTinh = null;
        }

        // Kiem tra so dien thoai
        if (soDienThoai != null) {
            soDienThoai = soDienThoai.trim();

            if (soDienThoai.isEmpty()) {
                soDienThoai = null;
            } else if (soDienThoai.length() > 20) {
                throw new IllegalArgumentException(
                        "So dien thoai khong duoc qua 20 ky tu!"
                );
            }
        }

        // Kiem tra email
        if (email != null) {
            email = email.trim();

            if (email.isEmpty()) {
                email = null;
            } else if (email.length() > 254
                    || !email.matches(
                            "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
                    )) {
                throw new IllegalArgumentException(
                        "Email khong hop le!"
                );
            }
        }

        // Lay ma co so tu SQL Server
        int maCoSo = coSoRepository.timMaCoSo(coSo);

        // Goi Repository de them nhan vien
        return nhanVienRepository.themNhanVien(
                maCoSo,
                hoTen,
                ngaySinh,
                gioiTinh,
                soDienThoai,
                email
        );
    }

    public boolean suaNhanVien(
            CoSo coSo,
            int maNhanVien,
            String hoTen,
            LocalDate ngaySinh,
            String gioiTinh,
            String soDienThoai,
            String email) throws SQLException {

        // Kiem tra co so
        if (coSo == null) {
            throw new IllegalArgumentException(
                    "Vui long chon co so!"
            );
        }

        // Kiem tra ma nhan vien
        if (maNhanVien <= 0) {
            throw new IllegalArgumentException(
                    "Ma nhan vien khong hop le!"
            );
        }

        // Kiem tra ho ten
        if (hoTen == null || hoTen.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Ho ten khong duoc de trong!"
            );
        }

        hoTen = hoTen.trim();

        if (hoTen.length() > 150) {
            throw new IllegalArgumentException(
                    "Ho ten khong duoc qua 150 ky tu!"
            );
        }

        // Kiem tra gioi tinh
        if (gioiTinh != null && !gioiTinh.isBlank()) {

            if (!gioiTinh.equals("NAM")
                    && !gioiTinh.equals("NU")
                    && !gioiTinh.equals("KHAC")) {

                throw new IllegalArgumentException(
                        "Gioi tinh khong hop le!"
                );
            }

        } else {
            gioiTinh = null;
        }

        // Kiem tra so dien thoai
        if (soDienThoai != null) {

            soDienThoai = soDienThoai.trim();

            if (soDienThoai.isEmpty()) {
                soDienThoai = null;
            } else if (soDienThoai.length() > 20) {
                throw new IllegalArgumentException(
                        "So dien thoai khong duoc qua 20 ky tu!"
                );
            }
        }

        // Kiem tra email
        if (email != null) {

            email = email.trim();

            if (email.isEmpty()) {
                email = null;
            } else if (email.length() > 254
                    || !email.matches(
                            "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
                    )) {

                throw new IllegalArgumentException(
                        "Email khong hop le!"
                );
            }
        }

        // Lay ma co so tu SQL Server
        int maCoSo = coSoRepository.timMaCoSo(coSo);

        // Goi Repository de cap nhat nhan vien
        return nhanVienRepository.suaNhanVien(
                maCoSo,
                maNhanVien,
                hoTen,
                ngaySinh,
                gioiTinh,
                soDienThoai,
                email
        );
    }

    public boolean ngungCongTac(
            CoSo coSo,
            int maNhanVien) throws SQLException {

        // Kiem tra co so
        if (coSo == null) {
            throw new IllegalArgumentException(
                    "Vui long chon co so!"
            );
        }

        // Kiem tra ma nhan vien
        if (maNhanVien <= 0) {
            throw new IllegalArgumentException(
                    "Ma nhan vien khong hop le!"
            );
        }

        // Lay ma co so tu SQL Server
        int maCoSo = coSoRepository.timMaCoSo(coSo);

        // Cap nhat trang thai cong tac tai co so
        return coSo == CoSo.TRUNG_TAM
                ? nhanVienRepository.ngungCongTacTrungTam(maCoSo, maNhanVien)
                : nhanVienRepository.ngungCongTac(maCoSo, maNhanVien);
    }

    public ChiTietGiaoVienTrungTam chiTietPhanCongTrungTam(
            CoSo coSo, int maNhanVien) throws SQLException {
        if (coSo != CoSo.TRUNG_TAM)
            throw new IllegalArgumentException("Chi tiết phân công này chỉ dành cho Trung tâm");
        if (maNhanVien <= 0)
            throw new IllegalArgumentException("Mã nhân viên không hợp lệ");
        return nhanVienRepository.chiTietPhanCongTrungTam(
                coSoRepository.timMaCoSo(coSo), maNhanVien);
    }

public boolean lamViecTroLai(
        CoSo coSo,
        int maNhanVien) throws SQLException {

    // Kiem tra co so
    if (coSo == null) {
        throw new IllegalArgumentException(
            "Vui long chon co so!"
        );
    }

    // Kiem tra ma nhan vien
    if (maNhanVien <= 0) {
        throw new IllegalArgumentException(
            "Ma nhan vien khong hop le!"
        );
    }

    // Lay ma co so tu SQL Server
    int maCoSo = coSoRepository.timMaCoSo(coSo);

    // Goi Repository de kich hoat lai cong tac
    return nhanVienRepository.lamViecTroLai(
        maCoSo,
        maNhanVien
    );
}

}
