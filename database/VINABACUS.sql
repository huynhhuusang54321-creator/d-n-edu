/* VINABACUS HOAN CHINH: CAI MOI 58 BANG + BO DU LIEU TEST HAI CO SO
   Database moi: cai schema va du lieu mau.
   Database VINABACUS da co dbo.CoSo: chi cap nhat PhongHoc/MaPhong,
   bo qua bo cai va du lieu demo phia sau.
   Khong tao he thong dang nhap hay phan quyen trong ung dung.
*/
-- =====================================================================
-- VINABACUS / SQL Server 2017+  --  CAU TRUC TOI UU, HocVien CO MaCoSo
-- Cai moi hoac cap nhat PhongHoc cho database dang co. Chay bang SSMS/sqlcmd.
-- Thay 01_TAO_BANG + 02_RANG_BUOC_CHI_MUC + 03_DANH_MUC cua file cu.
-- Sau file nay chay tiep cac phan thu tuc/view (04..09, goi nam) cua file cu.
-- Chua kiem thu tren SQL Server thuc te.
-- =============CREATE DATABASE [VINABACUS];========================================================

SET NOCOUNT ON;
IF DB_ID(N'VINABACUS') IS NULL EXEC(N'CREATE DATABASE [VINABACUS]');
GO
USE [VINABACUS];
GO
-- Database da co CoSo: cap nhat them danh muc phong tren VINABACUS.
-- CoSo chua ton tai: bo qua khoi nay va tiep tuc cai moi schema ben duoi.
IF OBJECT_ID(N'dbo.CoSo',N'U') IS NOT NULL
BEGIN
    IF OBJECT_ID(N'dbo.LichHocLop',N'U') IS NULL
        THROW 51001, N'Database VINABACUS chua co bang dbo.LichHocLop.', 1;

    SET XACT_ABORT ON;
    BEGIN TRY
        BEGIN TRAN;

        IF OBJECT_ID(N'dbo.PhongHoc',N'U') IS NULL
        BEGIN
            CREATE TABLE dbo.PhongHoc (
                MaPhong INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
                MaCoSo INT NOT NULL,
                TenPhong NVARCHAR(100) NOT NULL,
                TrangThai BIT NOT NULL DEFAULT (1),
                CONSTRAINT UQ_PhongHoc_CoSoTen UNIQUE (MaCoSo,TenPhong),
                CONSTRAINT UQ_PhongHoc_PhongCoSo UNIQUE (MaPhong,MaCoSo));
        END;

        IF COL_LENGTH(N'dbo.LichHocLop',N'MaPhong') IS NULL
            ALTER TABLE dbo.LichHocLop ADD MaPhong INT NULL;

        IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys
                       WHERE parent_object_id=OBJECT_ID(N'dbo.PhongHoc')
                         AND name=N'FK_PhongHoc_MaCoSo')
            ALTER TABLE dbo.PhongHoc WITH CHECK
                ADD CONSTRAINT FK_PhongHoc_MaCoSo
                FOREIGN KEY (MaCoSo) REFERENCES dbo.CoSo(MaCoSo);

        IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys
                       WHERE parent_object_id=OBJECT_ID(N'dbo.LichHocLop')
                         AND name=N'FK_LichHocLop_MaPhong')
            ALTER TABLE dbo.LichHocLop WITH CHECK
                ADD CONSTRAINT FK_LichHocLop_MaPhong
                FOREIGN KEY (MaPhong) REFERENCES dbo.PhongHoc(MaPhong);

        DECLARE @CoSoTrungTam INT=(SELECT MaCoSo FROM dbo.CoSo WHERE LoaiCoSo='TRUNG_TAM');
        IF @CoSoTrungTam IS NULL
            THROW 51002, N'Khong tim thay co so TRUNG_TAM trong VINABACUS.', 1;

        INSERT dbo.PhongHoc(MaCoSo,TenPhong)
        SELECT @CoSoTrungTam, v.TenPhong
        FROM (VALUES
            (N'CS1 - Phòng 1'), (N'CS1 - Phòng 10'), (N'CS1 - Phòng 2'),
            (N'CS1 - Phòng 3'), (N'CS1 - Phòng 4'), (N'CS1 - Phòng 5'),
            (N'CS1 - Phòng 7')
        ) AS v(TenPhong)
        WHERE NOT EXISTS (SELECT 1 FROM dbo.PhongHoc p
                          WHERE p.MaCoSo=@CoSoTrungTam AND p.TenPhong=v.TenPhong);

        COMMIT;
        EXEC sys.sp_set_session_context @key=N'VinabacusPhongMigration', @value=1;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK;
        THROW;
    END CATCH;
END
GO
-- Guard: neu da co bang thi BO QUA toan bo cac batch con lai (NOEXEC bat den cuoi file).
IF OBJECT_ID(N'dbo.CoSo',N'U') IS NOT NULL
BEGIN
    IF SESSION_CONTEXT(N'VinabacusPhongMigration') IS NULL
        RAISERROR(N'VINABACUS da co bang. Khong chay bo cai moi.',16,1);
    SET NOEXEC ON;
END
GO

-- ======================= 1. BANG ======================================
CREATE TABLE dbo.CoSo (
    MaCoSo INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    LoaiCoSo VARCHAR(20) NOT NULL CHECK (LoaiCoSo IN ('MAM_NON','TRUNG_TAM')),
    TenCoSo NVARCHAR(200) NOT NULL,
    LogoCoSo NVARCHAR(500) NULL,
    TrangThai BIT NOT NULL DEFAULT (1),
    CONSTRAINT UQ_CoSo_Loai UNIQUE (LoaiCoSo));
CREATE TABLE dbo.CaiDatHeThong (
    MaCaiDat TINYINT NOT NULL PRIMARY KEY CHECK (MaCaiDat=1),
    TenUngDung NVARCHAR(200) NOT NULL,
    LogoUngDung NVARCHAR(500) NULL);
CREATE TABLE dbo.NhanVien (
    MaNhanVien INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    HoTen NVARCHAR(150) NOT NULL,
    NgaySinh DATE NULL,
    GioiTinh VARCHAR(15) NULL CHECK (GioiTinh IN ('NAM','NU','KHAC')),
    SoDienThoai VARCHAR(20) NULL,
    Email VARCHAR(254) COLLATE Latin1_General_100_CI_AI NULL,
    EmailDaXacMinh BIT NOT NULL DEFAULT (0),
    DiaChi NVARCHAR(400) NULL,
    TrangThai BIT NOT NULL DEFAULT (1),
    CONSTRAINT CK_NhanVien_Email CHECK (Email IS NULL OR (CHARINDEX(' ',Email)=0 AND Email LIKE '%_@_%._%')));
CREATE TABLE dbo.NhanVienCoSo (
    MaNhanVienCoSo INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaNhanVien INT NOT NULL, MaCoSo INT NOT NULL,
    TrangThai BIT NOT NULL DEFAULT (1),
    CONSTRAINT UQ_NhanVienCoSo UNIQUE (MaNhanVien,MaCoSo));
CREATE TABLE dbo.TaiKhoan (
    MaTaiKhoan INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaNhanVien INT NOT NULL,
    TenDangNhap VARCHAR(100) COLLATE Latin1_General_100_CI_AI NOT NULL,
    MatKhauBam VARCHAR(255) NOT NULL,
    TrangThai BIT NOT NULL DEFAULT (1),
    PhaiDoiMatKhau BIT NOT NULL DEFAULT (1),
    NgayTao DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT CK_TaiKhoan_TenDangNhap CHECK (LEN(LTRIM(RTRIM(TenDangNhap)))>=3 AND LTRIM(RTRIM(TenDangNhap))=TenDangNhap AND CHARINDEX('@',TenDangNhap)=0),
    CONSTRAINT UQ_TaiKhoan_MaNhanVien UNIQUE (MaNhanVien),
    CONSTRAINT UQ_TaiKhoan_TenDangNhap UNIQUE (TenDangNhap),
    CONSTRAINT UQ_TaiKhoan_TaiKhoanNhanVien UNIQUE (MaTaiKhoan,MaNhanVien));
CREATE TABLE dbo.YeuCauKhoiPhucMatKhau (
    MaYeuCauKhoiPhuc INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaTaiKhoan INT NOT NULL,
    MaYeuCauNgoai UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID(),
    BamOTP VARBINARY(32) NOT NULL,
    NgayTao DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    NgayHetHan DATETIME2(0) NOT NULL,
    SoLanNhapSai TINYINT NOT NULL DEFAULT 0 CHECK (SoLanNhapSai BETWEEN 0 AND 5),
    LanGuiCuoi DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    TrangThai VARCHAR(20) NOT NULL DEFAULT 'CHO_XAC_THUC' CHECK (TrangThai IN ('CHO_XAC_THUC','DA_XAC_MINH','DA_SU_DUNG','HET_HAN','HUY')),
    MaPhienDatLai UNIQUEIDENTIFIER NULL,
    HetHanPhienDatLai DATETIME2(0) NULL,
    NgayXacMinh DATETIME2(0) NULL,
    NgayHoanTat DATETIME2(0) NULL,
    CONSTRAINT UQ_YeuCauKhoiPhuc_Ngoai UNIQUE (MaYeuCauNgoai),
    CONSTRAINT CK_YeuCauKhoiPhuc_ThoiHan CHECK (NgayHetHan>NgayTao),
    CONSTRAINT CK_YeuCauKhoiPhuc_Phien CHECK ((MaPhienDatLai IS NULL AND HetHanPhienDatLai IS NULL) OR (MaPhienDatLai IS NOT NULL AND HetHanPhienDatLai IS NOT NULL)));
CREATE TABLE dbo.VaiTro (
    MaVaiTro INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaVaiTroChu VARCHAR(30) NOT NULL UNIQUE CHECK (MaVaiTroChu IN ('ADMIN','TO_TRUONG','GIAO_VIEN')),
    TenVaiTro NVARCHAR(100) NOT NULL);
CREATE TABLE dbo.TaiKhoanCoSo (
    MaTaiKhoanCoSo INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaTaiKhoan INT NOT NULL, MaNhanVien INT NOT NULL, MaCoSo INT NOT NULL, MaVaiTro INT NOT NULL,
    LaCoSoMacDinh BIT NOT NULL DEFAULT (0),
    TrangThai BIT NOT NULL DEFAULT (1),
    CONSTRAINT UQ_TaiKhoanCoSo UNIQUE (MaTaiKhoan,MaCoSo));
CREATE TABLE dbo.Quyen (
    MaQuyen INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaQuyenChu VARCHAR(70) NOT NULL UNIQUE,
    TenQuyen NVARCHAR(150) NOT NULL);
CREATE TABLE dbo.VaiTroQuyen (
    MaVaiTroQuyen INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaVaiTro INT NOT NULL, MaQuyen INT NOT NULL,
    CONSTRAINT UQ_VaiTroQuyen UNIQUE (MaVaiTro,MaQuyen));
CREATE TABLE dbo.PhanQuyenTaiKhoan (
    MaPhanQuyen INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaTaiKhoan INT NOT NULL, MaCoSo INT NOT NULL, MaQuyen INT NOT NULL, ChoPhep BIT NOT NULL,
    CONSTRAINT UQ_PhanQuyenTaiKhoan UNIQUE (MaTaiKhoan,MaCoSo,MaQuyen));

-- >>> HocVien: MaCoSo la khoa ngoai (co so quan ly ho so). UQ(MaHocVien,MaCoSo) de cac bang con
-- >>> tham chieu tong hop, bao dam hoc vien chi phat sinh du lieu o dung co so cua minh.
CREATE TABLE dbo.HocVien (
    MaHocVien INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaCoSo INT NOT NULL,
    HoTen NVARCHAR(150) NOT NULL,
    NgaySinh DATE NULL,
    GioiTinh VARCHAR(15) NULL CHECK (GioiTinh IN ('NAM','NU','KHAC')),
    TenNguoiLienHe NVARCHAR(150) NULL,
    SoDienThoaiLienHe VARCHAR(20) NULL,
    DiaChi NVARCHAR(400) NULL,
    GhiChu NVARCHAR(500) NULL,
    NgayTao DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    TrangThaiHoSo VARCHAR(20) NOT NULL DEFAULT 'HOAT_DONG' CHECK (TrangThaiHoSo IN ('HOAT_DONG','NGUNG_SU_DUNG')),
    CONSTRAINT UQ_HocVien_HocVienCoSo UNIQUE (MaHocVien,MaCoSo));

CREATE TABLE dbo.MonHoc (
    MaMonHoc INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaCoSo INT NOT NULL,
    TenMonHoc NVARCHAR(150) NOT NULL,
    GhiChu NVARCHAR(500) NULL,
    TrangThai BIT NOT NULL DEFAULT (1),
    CONSTRAINT UQ_MonHoc_CoSoTen UNIQUE (MaCoSo,TenMonHoc),
    CONSTRAINT UQ_MonHoc_MonCoSo UNIQUE (MaMonHoc,MaCoSo));
CREATE TABLE dbo.KhoaHoc (
    MaKhoaHoc INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaCoSo INT NOT NULL, MaMonHoc INT NOT NULL,
    TenKhoaHoc NVARCHAR(200) NOT NULL,
    TongSoBuoi INT NOT NULL CHECK (TongSoBuoi>0),
    HocPhiMacDinh DECIMAL(18,2) NOT NULL CHECK (HocPhiMacDinh>=0),
    TrangThai BIT NOT NULL DEFAULT (1),
    CONSTRAINT UQ_KhoaHoc_CoSo UNIQUE (MaKhoaHoc,MaCoSo),
    CONSTRAINT UQ_KhoaHoc_MonCoSo UNIQUE (MaKhoaHoc,MaMonHoc,MaCoSo));
CREATE TABLE dbo.LopTrungTam (
    MaLop INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaCoSo INT NOT NULL, MaKhoaHoc INT NOT NULL,
    TenLop NVARCHAR(180) NOT NULL,
    MaGiaoVien INT NULL,
    NgayKhaiGiang DATE NULL,
    TrangThai VARCHAR(20) NOT NULL DEFAULT 'DANG_MO' CHECK (TrangThai IN ('DANG_MO','DA_DONG','TAM_DUNG')),
    CONSTRAINT UQ_LopTrungTam_LopKhoaCoSo UNIQUE (MaLop,MaKhoaHoc,MaCoSo));
CREATE TABLE dbo.PhongHoc (
    MaPhong INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaCoSo INT NOT NULL,
    TenPhong NVARCHAR(100) NOT NULL,
    TrangThai BIT NOT NULL DEFAULT (1),
    CONSTRAINT UQ_PhongHoc_CoSoTen UNIQUE (MaCoSo,TenPhong),
    CONSTRAINT UQ_PhongHoc_PhongCoSo UNIQUE (MaPhong,MaCoSo));
CREATE TABLE dbo.LichHocLop (
    MaLichHoc INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaLop INT NOT NULL, MaPhong INT NULL,
    ThuTrongTuan TINYINT NOT NULL CHECK (ThuTrongTuan BETWEEN 2 AND 8),
    GioBatDau TIME(0) NULL, GioKetThuc TIME(0) NULL,
    TuNgay DATE NOT NULL, DenNgay DATE NULL,
    TrangThai VARCHAR(15) NOT NULL DEFAULT 'HIEU_LUC' CHECK (TrangThai IN ('HIEU_LUC','HUY')),
    CONSTRAINT UQ_LichHocLop_LichLop UNIQUE (MaLichHoc,MaLop),
    CHECK (DenNgay IS NULL OR DenNgay>=TuNgay),
    CHECK (GioKetThuc IS NULL OR GioBatDau IS NULL OR GioKetThuc>GioBatDau));
CREATE TABLE dbo.BuoiHoc (
    MaBuoiHoc INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaLop INT NOT NULL, MaLichHoc INT NULL,
    NgayHoc DATE NOT NULL,
    SoCa SMALLINT NOT NULL DEFAULT (1) CHECK (SoCa>=1),
    TrangThai VARCHAR(20) NOT NULL DEFAULT 'DU_KIEN' CHECK (TrangThai IN ('DU_KIEN','DA_DIEN_RA','HUY')),
    GhiChu NVARCHAR(500) NULL,
    CONSTRAINT UQ_BuoiHoc_LopNgayCa UNIQUE (MaLop,NgayHoc,SoCa));
CREATE TABLE dbo.ChuongTrinhGiamGia (
    MaChuongTrinh INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaCoSo INT NOT NULL,
    TenChuongTrinh NVARCHAR(180) NOT NULL,
    PhamViApDung VARCHAR(12) NOT NULL DEFAULT 'KHOA_HOC' CHECK (PhamViApDung IN ('KHOA_HOC','GOI_NAM')),
    LoaiGiam VARCHAR(15) NOT NULL CHECK (LoaiGiam IN ('PHAN_TRAM','SO_TIEN')),
    GiaTriGiam DECIMAL(18,2) NOT NULL CHECK (GiaTriGiam>=0),
    TuNgay DATE NOT NULL, DenNgay DATE NOT NULL,
    TrangThai BIT NOT NULL DEFAULT (1),
    CHECK (DenNgay>=TuNgay),
    CHECK (LoaiGiam<>'PHAN_TRAM' OR GiaTriGiam<=100),
    CONSTRAINT UQ_ChuongTrinhGiamGia_CoSo UNIQUE (MaChuongTrinh,MaCoSo));
CREATE TABLE dbo.GiamGiaKhoaHoc (
    MaGiamGiaKhoa INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaChuongTrinh INT NOT NULL, MaKhoaHoc INT NOT NULL, MaCoSo INT NOT NULL,
    CONSTRAINT UQ_GiamGiaKhoaHoc UNIQUE (MaChuongTrinh,MaKhoaHoc));
CREATE TABLE dbo.DangKyGoiNam (
    MaDangKyGoiNam INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaHocVien INT NOT NULL, MaCoSo INT NOT NULL, MaYeuCau INT NULL,
    TenGoi NVARCHAR(200) NOT NULL,
    NgayDangKy DATE NOT NULL, NgayBatDau DATE NOT NULL, NgayKetThuc DATE NOT NULL,
    TongHocPhiGoc DECIMAL(18,2) NOT NULL CHECK (TongHocPhiGoc>=0),
    MaChuongTrinh INT NULL,
    SoTienGiam DECIMAL(18,2) NOT NULL DEFAULT 0 CHECK (SoTienGiam>=0),
    HocPhiSauGiam DECIMAL(18,2) NOT NULL CHECK (HocPhiSauGiam>=0),
    TrangThai VARCHAR(25) NOT NULL DEFAULT 'DANG_HOC' CHECK (TrangThai IN ('DANG_HOC','HOAN_THANH','NGHI_HAN','HUY')),
    MaTaiKhoanTao INT NOT NULL,
    MaGiaoDichNghiepVu VARCHAR(64) NOT NULL UNIQUE,
    NgayTao DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    GhiChu NVARCHAR(500) NULL,
    CONSTRAINT UQ_DangKyGoiNam_HocVienCoSo UNIQUE (MaDangKyGoiNam,MaHocVien,MaCoSo),
    CONSTRAINT CK_DangKyGoiNam_Tong CHECK (HocPhiSauGiam=TongHocPhiGoc-SoTienGiam AND SoTienGiam<=TongHocPhiGoc),
    CONSTRAINT CK_DangKyGoiNam_Nam CHECK (NgayKetThuc=DATEADD(DAY,-1,DATEADD(YEAR,1,NgayBatDau))));
CREATE TABLE dbo.DangKyKhoaHoc (
    MaDangKy INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaHocVien INT NOT NULL, MaCoSo INT NOT NULL, MaKhoaHoc INT NOT NULL,
    MaDangKyGoiNam INT NULL, SoThuTuTrongGoi INT NULL, MaYeuCau INT NULL,
    NgayDangKy DATE NOT NULL, NgayBatDau DATE NOT NULL, NgayKetThucDuKien DATE NULL,
    TongSoBuoiDangKy INT NOT NULL CHECK (TongSoBuoiDangKy>0),
    HocPhiThoaThuan DECIMAL(18,2) NOT NULL CHECK (HocPhiThoaThuan>=0),
    MaChuongTrinh INT NULL,
    SoTienGiam DECIMAL(18,2) NOT NULL DEFAULT (0) CHECK (SoTienGiam>=0),
    HocPhiSauGiam DECIMAL(18,2) NOT NULL CHECK (HocPhiSauGiam>=0),
    TrangThai VARCHAR(25) NOT NULL DEFAULT 'CHO_XEP_LOP' CHECK (TrangThai IN ('CHO_XEP_LOP','DANG_HOC','HOAN_THANH','DA_CHUYEN','NGHI_HAN','HUY')),
    NgayKetThucThucTe DATE NULL,
    GhiChu NVARCHAR(500) NULL,
    CONSTRAINT UQ_DangKy_KhoaCoSo UNIQUE (MaDangKy,MaKhoaHoc,MaCoSo),
    CONSTRAINT UQ_DangKy_HocVien UNIQUE (MaDangKy,MaHocVien),
    CONSTRAINT UQ_DangKy_HocVienCoSo UNIQUE (MaDangKy,MaHocVien,MaCoSo),
    CHECK (HocPhiSauGiam=HocPhiThoaThuan-SoTienGiam),
    CHECK (SoTienGiam<=HocPhiThoaThuan),
    CONSTRAINT CK_DangKyKhoaHoc_GoiNam CHECK ((MaDangKyGoiNam IS NULL AND SoThuTuTrongGoi IS NULL) OR (MaDangKyGoiNam IS NOT NULL AND SoThuTuTrongGoi>0)),
    CHECK (NgayKetThucDuKien IS NULL OR NgayKetThucDuKien>=NgayBatDau));
CREATE TABLE dbo.XepLopTrungTam (
    MaXepLop INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaDangKy INT NOT NULL, MaKhoaHoc INT NOT NULL, MaCoSo INT NOT NULL, MaLop INT NOT NULL,
    TuNgay DATE NOT NULL, DenNgay DATE NULL,
    TrangThai VARCHAR(20) NOT NULL DEFAULT 'HIEU_LUC' CHECK (TrangThai IN ('HIEU_LUC','KET_THUC','HUY')),
    LyDo NVARCHAR(500) NULL,
    CHECK (DenNgay IS NULL OR DenNgay>=TuNgay),
    CONSTRAINT UQ_XepLopTrungTam_DangKy UNIQUE (MaXepLop,MaDangKy));
CREATE TABLE dbo.SapXepBuoiHoc (
    MaSapXep INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaDangKy INT NOT NULL, MaHocVien INT NOT NULL, MaBuoiHoc INT NOT NULL,
    LoaiSapXep VARCHAR(20) NOT NULL CHECK (LoaiSapXep IN ('CHINH_THUC','DOI_BUOI','HOC_BO_SUNG','HOC_BU')),
    MaSapXepNguon INT NULL,
    TrangThai VARCHAR(20) NOT NULL DEFAULT 'DA_XEP' CHECK (TrangThai IN ('DA_XEP','HOAN_THANH','HUY')),
    NgayTao DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT UQ_SapXepBuoiHoc_NguonDangKy UNIQUE (MaSapXep,MaDangKy),
    CHECK (MaSapXepNguon IS NULL OR MaSapXepNguon<>MaSapXep),
    CHECK (LoaiSapXep NOT IN ('DOI_BUOI','HOC_BU') OR MaSapXepNguon IS NOT NULL));
CREATE TABLE dbo.DiemDanhTrungTam (
    MaDiemDanh INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaSapXep INT NOT NULL UNIQUE,
    TrangThai VARCHAR(25) NOT NULL CHECK (TrangThai IN ('CO_MAT','VANG_CO_PHEP','VANG_KHONG_PHEP','CHUA_DIEM_DANH')),
    GhiChu NVARCHAR(500) NULL,
    MaTaiKhoan INT NOT NULL,
    NgayCapNhat DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME());
CREATE TABLE dbo.ChuyenLopTrungTam (
    MaChuyenLop INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaCoSo INT NOT NULL, MaDangKyCu INT NOT NULL, MaDangKyMoi INT NULL, MaDangKyDich INT NOT NULL,
    MaXepLopCu INT NOT NULL, MaXepLopMoi INT NOT NULL,
    LoaiChuyen VARCHAR(15) NOT NULL CHECK (LoaiChuyen IN ('CUNG_KHOA','KHAC_KHOA')),
    NgayChuyen DATE NOT NULL,
    HocPhiCuSauGiam DECIMAL(18,2) NOT NULL CHECK (HocPhiCuSauGiam>=0),
    TongSoBuoiCu INT NOT NULL CHECK (TongSoBuoiCu>0),
    SoBuoiDaSuDung INT NOT NULL CHECK (SoBuoiDaSuDung>=0),
    GiaTriChuaSuDung DECIMAL(18,2) NOT NULL CHECK (GiaTriChuaSuDung>=0),
    HocPhiMoiApDung DECIMAL(18,2) NOT NULL CHECK (HocPhiMoiApDung>=0),
    NoCuTaiThoiDiemChuyen DECIMAL(18,2) NOT NULL DEFAULT (0) CHECK (NoCuTaiThoiDiemChuyen>=0),
    LyDo NVARCHAR(500) NOT NULL,
    MaTaiKhoan INT NOT NULL,
    MaGiaoDichNghiepVu VARCHAR(64) NOT NULL UNIQUE,
    NgayTao DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    CHECK (SoBuoiDaSuDung<=TongSoBuoiCu),
    CHECK ((LoaiChuyen='CUNG_KHOA' AND MaDangKyMoi IS NULL) OR (LoaiChuyen='KHAC_KHOA' AND MaDangKyMoi IS NOT NULL)),
    CHECK (MaDangKyMoi IS NULL OR MaDangKyMoi<>MaDangKyCu),
    CHECK (MaDangKyDich=ISNULL(MaDangKyMoi,MaDangKyCu)));
CREATE TABLE dbo.NamHoc (
    MaNamHoc INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaCoSo INT NOT NULL,
    TenNamHoc NVARCHAR(50) NOT NULL,
    TuNgay DATE NOT NULL, DenNgay DATE NOT NULL,
    CHECK (DenNgay>=TuNgay),
    CONSTRAINT UQ_NamHoc_CoSoTen UNIQUE (MaCoSo,TenNamHoc),
    CONSTRAINT UQ_NamHoc_MaCoSo UNIQUE (MaNamHoc,MaCoSo));
CREATE TABLE dbo.LopMamNon (
    MaLop INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaNamHoc INT NOT NULL, MaCoSo INT NOT NULL,
    TenLop NVARCHAR(150) NOT NULL,
    TrangThai BIT NOT NULL DEFAULT (1),
    CONSTRAINT UQ_LopMamNon_NamTen UNIQUE (MaNamHoc,TenLop),
    CONSTRAINT UQ_LopMamNon_CoSo UNIQUE (MaLop,MaCoSo));
CREATE TABLE dbo.PhanCongGiaoVienMamNon (
    MaPhanCong INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaLop INT NOT NULL, MaCoSo INT NOT NULL, MaNhanVien INT NOT NULL,
    TuNgay DATE NOT NULL, DenNgay DATE NULL,
    CHECK (DenNgay IS NULL OR DenNgay>=TuNgay));
CREATE TABLE dbo.XepLopMamNon (
    MaXepLop INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaHocVien INT NOT NULL, MaCoSo INT NOT NULL, MaLop INT NOT NULL, MaYeuCau INT NULL,
    TuNgay DATE NOT NULL, DenNgay DATE NULL,
    NgayGhiNhan DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    TrangThai VARCHAR(20) NOT NULL DEFAULT 'HIEU_LUC' CHECK (TrangThai IN ('HIEU_LUC','CHUYEN_LOP','NGHI_HAN','HUY')),
    LyDo NVARCHAR(500) NULL,
    CHECK (DenNgay IS NULL OR DenNgay>=TuNgay),
    CONSTRAINT UQ_XepLopMamNon_HocVienCoSo UNIQUE (MaXepLop,MaHocVien,MaCoSo),
    CONSTRAINT UQ_XepLopMamNon_Lop UNIQUE (MaXepLop,MaLop));
CREATE TABLE dbo.NgayNghiChung (
    MaNgayNghi INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaCoSo INT NOT NULL, NgayNghi DATE NOT NULL, LyDo NVARCHAR(300) NOT NULL,
    CONSTRAINT UQ_NgayNghiChung UNIQUE (MaCoSo,NgayNghi));
CREATE TABLE dbo.ThangMamNon (
    MaThang INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaCoSo INT NOT NULL,
    Nam SMALLINT NOT NULL CHECK (Nam BETWEEN 2000 AND 2100),
    Thang TINYINT NOT NULL CHECK (Thang BETWEEN 1 AND 12),
    SoNgayChuan TINYINT NOT NULL CHECK (SoNgayChuan BETWEEN 0 AND 31),
    LyDoDieuChinhNgay NVARCHAR(500) NULL,
    TrangThai VARCHAR(15) NOT NULL DEFAULT 'MO' CHECK (TrangThai IN ('MO','CHOT')),
    NgayChot DATETIME2(0) NULL, MaNguoiChot INT NULL,
    CONSTRAINT UQ_ThangMamNon_CoSoKy UNIQUE (MaCoSo,Nam,Thang),
    CONSTRAINT UQ_ThangMamNon_MaCoSo UNIQUE (MaThang,MaCoSo));
CREATE TABLE dbo.DiemDanhMamNon (
    MaDiemDanh INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaHocVien INT NOT NULL, MaCoSo INT NOT NULL, MaXepLop INT NOT NULL,
    NgayHoc DATE NOT NULL,
    TrangThai VARCHAR(25) NOT NULL DEFAULT 'CHUA_DIEM_DANH' CHECK (TrangThai IN ('CHUA_DIEM_DANH','CO_MAT','VANG_CO_PHEP','VANG_KHONG_PHEP')),
    GhiChu NVARCHAR(500) NULL,
    MaTaiKhoanDiemDanh INT NOT NULL,
    ThoiDiemCapNhat DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT UQ_DiemDanhMamNon_DiemHocVienCoSo UNIQUE (MaDiemDanh,MaHocVien,MaCoSo),
    CONSTRAINT UQ_DiemDanhMamNon_Ngay UNIQUE (MaHocVien,MaCoSo,NgayHoc));
CREATE TABLE dbo.LichSuSuaDiemDanhMamNon (
    MaLichSu INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaDiemDanh INT NOT NULL,
    TrangThaiTruoc VARCHAR(25) NOT NULL, TrangThaiSau VARCHAR(25) NOT NULL,
    GhiChuTruoc NVARCHAR(500) NULL, GhiChuSau NVARCHAR(500) NULL,
    LyDo NVARCHAR(500) NOT NULL,
    MaTaiKhoan INT NOT NULL,
    NgaySua DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME());
CREATE TABLE dbo.MucHocPhiLop (
    MaMucHocPhi INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaLop INT NOT NULL, ThangBatDau DATE NOT NULL,
    HocPhiThang DECIMAL(18,2) NOT NULL CHECK (HocPhiThang>=0),
    MaTaiKhoanThietLap INT NOT NULL,
    NgayTao DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT UQ_MucHocPhiLop UNIQUE (MaLop,ThangBatDau),
    CHECK (DAY(ThangBatDau)=1));
CREATE TABLE dbo.HocPhiRiengMamNon (
    MaHocPhiRieng INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaHocVien INT NOT NULL, MaLop INT NOT NULL, ThangBatDau DATE NOT NULL,
    HocPhiRieng DECIMAL(18,2) NOT NULL CHECK (HocPhiRieng>=0),
    LyDo NVARCHAR(500) NOT NULL,
    MaTaiKhoanDuyet INT NOT NULL,
    NgayTao DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT UQ_HocPhiRiengMamNon UNIQUE (MaHocVien,MaLop,ThangBatDau),
    CHECK (DAY(ThangBatDau)=1));
CREATE TABLE dbo.DonGiaTienAn (
    MaDonGiaTienAn INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaLop INT NOT NULL, ThangBatDau DATE NOT NULL,
    DonGiaNgay DECIMAL(18,2) NOT NULL CHECK (DonGiaNgay>=0),
    MaTaiKhoanThietLap INT NOT NULL,
    NgayTao DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT UQ_DonGiaTienAn UNIQUE (MaLop,ThangBatDau),
    CONSTRAINT UQ_DonGiaTienAn_MaLop UNIQUE (MaDonGiaTienAn,MaLop),
    CHECK (DAY(ThangBatDau)=1));
CREATE TABLE dbo.BangTinhPhiMamNon (
    MaBangTinh INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaHocVien INT NOT NULL, MaCoSo INT NOT NULL, MaThang INT NOT NULL,
    NgayNhapHocTrongThang DATE NULL, NgayNghiHanTrongThang DATE NULL,
    TongHocPhi DECIMAL(18,2) NOT NULL DEFAULT (0) CHECK (TongHocPhi>=0),
    TongTienAn DECIMAL(18,2) NOT NULL DEFAULT (0) CHECK (TongTienAn>=0),
    TrangThai VARCHAR(15) NOT NULL DEFAULT 'NHAP' CHECK (TrangThai IN ('NHAP','CHOT')),
    NgayTinh DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    NgayChot DATETIME2(0) NULL,
    CONSTRAINT UQ_BangTinhPhiMamNon_HocVienThang UNIQUE (MaHocVien,MaThang),
    CONSTRAINT UQ_BangTinhPhiMamNon_HocVienCoSo UNIQUE (MaBangTinh,MaHocVien,MaCoSo));
CREATE TABLE dbo.ChiTietHocPhiMamNon (
    MaChiTietHocPhi INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaBangTinh INT NOT NULL, MaXepLop INT NOT NULL, MaHocVien INT NOT NULL, MaCoSo INT NOT NULL,
    NgayBatDauPhanBo DATE NOT NULL, NgayKetThucPhanBo DATE NOT NULL,
    SoNgayTinhPhi INT NOT NULL CHECK (SoNgayTinhPhi>=0),
    SoNgayChuan INT NOT NULL CHECK (SoNgayChuan>0),
    MucHocPhiThang DECIMAL(18,2) NOT NULL CHECK (MucHocPhiThang>=0),
    ThanhTien DECIMAL(18,2) NOT NULL CHECK (ThanhTien>=0),
    CHECK (NgayKetThucPhanBo>=NgayBatDauPhanBo));
CREATE TABLE dbo.ChiTietTienAnMamNon (
    MaChiTietTienAn INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaBangTinh INT NOT NULL, MaXepLop INT NOT NULL, MaHocVien INT NOT NULL, MaCoSo INT NOT NULL,
    MaLop INT NOT NULL, MaDonGiaTienAn INT NOT NULL,
    TuNgay DATE NOT NULL, DenNgay DATE NOT NULL,
    SoNgayTinhTienAn INT NOT NULL CHECK (SoNgayTinhTienAn>=0),
    DonGiaApDung DECIMAL(18,2) NOT NULL CHECK (DonGiaApDung>=0),
    ThanhTien DECIMAL(18,2) NOT NULL CHECK (ThanhTien>=0),
    CHECK (DenNgay>=TuNgay));
CREATE TABLE dbo.PhatSinhTienAn (
    MaPhatSinhTienAn INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaHocVien INT NOT NULL, MaCoSo INT NOT NULL,
    LoaiBienDong VARCHAR(25) NOT NULL CHECK (LoaiBienDong IN ('TAO','SU_DUNG','HOAN','HOAN_TAC_TAO','HOAN_TAC_SU_DUNG')),
    SoTien DECIMAL(18,2) NOT NULL CHECK (SoTien>0),
    MaDiemDanh INT NULL, MaBangTinh INT NULL, MaKhoanThuDich INT NULL, MaPhieuHoan INT NULL, MaPhatSinhGoc INT NULL,
    MaGiaoDichNghiepVu VARCHAR(64) NOT NULL UNIQUE,
    NgayPhatSinh DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    MaTaiKhoan INT NOT NULL,
    LyDo NVARCHAR(500) NOT NULL);
CREATE TABLE dbo.KhachTiemNang (
    MaKhach INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaCoSo INT NOT NULL,
    HoTen NVARCHAR(150) NOT NULL,
    SoDienThoai VARCHAR(20) NULL,
    TenNguoiLienHe NVARCHAR(150) NULL,
    NhuCau NVARCHAR(500) NULL,
    TrangThai VARCHAR(30) NOT NULL DEFAULT 'TIEP_NHAN' CHECK (TrangThai IN ('TIEP_NHAN','HOC_THU','CHO_XAC_NHAN','CHO_XEP_LOP','DA_CHUYEN_DOI','KHONG_DANG_KY')),
    DaHocThu BIT NOT NULL DEFAULT (0),
    MaNhanVienPhuTrach INT NULL,
    NgayTiepNhan DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    GhiChu NVARCHAR(500) NULL,
    CONSTRAINT UQ_KhachTiemNang_KhachCoSo UNIQUE (MaKhach,MaCoSo));
CREATE TABLE dbo.LichSuTuVan (
    MaTuVan INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaKhach INT NOT NULL, MaTaiKhoan INT NOT NULL,
    NgayTuVan DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    NoiDung NVARCHAR(1000) NOT NULL,
    KetQua NVARCHAR(500) NULL,
    NgayHenTiep DATE NULL);
CREATE TABLE dbo.YeuCauXepLop (
    MaYeuCau INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaKhach INT NOT NULL, MaHocVien INT NOT NULL, MaCoSo INT NOT NULL, MaKhoaHoc INT NULL,
    NhuCau NVARCHAR(500) NULL,
    TrangThai VARCHAR(25) NOT NULL DEFAULT 'CHO_XEP_LOP' CHECK (TrangThai IN ('CHO_XEP_LOP','HOAN_TAT','HUY')),
    NgayXacNhan DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    NgayHoanTat DATETIME2(0) NULL,
    MaGiaoDichHoanTat VARCHAR(64) NULL,
    CONSTRAINT UQ_YeuCauXepLop_MaCoSo UNIQUE (MaYeuCau,MaCoSo),
    CONSTRAINT UQ_YeuCauXepLop_HocVienCoSo UNIQUE (MaYeuCau,MaHocVien,MaCoSo));
CREATE TABLE dbo.KhoanPhaiThu (
    MaKhoanThu INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaHocVien INT NOT NULL, MaCoSo INT NOT NULL,
    LoaiKhoan VARCHAR(25) NOT NULL CHECK (LoaiKhoan IN ('HOC_PHI_KHOA','HOC_PHI_THANG','TIEN_AN')),
    MaDangKy INT NULL, MaBangTinh INT NULL,
    SoTienGoc DECIMAL(18,2) NOT NULL CHECK (SoTienGoc>=0),
    NgayPhatSinh DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    HanThanhToan DATE NULL,
    TrangThai VARCHAR(15) NOT NULL DEFAULT 'HIEU_LUC' CHECK (TrangThai IN ('HIEU_LUC','HUY')),
    CONSTRAINT UQ_KhoanPhaiThu_HocVienCoSo UNIQUE (MaKhoanThu,MaHocVien,MaCoSo),
    CHECK ((LoaiKhoan='HOC_PHI_KHOA' AND MaDangKy IS NOT NULL AND MaBangTinh IS NULL) OR
           (LoaiKhoan IN ('HOC_PHI_THANG','TIEN_AN') AND MaBangTinh IS NOT NULL AND MaDangKy IS NULL)));
CREATE TABLE dbo.DieuChinhKhoanThu (
    MaDieuChinh INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaKhoanThu INT NOT NULL,
    SoTienDieuChinh DECIMAL(18,2) NOT NULL CHECK (SoTienDieuChinh<>0),
    LoaiDieuChinh VARCHAR(30) NOT NULL CHECK (LoaiDieuChinh IN ('CHUYEN_LOP','CHUYEN_KHOA','SAU_CHOT','KHAC')),
    MaThangNguon INT NULL, MaThangGhiNhan INT NULL,
    LyDo NVARCHAR(500) NOT NULL,
    MaTaiKhoan INT NOT NULL,
    NgayDieuChinh DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    TrangThai VARCHAR(15) NOT NULL DEFAULT 'XAC_NHAN' CHECK (TrangThai IN ('NHAP','XAC_NHAN','HUY')),
    MaGiaoDichNghiepVu VARCHAR(64) NOT NULL UNIQUE);
CREATE TABLE dbo.PhieuThu (
    MaPhieuThu INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    SoPhieuThu VARCHAR(40) NOT NULL UNIQUE,
    MaHocVien INT NOT NULL, MaCoSo INT NOT NULL,
    TongTien DECIMAL(18,2) NOT NULL CHECK (TongTien>0),
    NgayThu DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    HinhThucThanhToan VARCHAR(25) NOT NULL CHECK (HinhThucThanhToan IN ('TIEN_MAT','CHUYEN_KHOAN','KHAC')),
    MaTaiKhoan INT NOT NULL,
    TrangThai VARCHAR(15) NOT NULL DEFAULT 'XAC_NHAN' CHECK (TrangThai IN ('NHAP','XAC_NHAN','HUY')),
    NgayHuy DATETIME2(0) NULL, MaTaiKhoanHuy INT NULL, LyDoHuy NVARCHAR(500) NULL,
    MaGiaoDichNghiepVu VARCHAR(64) NOT NULL UNIQUE,
    GhiChu NVARCHAR(500) NULL,
    CONSTRAINT UQ_PhieuThu_HocVienCoSo UNIQUE (MaPhieuThu,MaHocVien,MaCoSo));
CREATE TABLE dbo.ChiTietPhieuThu (
    MaChiTietPhieuThu INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaPhieuThu INT NOT NULL, MaKhoanThu INT NOT NULL, MaHocVien INT NOT NULL, MaCoSo INT NOT NULL,
    SoTienPhanBo DECIMAL(18,2) NOT NULL CHECK (SoTienPhanBo>0),
    LoaiBienDong VARCHAR(25) NOT NULL CHECK (LoaiBienDong IN ('PHAN_BO','HOAN_TAC_PHAN_BO')),
    MaChiTietNguon INT NULL, MaDieuChinh INT NULL,
    MaGiaoDichNghiepVu VARCHAR(64) NOT NULL UNIQUE,
    NgayGhiNhan DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT UQ_ChiTietPhieuThu_NguonDayDu UNIQUE (MaChiTietPhieuThu,MaPhieuThu,MaKhoanThu,MaHocVien,MaCoSo),
    CONSTRAINT UQ_ChiTietPhieuThu_NguonTinDung UNIQUE (MaChiTietPhieuThu,MaKhoanThu,MaHocVien,MaCoSo),
    CHECK ((LoaiBienDong='PHAN_BO' AND MaChiTietNguon IS NULL) OR (LoaiBienDong='HOAN_TAC_PHAN_BO' AND MaChiTietNguon IS NOT NULL)));
CREATE TABLE dbo.PhatSinhTinDungHocPhi (
    MaPhatSinh INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaHocVien INT NOT NULL, MaCoSo INT NOT NULL,
    LoaiTinDung VARCHAR(20) NOT NULL CHECK (LoaiTinDung IN ('MAM_NON','TRUNG_TAM')),
    LoaiBienDong VARCHAR(25) NOT NULL CHECK (LoaiBienDong IN ('TAO','SU_DUNG','HOAN_TAC_TAO','HOAN_TAC_SU_DUNG')),
    MaKhoanThuNguon INT NULL, MaChiTietHoanTacNguon INT NULL, MaKhoanThuDich INT NULL, MaPhatSinhGoc INT NULL,
    SoTien DECIMAL(18,2) NOT NULL CHECK (SoTien>0),
    MaGiaoDichNghiepVu VARCHAR(64) NOT NULL UNIQUE,
    NgayPhatSinh DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    MaTaiKhoan INT NOT NULL,
    LyDo NVARCHAR(500) NOT NULL,
    CHECK ((LoaiBienDong='TAO' AND MaKhoanThuNguon IS NOT NULL AND MaChiTietHoanTacNguon IS NOT NULL AND MaKhoanThuDich IS NULL)
        OR (LoaiBienDong='SU_DUNG' AND MaKhoanThuDich IS NOT NULL AND MaPhatSinhGoc IS NULL)
        OR (LoaiBienDong='HOAN_TAC_TAO' AND MaPhatSinhGoc IS NOT NULL)
        OR (LoaiBienDong='HOAN_TAC_SU_DUNG' AND MaPhatSinhGoc IS NOT NULL AND MaKhoanThuDich IS NOT NULL)),
    CONSTRAINT UQ_TinDung_MaHocVienCoSo UNIQUE (MaPhatSinh,MaHocVien,MaCoSo));
CREATE TABLE dbo.PhieuHoanTienAn (
    MaPhieuHoan INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaHocVien INT NOT NULL, MaCoSo INT NOT NULL,
    SoTienHoan DECIMAL(18,2) NOT NULL CHECK (SoTienHoan>0),
    NgayHoan DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    LyDo NVARCHAR(500) NOT NULL,
    MaTaiKhoan INT NOT NULL,
    TrangThai VARCHAR(15) NOT NULL DEFAULT 'XAC_NHAN' CHECK (TrangThai IN ('NHAP','XAC_NHAN','HUY')),
    MaGiaoDichNghiepVu VARCHAR(64) NOT NULL UNIQUE,
    CONSTRAINT UQ_PhieuHoanTienAn_HocVienCoSo UNIQUE (MaPhieuHoan,MaHocVien,MaCoSo));
CREATE TABLE dbo.BuTruChuyenLop (
    MaBuTru INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaChuyenLop INT NOT NULL, MaKhoanThuNguon INT NOT NULL, MaKhoanThuDich INT NOT NULL, MaPhatSinhTinDung INT NULL,
    SoTienBuTru DECIMAL(18,2) NOT NULL CHECK (SoTienBuTru>0),
    NgayBuTru DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    TrangThai VARCHAR(15) NOT NULL DEFAULT 'XAC_NHAN' CHECK (TrangThai IN ('NHAP','XAC_NHAN','HUY')),
    MaGiaoDichNghiepVu VARCHAR(64) NOT NULL UNIQUE,
    CHECK (MaKhoanThuNguon<>MaKhoanThuDich));
CREATE TABLE dbo.HangHoa (
    MaHangHoa INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaCoSo INT NOT NULL,
    MaSanPham VARCHAR(40) NOT NULL,
    TenHangHoa NVARCHAR(200) NOT NULL,
    DonViTinh NVARCHAR(40) NOT NULL,
    DonGiaSi DECIMAL(18,2) NOT NULL DEFAULT (0) CHECK (DonGiaSi>=0),
    DonGiaXuatBan DECIMAL(18,2) NOT NULL DEFAULT (0) CHECK (DonGiaXuatBan>=0),
    TrangThai BIT NOT NULL DEFAULT (1),
    CONSTRAINT UQ_HangHoa_CoSoMa UNIQUE (MaCoSo,MaSanPham),
    CONSTRAINT UQ_HangHoa_HangCoSo UNIQUE (MaHangHoa,MaCoSo));
CREATE TABLE dbo.KyKho (
    MaKyKho INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaCoSo INT NOT NULL,
    Nam SMALLINT NOT NULL CHECK (Nam BETWEEN 2000 AND 2100),
    Thang TINYINT NOT NULL CHECK (Thang BETWEEN 1 AND 12),
    TrangThai VARCHAR(15) NOT NULL DEFAULT 'MO' CHECK (TrangThai IN ('MO','CHOT')),
    NgayChot DATETIME2(0) NULL, MaTaiKhoanChot INT NULL,
    CONSTRAINT UQ_KyKho_CoSoThang UNIQUE (MaCoSo,Nam,Thang),
    CONSTRAINT UQ_KyKho_KyCoSo UNIQUE (MaKyKho,MaCoSo));
CREATE TABLE dbo.KiemKeKho (
    MaKiemKe INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaKyKho INT NOT NULL, MaCoSo INT NOT NULL,
    NgayKiemKe DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    MaTaiKhoan INT NOT NULL,
    TrangThai VARCHAR(15) NOT NULL DEFAULT 'NHAP' CHECK (TrangThai IN ('NHAP','XAC_NHAN','HUY')),
    MaGiaoDichNghiepVu VARCHAR(64) NOT NULL UNIQUE,
    GhiChu NVARCHAR(500) NULL,
    CONSTRAINT UQ_KiemKeKho_CoSo UNIQUE (MaKiemKe,MaCoSo));
CREATE TABLE dbo.ChiTietKiemKe (
    MaChiTietKiemKe INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaKiemKe INT NOT NULL, MaCoSo INT NOT NULL, MaHangHoa INT NOT NULL,
    TonHeThong DECIMAL(18,3) NOT NULL CHECK (TonHeThong>=0),
    TonThucTe DECIMAL(18,3) NOT NULL CHECK (TonThucTe>=0),
    CONSTRAINT UQ_ChiTietKiemKe_Hang UNIQUE (MaKiemKe,MaHangHoa));
CREATE TABLE dbo.PhieuKho (
    MaPhieuKho INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaCoSo INT NOT NULL, MaKyKho INT NOT NULL, MaKiemKe INT NULL,
    LoaiPhieu VARCHAR(25) NOT NULL CHECK (LoaiPhieu IN ('TON_DAU','NHAP','XUAT_HS','XUAT_CS','DIEU_CHINH')),
    NgayLap DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    MaTaiKhoan INT NOT NULL,
    TrangThai VARCHAR(15) NOT NULL DEFAULT 'XAC_NHAN' CHECK (TrangThai IN ('NHAP','XAC_NHAN','HUY')),
    MaGiaoDichNghiepVu VARCHAR(64) NOT NULL UNIQUE,
    GhiChu NVARCHAR(500) NULL,
    CONSTRAINT UQ_PhieuKho_CoSo UNIQUE (MaPhieuKho,MaCoSo),
    CHECK ((LoaiPhieu='DIEU_CHINH' AND MaKiemKe IS NOT NULL) OR (LoaiPhieu<>'DIEU_CHINH' AND MaKiemKe IS NULL)));
CREATE TABLE dbo.ChiTietPhieuKho (
    MaChiTiet INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaPhieuKho INT NOT NULL, MaCoSo INT NOT NULL, MaHangHoa INT NOT NULL,
    SoLuong DECIMAL(18,3) NOT NULL CHECK (SoLuong<>0),
    DonGiaSiTaiThoiDiem DECIMAL(18,2) NOT NULL CHECK (DonGiaSiTaiThoiDiem>=0),
    DonGiaXuatBanTaiThoiDiem DECIMAL(18,2) NOT NULL CHECK (DonGiaXuatBanTaiThoiDiem>=0),
    GhiChu NVARCHAR(500) NULL,
    CONSTRAINT UQ_ChiTietPhieuKho_Hang UNIQUE (MaPhieuKho,MaHangHoa));
CREATE TABLE dbo.SoLieuChotKho (
    MaSoLieu INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    MaKyKho INT NOT NULL, MaCoSo INT NOT NULL, MaHangHoa INT NOT NULL,
    MaSanPham VARCHAR(40) NOT NULL, TenHangHoa NVARCHAR(200) NOT NULL, DonViTinh NVARCHAR(40) NOT NULL,
    DonGiaSi DECIMAL(18,2) NOT NULL CHECK (DonGiaSi>=0),
    DonGiaXuatBan DECIMAL(18,2) NOT NULL CHECK (DonGiaXuatBan>=0),
    TonDau DECIMAL(18,3) NOT NULL CHECK (TonDau>=0),
    Nhap DECIMAL(18,3) NOT NULL CHECK (Nhap>=0),
    XuatHS DECIMAL(18,3) NOT NULL CHECK (XuatHS>=0),
    XuatCS DECIMAL(18,3) NOT NULL CHECK (XuatCS>=0),
    DieuChinhKiemKe DECIMAL(18,3) NOT NULL DEFAULT (0),
    TonCuoi DECIMAL(18,3) NOT NULL CHECK (TonCuoi>=0),
    GhiChu NVARCHAR(500) NULL,
    CONSTRAINT UQ_SoLieuChotKho UNIQUE (MaKyKho,MaHangHoa));
GO

-- ======================= 2. KHOA NGOAI (khai bao theo bang du lieu) =====
-- Ten FK tu sinh: FK_<BangCon>_<CotCon>. Bo cac FK trung lap/suy ra duoc:
--  * (MaHocVien,MaCoSo)->HocVien thay cho FK MaHocVien va FK MaCoSo rieng le;
--  * FK tong hop hoan tac thay cho FK don MaChiTietNguon / MaChiTietHoanTacNguon;
--  * FK tong hop MaSapXepNguon,MaDangKy thay cho FK don MaSapXepNguon.
CREATE TABLE #FK (Child SYSNAME, ChildCols NVARCHAR(400), Parent SYSNAME, ParentCols NVARCHAR(400));
INSERT #FK VALUES
-- tai khoan / phan quyen
(N'YeuCauKhoiPhucMatKhau',N'MaTaiKhoan',N'TaiKhoan',N'MaTaiKhoan'),
(N'NhanVienCoSo',N'MaNhanVien',N'NhanVien',N'MaNhanVien'),
(N'NhanVienCoSo',N'MaCoSo',N'CoSo',N'MaCoSo'),
(N'TaiKhoan',N'MaNhanVien',N'NhanVien',N'MaNhanVien'),
(N'TaiKhoanCoSo',N'MaTaiKhoan,MaNhanVien',N'TaiKhoan',N'MaTaiKhoan,MaNhanVien'),
(N'TaiKhoanCoSo',N'MaNhanVien,MaCoSo',N'NhanVienCoSo',N'MaNhanVien,MaCoSo'),
(N'TaiKhoanCoSo',N'MaVaiTro',N'VaiTro',N'MaVaiTro'),
(N'VaiTroQuyen',N'MaVaiTro',N'VaiTro',N'MaVaiTro'),
(N'VaiTroQuyen',N'MaQuyen',N'Quyen',N'MaQuyen'),
(N'PhanQuyenTaiKhoan',N'MaTaiKhoan,MaCoSo',N'TaiKhoanCoSo',N'MaTaiKhoan,MaCoSo'),
(N'PhanQuyenTaiKhoan',N'MaQuyen',N'Quyen',N'MaQuyen'),
-- danh muc dung chung
(N'HocVien',N'MaCoSo',N'CoSo',N'MaCoSo'),
(N'PhongHoc',N'MaCoSo',N'CoSo',N'MaCoSo'),
(N'MonHoc',N'MaCoSo',N'CoSo',N'MaCoSo'),
(N'KhoaHoc',N'MaMonHoc,MaCoSo',N'MonHoc',N'MaMonHoc,MaCoSo'),
(N'LopTrungTam',N'MaKhoaHoc,MaCoSo',N'KhoaHoc',N'MaKhoaHoc,MaCoSo'),
(N'LopTrungTam',N'MaGiaoVien,MaCoSo',N'NhanVienCoSo',N'MaNhanVien,MaCoSo'),
(N'LichHocLop',N'MaLop',N'LopTrungTam',N'MaLop'),
(N'LichHocLop',N'MaPhong',N'PhongHoc',N'MaPhong'),
(N'BuoiHoc',N'MaLop',N'LopTrungTam',N'MaLop'),
(N'BuoiHoc',N'MaLichHoc,MaLop',N'LichHocLop',N'MaLichHoc,MaLop'),
(N'ChuongTrinhGiamGia',N'MaCoSo',N'CoSo',N'MaCoSo'),
(N'GiamGiaKhoaHoc',N'MaChuongTrinh,MaCoSo',N'ChuongTrinhGiamGia',N'MaChuongTrinh,MaCoSo'),
(N'GiamGiaKhoaHoc',N'MaKhoaHoc,MaCoSo',N'KhoaHoc',N'MaKhoaHoc,MaCoSo'),
-- trung tam
(N'DangKyGoiNam',N'MaHocVien,MaCoSo',N'HocVien',N'MaHocVien,MaCoSo'),
(N'DangKyGoiNam',N'MaYeuCau,MaHocVien,MaCoSo',N'YeuCauXepLop',N'MaYeuCau,MaHocVien,MaCoSo'),
(N'DangKyGoiNam',N'MaChuongTrinh,MaCoSo',N'ChuongTrinhGiamGia',N'MaChuongTrinh,MaCoSo'),
(N'DangKyGoiNam',N'MaTaiKhoanTao,MaCoSo',N'TaiKhoanCoSo',N'MaTaiKhoan,MaCoSo'),
(N'DangKyKhoaHoc',N'MaDangKyGoiNam,MaHocVien,MaCoSo',N'DangKyGoiNam',N'MaDangKyGoiNam,MaHocVien,MaCoSo'),
(N'DangKyKhoaHoc',N'MaHocVien,MaCoSo',N'HocVien',N'MaHocVien,MaCoSo'),
(N'DangKyKhoaHoc',N'MaKhoaHoc,MaCoSo',N'KhoaHoc',N'MaKhoaHoc,MaCoSo'),
(N'DangKyKhoaHoc',N'MaYeuCau,MaCoSo',N'YeuCauXepLop',N'MaYeuCau,MaCoSo'),
(N'DangKyKhoaHoc',N'MaChuongTrinh,MaCoSo',N'ChuongTrinhGiamGia',N'MaChuongTrinh,MaCoSo'),
(N'XepLopTrungTam',N'MaDangKy,MaKhoaHoc,MaCoSo',N'DangKyKhoaHoc',N'MaDangKy,MaKhoaHoc,MaCoSo'),
(N'XepLopTrungTam',N'MaLop,MaKhoaHoc,MaCoSo',N'LopTrungTam',N'MaLop,MaKhoaHoc,MaCoSo'),
(N'SapXepBuoiHoc',N'MaDangKy,MaHocVien',N'DangKyKhoaHoc',N'MaDangKy,MaHocVien'),
(N'SapXepBuoiHoc',N'MaBuoiHoc',N'BuoiHoc',N'MaBuoiHoc'),
(N'SapXepBuoiHoc',N'MaSapXepNguon,MaDangKy',N'SapXepBuoiHoc',N'MaSapXep,MaDangKy'),
(N'DiemDanhTrungTam',N'MaSapXep',N'SapXepBuoiHoc',N'MaSapXep'),
(N'DiemDanhTrungTam',N'MaTaiKhoan',N'TaiKhoan',N'MaTaiKhoan'),
(N'ChuyenLopTrungTam',N'MaCoSo',N'CoSo',N'MaCoSo'),
(N'ChuyenLopTrungTam',N'MaDangKyMoi',N'DangKyKhoaHoc',N'MaDangKy'),
(N'ChuyenLopTrungTam',N'MaXepLopCu,MaDangKyCu',N'XepLopTrungTam',N'MaXepLop,MaDangKy'),
(N'ChuyenLopTrungTam',N'MaXepLopMoi,MaDangKyDich',N'XepLopTrungTam',N'MaXepLop,MaDangKy'),
(N'ChuyenLopTrungTam',N'MaTaiKhoan',N'TaiKhoan',N'MaTaiKhoan'),
-- mam non
(N'NamHoc',N'MaCoSo',N'CoSo',N'MaCoSo'),
(N'LopMamNon',N'MaNamHoc,MaCoSo',N'NamHoc',N'MaNamHoc,MaCoSo'),
(N'PhanCongGiaoVienMamNon',N'MaLop,MaCoSo',N'LopMamNon',N'MaLop,MaCoSo'),
(N'PhanCongGiaoVienMamNon',N'MaNhanVien,MaCoSo',N'NhanVienCoSo',N'MaNhanVien,MaCoSo'),
(N'XepLopMamNon',N'MaHocVien,MaCoSo',N'HocVien',N'MaHocVien,MaCoSo'),
(N'XepLopMamNon',N'MaLop,MaCoSo',N'LopMamNon',N'MaLop,MaCoSo'),
(N'XepLopMamNon',N'MaYeuCau,MaCoSo',N'YeuCauXepLop',N'MaYeuCau,MaCoSo'),
(N'NgayNghiChung',N'MaCoSo',N'CoSo',N'MaCoSo'),
(N'ThangMamNon',N'MaCoSo',N'CoSo',N'MaCoSo'),
(N'ThangMamNon',N'MaNguoiChot',N'TaiKhoan',N'MaTaiKhoan'),
(N'DiemDanhMamNon',N'MaXepLop,MaHocVien,MaCoSo',N'XepLopMamNon',N'MaXepLop,MaHocVien,MaCoSo'),
(N'DiemDanhMamNon',N'MaTaiKhoanDiemDanh,MaCoSo',N'TaiKhoanCoSo',N'MaTaiKhoan,MaCoSo'),
(N'LichSuSuaDiemDanhMamNon',N'MaDiemDanh',N'DiemDanhMamNon',N'MaDiemDanh'),
(N'LichSuSuaDiemDanhMamNon',N'MaTaiKhoan',N'TaiKhoan',N'MaTaiKhoan'),
(N'MucHocPhiLop',N'MaLop',N'LopMamNon',N'MaLop'),
(N'MucHocPhiLop',N'MaTaiKhoanThietLap',N'TaiKhoan',N'MaTaiKhoan'),
(N'HocPhiRiengMamNon',N'MaHocVien',N'HocVien',N'MaHocVien'),
(N'HocPhiRiengMamNon',N'MaLop',N'LopMamNon',N'MaLop'),
(N'HocPhiRiengMamNon',N'MaTaiKhoanDuyet',N'TaiKhoan',N'MaTaiKhoan'),
(N'DonGiaTienAn',N'MaLop',N'LopMamNon',N'MaLop'),
(N'DonGiaTienAn',N'MaTaiKhoanThietLap',N'TaiKhoan',N'MaTaiKhoan'),
(N'BangTinhPhiMamNon',N'MaHocVien,MaCoSo',N'HocVien',N'MaHocVien,MaCoSo'),
(N'BangTinhPhiMamNon',N'MaThang,MaCoSo',N'ThangMamNon',N'MaThang,MaCoSo'),
(N'ChiTietHocPhiMamNon',N'MaBangTinh,MaHocVien,MaCoSo',N'BangTinhPhiMamNon',N'MaBangTinh,MaHocVien,MaCoSo'),
(N'ChiTietHocPhiMamNon',N'MaXepLop,MaHocVien,MaCoSo',N'XepLopMamNon',N'MaXepLop,MaHocVien,MaCoSo'),
(N'ChiTietTienAnMamNon',N'MaBangTinh,MaHocVien,MaCoSo',N'BangTinhPhiMamNon',N'MaBangTinh,MaHocVien,MaCoSo'),
(N'ChiTietTienAnMamNon',N'MaXepLop,MaHocVien,MaCoSo',N'XepLopMamNon',N'MaXepLop,MaHocVien,MaCoSo'),
(N'ChiTietTienAnMamNon',N'MaXepLop,MaLop',N'XepLopMamNon',N'MaXepLop,MaLop'),
(N'ChiTietTienAnMamNon',N'MaDonGiaTienAn,MaLop',N'DonGiaTienAn',N'MaDonGiaTienAn,MaLop'),
-- tuyen sinh
(N'KhachTiemNang',N'MaCoSo',N'CoSo',N'MaCoSo'),
(N'KhachTiemNang',N'MaNhanVienPhuTrach,MaCoSo',N'NhanVienCoSo',N'MaNhanVien,MaCoSo'),
(N'LichSuTuVan',N'MaKhach',N'KhachTiemNang',N'MaKhach'),
(N'LichSuTuVan',N'MaTaiKhoan',N'TaiKhoan',N'MaTaiKhoan'),
(N'YeuCauXepLop',N'MaKhach,MaCoSo',N'KhachTiemNang',N'MaKhach,MaCoSo'),
(N'YeuCauXepLop',N'MaHocVien,MaCoSo',N'HocVien',N'MaHocVien,MaCoSo'),
(N'YeuCauXepLop',N'MaKhoaHoc,MaCoSo',N'KhoaHoc',N'MaKhoaHoc,MaCoSo'),
-- tai chinh
(N'KhoanPhaiThu',N'MaHocVien,MaCoSo',N'HocVien',N'MaHocVien,MaCoSo'),
(N'KhoanPhaiThu',N'MaDangKy,MaHocVien,MaCoSo',N'DangKyKhoaHoc',N'MaDangKy,MaHocVien,MaCoSo'),
(N'KhoanPhaiThu',N'MaBangTinh,MaHocVien,MaCoSo',N'BangTinhPhiMamNon',N'MaBangTinh,MaHocVien,MaCoSo'),
(N'DieuChinhKhoanThu',N'MaKhoanThu',N'KhoanPhaiThu',N'MaKhoanThu'),
(N'DieuChinhKhoanThu',N'MaTaiKhoan',N'TaiKhoan',N'MaTaiKhoan'),
(N'DieuChinhKhoanThu',N'MaThangNguon',N'ThangMamNon',N'MaThang'),
(N'DieuChinhKhoanThu',N'MaThangGhiNhan',N'ThangMamNon',N'MaThang'),
(N'PhieuThu',N'MaHocVien,MaCoSo',N'HocVien',N'MaHocVien,MaCoSo'),
(N'PhieuThu',N'MaTaiKhoan,MaCoSo',N'TaiKhoanCoSo',N'MaTaiKhoan,MaCoSo'),
(N'PhieuThu',N'MaTaiKhoanHuy',N'TaiKhoan',N'MaTaiKhoan'),
(N'ChiTietPhieuThu',N'MaPhieuThu,MaHocVien,MaCoSo',N'PhieuThu',N'MaPhieuThu,MaHocVien,MaCoSo'),
(N'ChiTietPhieuThu',N'MaKhoanThu,MaHocVien,MaCoSo',N'KhoanPhaiThu',N'MaKhoanThu,MaHocVien,MaCoSo'),
(N'ChiTietPhieuThu',N'MaChiTietNguon,MaPhieuThu,MaKhoanThu,MaHocVien,MaCoSo',N'ChiTietPhieuThu',N'MaChiTietPhieuThu,MaPhieuThu,MaKhoanThu,MaHocVien,MaCoSo'),
(N'ChiTietPhieuThu',N'MaDieuChinh',N'DieuChinhKhoanThu',N'MaDieuChinh'),
(N'PhatSinhTinDungHocPhi',N'MaHocVien,MaCoSo',N'HocVien',N'MaHocVien,MaCoSo'),
(N'PhatSinhTinDungHocPhi',N'MaKhoanThuNguon,MaHocVien,MaCoSo',N'KhoanPhaiThu',N'MaKhoanThu,MaHocVien,MaCoSo'),
(N'PhatSinhTinDungHocPhi',N'MaKhoanThuDich,MaHocVien,MaCoSo',N'KhoanPhaiThu',N'MaKhoanThu,MaHocVien,MaCoSo'),
(N'PhatSinhTinDungHocPhi',N'MaChiTietHoanTacNguon,MaKhoanThuNguon,MaHocVien,MaCoSo',N'ChiTietPhieuThu',N'MaChiTietPhieuThu,MaKhoanThu,MaHocVien,MaCoSo'),
(N'PhatSinhTinDungHocPhi',N'MaPhatSinhGoc',N'PhatSinhTinDungHocPhi',N'MaPhatSinh'),
(N'PhatSinhTinDungHocPhi',N'MaTaiKhoan',N'TaiKhoan',N'MaTaiKhoan'),
(N'PhieuHoanTienAn',N'MaHocVien,MaCoSo',N'HocVien',N'MaHocVien,MaCoSo'),
(N'PhieuHoanTienAn',N'MaTaiKhoan,MaCoSo',N'TaiKhoanCoSo',N'MaTaiKhoan,MaCoSo'),
(N'BuTruChuyenLop',N'MaChuyenLop',N'ChuyenLopTrungTam',N'MaChuyenLop'),
(N'BuTruChuyenLop',N'MaKhoanThuNguon',N'KhoanPhaiThu',N'MaKhoanThu'),
(N'BuTruChuyenLop',N'MaKhoanThuDich',N'KhoanPhaiThu',N'MaKhoanThu'),
(N'BuTruChuyenLop',N'MaPhatSinhTinDung',N'PhatSinhTinDungHocPhi',N'MaPhatSinh'),
(N'PhatSinhTienAn',N'MaHocVien,MaCoSo',N'HocVien',N'MaHocVien,MaCoSo'),
(N'PhatSinhTienAn',N'MaDiemDanh,MaHocVien,MaCoSo',N'DiemDanhMamNon',N'MaDiemDanh,MaHocVien,MaCoSo'),
(N'PhatSinhTienAn',N'MaBangTinh,MaHocVien,MaCoSo',N'BangTinhPhiMamNon',N'MaBangTinh,MaHocVien,MaCoSo'),
(N'PhatSinhTienAn',N'MaKhoanThuDich,MaHocVien,MaCoSo',N'KhoanPhaiThu',N'MaKhoanThu,MaHocVien,MaCoSo'),
(N'PhatSinhTienAn',N'MaPhieuHoan,MaHocVien,MaCoSo',N'PhieuHoanTienAn',N'MaPhieuHoan,MaHocVien,MaCoSo'),
(N'PhatSinhTienAn',N'MaPhatSinhGoc',N'PhatSinhTienAn',N'MaPhatSinhTienAn'),
(N'PhatSinhTienAn',N'MaTaiKhoan',N'TaiKhoan',N'MaTaiKhoan'),
-- kho
(N'HangHoa',N'MaCoSo',N'CoSo',N'MaCoSo'),
(N'KyKho',N'MaCoSo',N'CoSo',N'MaCoSo'),
(N'KyKho',N'MaTaiKhoanChot',N'TaiKhoan',N'MaTaiKhoan'),
(N'KiemKeKho',N'MaKyKho,MaCoSo',N'KyKho',N'MaKyKho,MaCoSo'),
(N'KiemKeKho',N'MaTaiKhoan,MaCoSo',N'TaiKhoanCoSo',N'MaTaiKhoan,MaCoSo'),
(N'ChiTietKiemKe',N'MaKiemKe,MaCoSo',N'KiemKeKho',N'MaKiemKe,MaCoSo'),
(N'ChiTietKiemKe',N'MaHangHoa,MaCoSo',N'HangHoa',N'MaHangHoa,MaCoSo'),
(N'PhieuKho',N'MaKyKho,MaCoSo',N'KyKho',N'MaKyKho,MaCoSo'),
(N'PhieuKho',N'MaKiemKe,MaCoSo',N'KiemKeKho',N'MaKiemKe,MaCoSo'),
(N'PhieuKho',N'MaTaiKhoan,MaCoSo',N'TaiKhoanCoSo',N'MaTaiKhoan,MaCoSo'),
(N'ChiTietPhieuKho',N'MaPhieuKho,MaCoSo',N'PhieuKho',N'MaPhieuKho,MaCoSo'),
(N'ChiTietPhieuKho',N'MaHangHoa,MaCoSo',N'HangHoa',N'MaHangHoa,MaCoSo'),
(N'SoLieuChotKho',N'MaKyKho,MaCoSo',N'KyKho',N'MaKyKho,MaCoSo'),
(N'SoLieuChotKho',N'MaHangHoa,MaCoSo',N'HangHoa',N'MaHangHoa,MaCoSo');

DECLARE @c SYSNAME,@cc NVARCHAR(400),@p SYSNAME,@pc NVARCHAR(400),@sql NVARCHAR(MAX),@n INT=0;
DECLARE cur CURSOR LOCAL FAST_FORWARD FOR SELECT Child,ChildCols,Parent,ParentCols FROM #FK;
OPEN cur; FETCH NEXT FROM cur INTO @c,@cc,@p,@pc;
WHILE @@FETCH_STATUS=0
BEGIN
    SET @sql=N'ALTER TABLE dbo.'+QUOTENAME(@c)+N' WITH CHECK ADD CONSTRAINT '
        +QUOTENAME(LEFT(N'FK_'+@c+N'_'+REPLACE(@cc,N',',N'_'),128))
        +N' FOREIGN KEY ('+@cc+N') REFERENCES dbo.'+QUOTENAME(@p)+N'('+@pc+N');';
    BEGIN TRY EXEC sys.sp_executesql @sql; SET @n+=1; END TRY
    BEGIN CATCH PRINT N'LOI FK: '+@sql+N' -> '+ERROR_MESSAGE(); THROW; END CATCH
    FETCH NEXT FROM cur INTO @c,@cc,@p,@pc;
END
CLOSE cur; DEALLOCATE cur;
PRINT CONCAT(N'Da tao ',@n,N' khoa ngoai.');
GO
-- So khoa ngoai du kien luu lai de buoc kiem tra cuoi file doi chieu.
DECLARE @soFK INT=(SELECT COUNT(*) FROM #FK);
EXEC sys.sp_addextendedproperty @name=N'SoKhoaNgoaiDuKien',@value=@soFK;
DROP TABLE #FK;
GO

-- ======================= 3. CHI MUC ====================================
-- Hoc vien: tra cuu theo co so + trang thai + ten (covering cho danh sach/cache).
CREATE INDEX IX_HocVien_CoSo_Ten ON dbo.HocVien(MaCoSo,TrangThaiHoSo,HoTen) INCLUDE(NgaySinh,GioiTinh,SoDienThoaiLienHe);
CREATE UNIQUE INDEX UX_NhanVien_Email ON dbo.NhanVien(Email) WHERE Email IS NOT NULL;
CREATE UNIQUE INDEX UX_TaiKhoanCoSo_MacDinh ON dbo.TaiKhoanCoSo(MaTaiKhoan) WHERE LaCoSoMacDinh=1 AND TrangThai=1;
CREATE INDEX IX_YeuCauKhoiPhuc_TaiKhoan ON dbo.YeuCauKhoiPhucMatKhau(MaTaiKhoan,TrangThai,NgayHetHan);
CREATE UNIQUE INDEX UX_YeuCauKhoiPhuc_Phien ON dbo.YeuCauKhoiPhucMatKhau(MaPhienDatLai) WHERE MaPhienDatLai IS NOT NULL;
CREATE UNIQUE INDEX UX_DangKyGoiNam_YeuCau ON dbo.DangKyGoiNam(MaYeuCau) WHERE MaYeuCau IS NOT NULL;
CREATE INDEX IX_DangKyGoiNam_HocVien ON dbo.DangKyGoiNam(MaCoSo,MaHocVien,TrangThai,NgayDangKy);
CREATE UNIQUE INDEX UX_DangKyKhoaHoc_GoiNamThuTu ON dbo.DangKyKhoaHoc(MaDangKyGoiNam,SoThuTuTrongGoi) WHERE MaDangKyGoiNam IS NOT NULL;
CREATE UNIQUE INDEX UX_DangKyKhoaHoc_YeuCau ON dbo.DangKyKhoaHoc(MaYeuCau) WHERE MaYeuCau IS NOT NULL;
CREATE INDEX IX_DangKyKhoaHoc_CoSoHocVien ON dbo.DangKyKhoaHoc(MaCoSo,MaHocVien,TrangThai) INCLUDE(NgayBatDau,NgayKetThucDuKien);
CREATE UNIQUE INDEX UX_XepLopMamNon_YeuCau ON dbo.XepLopMamNon(MaYeuCau) WHERE MaYeuCau IS NOT NULL;
CREATE UNIQUE INDEX UX_YeuCauXepLop_GiaoDich ON dbo.YeuCauXepLop(MaGiaoDichHoanTat) WHERE MaGiaoDichHoanTat IS NOT NULL;
CREATE UNIQUE INDEX UX_XepLopTrungTam_DangHieuLuc ON dbo.XepLopTrungTam(MaDangKy) WHERE TrangThai='HIEU_LUC' AND DenNgay IS NULL;
CREATE UNIQUE INDEX UX_XepLopMamNon_HocVienHieuLuc ON dbo.XepLopMamNon(MaHocVien,MaCoSo) WHERE TrangThai='HIEU_LUC' AND DenNgay IS NULL;
CREATE INDEX IX_XepLopMamNon_CoSoLop ON dbo.XepLopMamNon(MaCoSo,MaLop,TrangThai,TuNgay,DenNgay) INCLUDE(MaHocVien);
CREATE UNIQUE INDEX UX_SapXepBuoiHoc_HocVienBuoi ON dbo.SapXepBuoiHoc(MaHocVien,MaBuoiHoc) WHERE TrangThai IN ('DA_XEP','HOAN_THANH');
CREATE UNIQUE INDEX UX_SapXepBuoiHoc_HocBuNguon ON dbo.SapXepBuoiHoc(MaDangKy,MaSapXepNguon) WHERE LoaiSapXep='HOC_BU' AND TrangThai='HOAN_THANH';
CREATE INDEX IX_SapXepBuoiHoc_DangKy ON dbo.SapXepBuoiHoc(MaDangKy,LoaiSapXep,TrangThai);
CREATE INDEX IX_SapXepBuoiHoc_Buoi ON dbo.SapXepBuoiHoc(MaBuoiHoc,TrangThai) INCLUDE(MaDangKy,MaHocVien);
CREATE INDEX IX_BuoiHoc_LopNgay ON dbo.BuoiHoc(MaLop,NgayHoc) INCLUDE(TrangThai);
CREATE UNIQUE INDEX UX_KhoanPhaiThu_HocPhiKhoa ON dbo.KhoanPhaiThu(MaDangKy) WHERE LoaiKhoan='HOC_PHI_KHOA' AND MaDangKy IS NOT NULL;
CREATE UNIQUE INDEX UX_KhoanPhaiThu_BangLoai ON dbo.KhoanPhaiThu(MaBangTinh,LoaiKhoan) WHERE MaBangTinh IS NOT NULL;
CREATE INDEX IX_KhoanPhaiThu_HocVienCoSo ON dbo.KhoanPhaiThu(MaHocVien,MaCoSo,LoaiKhoan);
CREATE INDEX IX_ChiTietPhieuThu_Khoan ON dbo.ChiTietPhieuThu(MaKhoanThu,LoaiBienDong);
CREATE INDEX IX_ChiTietPhieuThu_Nguon ON dbo.ChiTietPhieuThu(MaChiTietNguon) WHERE MaChiTietNguon IS NOT NULL;
CREATE INDEX IX_PhieuThu_HocVienCoSoNgay ON dbo.PhieuThu(MaHocVien,MaCoSo,NgayThu);
CREATE UNIQUE INDEX UX_TinDung_HoanTacNguon ON dbo.PhatSinhTinDungHocPhi(MaChiTietHoanTacNguon) WHERE LoaiBienDong='TAO' AND MaChiTietHoanTacNguon IS NOT NULL;
CREATE INDEX IX_TinDung_SoDu ON dbo.PhatSinhTinDungHocPhi(MaHocVien,MaCoSo,LoaiTinDung,LoaiBienDong) INCLUDE(SoTien);
CREATE INDEX IX_TienAn_SoDu ON dbo.PhatSinhTienAn(MaHocVien,MaCoSo,LoaiBienDong) INCLUDE(SoTien);
CREATE UNIQUE INDEX UX_TienAn_DiemDanh ON dbo.PhatSinhTienAn(MaDiemDanh) WHERE LoaiBienDong='TAO' AND MaDiemDanh IS NOT NULL;
CREATE INDEX IX_DiemDanhMamNon_CoSoNgay ON dbo.DiemDanhMamNon(MaCoSo,NgayHoc);
CREATE INDEX IX_BangTinhPhiMamNon_Thang ON dbo.BangTinhPhiMamNon(MaCoSo,MaThang,TrangThai) INCLUDE(MaHocVien,TongHocPhi,TongTienAn);
CREATE INDEX IX_ChuyenLopTrungTam_DangKyCu ON dbo.ChuyenLopTrungTam(MaDangKyCu,NgayChuyen);
CREATE UNIQUE INDEX UX_PhieuKho_KiemKe ON dbo.PhieuKho(MaKiemKe) WHERE LoaiPhieu='DIEU_CHINH' AND MaKiemKe IS NOT NULL;
CREATE INDEX IX_PhieuKho_KyTrangThai ON dbo.PhieuKho(MaKyKho,TrangThai,LoaiPhieu) INCLUDE(MaCoSo);
CREATE INDEX IX_ChiTietPhieuKho_Hang ON dbo.ChiTietPhieuKho(MaHangHoa,MaCoSo) INCLUDE(SoLuong);
GO

-- ======================= 4. DANH MUC KHOI TAO ==========================
INSERT dbo.CoSo(LoaiCoSo,TenCoSo) VALUES ('MAM_NON',N'Cơ sở Mầm non'),('TRUNG_TAM',N'Cơ sở Trung tâm');
INSERT dbo.CaiDatHeThong(MaCaiDat,TenUngDung) VALUES (1,N'VINABACUS');
INSERT dbo.VaiTro(MaVaiTroChu,TenVaiTro) VALUES ('ADMIN',N'Admin'),('TO_TRUONG',N'Tổ trưởng'),('GIAO_VIEN',N'Giáo viên');
INSERT dbo.Quyen(MaQuyenChu,TenQuyen) VALUES
('XEM_HOC_VIEN',N'Xem hồ sơ học viên'),('QUAN_LY_HOC_VIEN',N'Quản lý hồ sơ học viên'),
('XEM_BAO_CAO',N'Xem thống kê cơ sở'),('DIEM_DANH',N'Điểm danh học viên thuộc lớp được phân công'),
('SUA_DIEM_DANH_DA_CHOT',N'Sửa điểm danh Mầm non sau chốt'),('XEP_LOP',N'Xếp lớp chính thức'),
('CHUYEN_LOP',N'Chuyển lớp hoặc khóa'),('THU_HOC_PHI',N'Xác nhận phiếu thu'),
('DIEU_CHINH_HOC_PHI',N'Điều chỉnh học phí và tín dụng'),('XEM_CONG_NO',N'Xem công nợ học viên'),
('TUYEN_SINH',N'Tiếp nhận khách tiềm năng'),('TAO_TAI_KHOAN',N'Tạo tài khoản trong cơ sở được phân quyền'),
('PHAN_QUYEN',N'Cấp quyền trong phạm vi được cho phép'),('THAY_DOI_VAI_TRO',N'Chỉ Admin được nâng hoặc hạ vai trò tại cơ sở'),
('CAI_DAT_HE_THONG',N'Thay đổi tên ứng dụng và logo'),('KHO_XEM',N'Xem tồn kho tại cơ sở'),
('KHO_NHAP',N'Nhập kho và tồn đầu'),('KHO_XUAT',N'Xuất học viên và xuất cơ sở'),
('KHO_KIEM_KE',N'Kiểm kê và chốt kho'),('KHO_HANG_HOA',N'Thêm sửa hàng hóa'),
('KHO_XUAT_EXCEL',N'Xuất tồn kho ra Excel'),('HOAN_TIEN_AN',N'Hoàn tiền ăn dư cho học viên'),
('CHOT_THANG_MAM_NON',N'Chốt kỳ phí Mầm non');
-- Admin: tat ca; To truong: tru 4 quyen doc quyen Admin; Giao vien: xem/diem danh/kho xuat.
INSERT dbo.VaiTroQuyen(MaVaiTro,MaQuyen)
SELECT v.MaVaiTro,q.MaQuyen FROM dbo.VaiTro v CROSS JOIN dbo.Quyen q
WHERE v.MaVaiTroChu='ADMIN'
   OR (v.MaVaiTroChu='TO_TRUONG' AND q.MaQuyenChu NOT IN ('SUA_DIEM_DANH_DA_CHOT','HOAN_TIEN_AN','CAI_DAT_HE_THONG','THAY_DOI_VAI_TRO'))
   OR (v.MaVaiTroChu='GIAO_VIEN' AND q.MaQuyenChu IN ('XEM_HOC_VIEN','XEM_BAO_CAO','DIEM_DANH','KHO_XEM','KHO_XUAT'));
GO

-- ======================= 5. DU LIEU MAU (CHI MOI TRUONG THU NGHIEM) ====
-- Xoa ca khoi nay khi cai that. Hoc vien mau gan co so qua MaCoSo.
SET XACT_ABORT ON;
BEGIN TRY
  BEGIN TRAN;
  DECLARE @TT INT=(SELECT MaCoSo FROM dbo.CoSo WHERE LoaiCoSo='TRUNG_TAM');
  DECLARE @MN INT=(SELECT MaCoSo FROM dbo.CoSo WHERE LoaiCoSo='MAM_NON');

  INSERT dbo.PhongHoc(MaCoSo,TenPhong) VALUES
   (@TT,N'CS1 - Phòng 1'),(@TT,N'CS1 - Phòng 10'),(@TT,N'CS1 - Phòng 2'),
   (@TT,N'CS1 - Phòng 3'),(@TT,N'CS1 - Phòng 4'),(@TT,N'CS1 - Phòng 5'),
   (@TT,N'CS1 - Phòng 7');

  INSERT dbo.HocVien(MaCoSo,HoTen,NgaySinh,GioiTinh,TenNguoiLienHe,SoDienThoaiLienHe,DiaChi,GhiChu) VALUES
   (@TT,N'[MẪU] Nguyễn Minh Anh','20170618','NU',N'[MẪU] Phụ huynh Anh','0900000001',N'Ninh Kiều, Cần Thơ',N'Dữ liệu minh họa'),
   (@MN,N'[MẪU] Trần Hải Nam','20190826','NAM',N'[MẪU] Phụ huynh Nam','0900000002',N'Cái Răng, Cần Thơ',N'Dữ liệu minh họa'),
   (@TT,N'Nguyễn Minh Anh','20190512','NU',N'Nguyễn Văn Hùng','0912345678',N'Ninh Kiều, Cần Thơ',N'Học viên mẫu thứ nhất'),
   (@MN,N'Trần Gia Bảo','20180920','NAM',N'Trần Thị Lan','0987654321',N'Cái Răng, Cần Thơ',N'Học viên mẫu thứ hai');

  INSERT dbo.MonHoc(MaCoSo,TenMonHoc) VALUES (@TT,N'[MẪU] Toán tư duy'),(@TT,N'[MẪU] Tiếng Anh');
  DECLARE @MonToan INT=(SELECT MaMonHoc FROM dbo.MonHoc WHERE MaCoSo=@TT AND TenMonHoc=N'[MẪU] Toán tư duy');
  DECLARE @MonAnh  INT=(SELECT MaMonHoc FROM dbo.MonHoc WHERE MaCoSo=@TT AND TenMonHoc=N'[MẪU] Tiếng Anh');
  INSERT dbo.KhoaHoc(MaCoSo,MaMonHoc,TenKhoaHoc,TongSoBuoi,HocPhiMacDinh) VALUES
   (@TT,@MonToan,N'[MẪU] Kid 1',32,2000000),(@TT,@MonToan,N'[MẪU] Kid 2',32,3000000),(@TT,@MonAnh,N'[MẪU] Cambridge 1A',36,3600000);
  DECLARE @Kid1 INT=(SELECT MaKhoaHoc FROM dbo.KhoaHoc WHERE MaCoSo=@TT AND TenKhoaHoc=N'[MẪU] Kid 1');
  INSERT dbo.LopTrungTam(MaCoSo,MaKhoaHoc,TenLop,NgayKhaiGiang) VALUES (@TT,@Kid1,N'[MẪU] Lớp Toán A','20261110');
  DECLARE @Lop INT=SCOPE_IDENTITY();
  DECLARE @P1 INT=(SELECT MaPhong FROM dbo.PhongHoc WHERE MaCoSo=@TT AND TenPhong=N'CS1 - Phòng 1');
  DECLARE @P2 INT=(SELECT MaPhong FROM dbo.PhongHoc WHERE MaCoSo=@TT AND TenPhong=N'CS1 - Phòng 2');
  INSERT dbo.LichHocLop(MaLop,MaPhong,ThuTrongTuan,GioBatDau,GioKetThuc,TuNgay) VALUES
   (@Lop,@P1,3,'17:30','19:00','20261110'),(@Lop,@P2,5,'17:30','19:00','20261110'),
   (@Lop,@P1,7,'08:00','09:30','20261110');

  INSERT dbo.ChuongTrinhGiamGia(MaCoSo,TenChuongTrinh,PhamViApDung,LoaiGiam,GiaTriGiam,TuNgay,DenNgay) VALUES
   (@TT,N'[MẪU] Ưu đãi học 1 năm giảm 15%','GOI_NAM','PHAN_TRAM',15,'20260101','20281231'),
   (@TT,N'[MẪU] Giảm 200.000đ khóa Kid 1','KHOA_HOC','SO_TIEN',200000,'20260101','20281231');
  INSERT dbo.GiamGiaKhoaHoc(MaChuongTrinh,MaKhoaHoc,MaCoSo)
  SELECT MaChuongTrinh,@Kid1,@TT FROM dbo.ChuongTrinhGiamGia WHERE MaCoSo=@TT AND PhamViApDung='KHOA_HOC';

  INSERT dbo.NamHoc(MaCoSo,TenNamHoc,TuNgay,DenNgay) VALUES (@MN,N'[MẪU] 2026–2027','20260801','20270731');
  DECLARE @NamHoc INT=SCOPE_IDENTITY();
  INSERT dbo.LopMamNon(MaNamHoc,MaCoSo,TenLop) VALUES (@NamHoc,@MN,N'[MẪU] Lớp Mầm'),(@NamHoc,@MN,N'[MẪU] Lớp Chồi'),(@NamHoc,@MN,N'[MẪU] Lớp Lá');

  INSERT dbo.HangHoa(MaCoSo,MaSanPham,TenHangHoa,DonViTinh,DonGiaSi,DonGiaXuatBan) VALUES
   (@MN,'DEMO-MN-01',N'[MẪU] Vở tập tô',N'Quyển',9000,12000),(@TT,'DEMO-TT-01',N'[MẪU] Bút chì',N'Cây',3000,5000);
  COMMIT;
END TRY
BEGIN CATCH
  IF XACT_STATE()<>0 ROLLBACK;
  THROW;
END CATCH;
GO

-- ======================= 6. KIEM TRA CAU TRUC ==========================
IF (SELECT COUNT(*) FROM sys.tables WHERE schema_id=SCHEMA_ID(N'dbo'))<>58
    THROW 55001,N'So bang khac 58.',1;
IF EXISTS(SELECT 1 FROM sys.foreign_keys WHERE is_not_trusted=1 OR is_disabled=1)
    THROW 55002,N'Co khoa ngoai bi tat hoac chua xac thuc.',1;
DECLARE @du INT=CONVERT(INT,(SELECT value FROM sys.extended_properties WHERE class=0 AND name=N'SoKhoaNgoaiDuKien'));
IF (SELECT COUNT(*) FROM sys.foreign_keys WHERE schema_id=SCHEMA_ID(N'dbo'))<>@du
    THROW 55003,N'So khoa ngoai khong khop so du kien.',1;
IF COL_LENGTH(N'dbo.HocVien',N'MaCoSo') IS NULL
    THROW 55004,N'HocVien thieu cot MaCoSo.',1;
IF NOT EXISTS(SELECT 1 FROM sys.foreign_keys WHERE parent_object_id=OBJECT_ID(N'dbo.HocVien') AND referenced_object_id=OBJECT_ID(N'dbo.CoSo'))
    THROW 55005,N'HocVien chua co khoa ngoai toi CoSo.',1;
EXEC sys.sp_dropextendedproperty @name=N'SoKhoaNgoaiDuKien';
PRINT N'PASS: kiem tra cau truc co ban (khong thay the kiem thu kich ban).';
SELECT DB_NAME() AS CoSoDuLieu,
       (SELECT COUNT(*) FROM sys.tables WHERE schema_id=SCHEMA_ID(N'dbo')) AS SoBang,
       (SELECT COUNT(*) FROM sys.foreign_keys) AS SoKhoaNgoai,
       (SELECT COUNT(*) FROM dbo.HocVien) AS SoHocVien;
GO
SET NOEXEC OFF;   -- ket thuc guard
GO
-- Neu batch dau vua cap nhat database dang co, bo qua phan du lieu demo.
IF TRY_CONVERT(INT, SESSION_CONTEXT(N'VinabacusPhongMigration'))=1
BEGIN
    EXEC sys.sp_set_session_context @key=N'VinabacusPhongMigration', @value=NULL;
    PRINT N'OK: Da cap nhat PhongHoc va LichHocLop trong VINABACUS.';
    SET NOEXEC ON;
END
GO

-- ==================== BO SUNG DU LIEU MAU ====================
/* ===========================================================================
 VINABACUS — BO DU LIEU MAU HOAN CHINH (Trung tam + Mam non)
 Dung cho file SQL VINABACUS goc do nguoi dung tai len 16/11/2026

 HUONG DAN:
  1. Chi chay khi da cai THANH CONG file SQL goc vao database VINABACUS.
  2. Mo trong SSMS va Execute. Co the chay lai neu khong sua/xoa rieng cac mau.
  3. Chay 03_KIEM_TRA_DU_LIEU.sql de xem bao cao va rang buoc.

 AN TOAN:
  - Chi INSERT co dieu kien NOT EXISTS; KHONG DROP/DELETE/UPDATE bang goc.
  - Giao dich chung, XACT_ABORT ON: loi thi rollback.
  - Du lieu test duoc danh dau [TEST] / example.invalid / so dien thoai gia.
  - KHONG xay dung dang nhap hay phan quyen. Hai tai khoan ki thuat bat buoc
    cho FK MaTaiKhoan, bam ngau nhien vo hieu cho viec dang nhap. KHONG la auth.
  - CHI CHAY TREN MOI TRUONG TEST. KHONG chay tren du lieu san xuat.

 DO: Doi chieu ten cot va khoa ngoai tu file SQL goc, them bang PhongHoc.
 CHUA: Khong co SQL Server truc tiep trong moi truong tao script de chay E2E.
=========================================================================== */
USE [VINABACUS];
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;

    IF OBJECT_ID(N'dbo.HocVien',N'U') IS NULL
       OR OBJECT_ID(N'dbo.TaiKhoanCoSo',N'U') IS NULL
       OR OBJECT_ID(N'dbo.ChiTietTienAnMamNon',N'U') IS NULL
       OR OBJECT_ID(N'dbo.ChiTietPhieuKho',N'U') IS NULL
       OR OBJECT_ID(N'dbo.PhongHoc',N'U') IS NULL
       OR COL_LENGTH(N'dbo.LichHocLop',N'MaPhong') IS NULL
        THROW 57001, N'Chua cai day du cau truc 58 bang VINABACUS. Hay cai SQL goc truoc.', 1;

    DECLARE @TT INT=(SELECT MaCoSo FROM dbo.CoSo WHERE LoaiCoSo='TRUNG_TAM');
    DECLARE @MN INT=(SELECT MaCoSo FROM dbo.CoSo WHERE LoaiCoSo='MAM_NON');
    DECLARE @P1 INT=(SELECT MaPhong FROM dbo.PhongHoc WHERE MaCoSo=@TT AND TenPhong=N'CS1 - Phòng 1');
    DECLARE @P2 INT=(SELECT MaPhong FROM dbo.PhongHoc WHERE MaCoSo=@TT AND TenPhong=N'CS1 - Phòng 2');
    DECLARE @VaiTroDemo INT=(SELECT MaVaiTro FROM dbo.VaiTro WHERE MaVaiTroChu='GIAO_VIEN');
    DECLARE @Tag NVARCHAR(100)=N'[TEST VINABACUS 2026]';
    IF @TT IS NULL OR @MN IS NULL OR @VaiTroDemo IS NULL
        THROW 57002, N'Thieu CoSo TRUNG_TAM, MAM_NON hoac VaiTro GIAO_VIEN tu SQL goc.', 1;
    IF @P1 IS NULL OR @P2 IS NULL
        THROW 57028, N'Thieu phong mau. Hay chay migration PhongHoc truoc.', 1;

    /* --------------------------------------------------------------------
       A. NHAN VIEN + LIEN KET CO SO + 2 TAI KHOAN KY THUAT
       (khong tao giao dien dang nhap, khong thay doi phan quyen)
    -------------------------------------------------------------------- */
    INSERT dbo.NhanVien(HoTen,Email,GioiTinh)
    SELECT v.HoTen,v.Email,v.GioiTinh
    FROM (VALUES
       (N'[TEST] Người ghi nhận Trung tâm','seed.tt.operator@example.invalid','NAM'),
       (N'[TEST] Giáo viên Toán A','seed.tt.gv01@example.invalid','NU'),
       (N'[TEST] Giáo viên Anh văn B','seed.tt.gv02@example.invalid','NU'),
       (N'[TEST] Người ghi nhận Mầm non','seed.mn.operator@example.invalid','NU'),
       (N'[TEST] Cô giáo Mầm','seed.mn.gv01@example.invalid','NU'),
       (N'[TEST] Cô giáo Lá','seed.mn.gv02@example.invalid','NU')
    ) v(HoTen,Email,GioiTinh)
    WHERE NOT EXISTS (SELECT 1 FROM dbo.NhanVien nv WHERE nv.Email=v.Email);

    INSERT dbo.NhanVienCoSo(MaNhanVien,MaCoSo)
    SELECT nv.MaNhanVien, cs.MaCoSo
    FROM (VALUES
       ('seed.tt.operator@example.invalid','TRUNG_TAM'),
       ('seed.tt.gv01@example.invalid','TRUNG_TAM'),
       ('seed.tt.gv02@example.invalid','TRUNG_TAM'),
       ('seed.mn.operator@example.invalid','MAM_NON'),
       ('seed.mn.gv01@example.invalid','MAM_NON'),
       ('seed.mn.gv02@example.invalid','MAM_NON')
    ) v(Email,LoaiCoSo)
    JOIN dbo.NhanVien nv ON nv.Email=v.Email
    JOIN dbo.CoSo cs ON cs.LoaiCoSo=v.LoaiCoSo
    WHERE NOT EXISTS (SELECT 1 FROM dbo.NhanVienCoSo csnv
                      WHERE csnv.MaNhanVien=nv.MaNhanVien AND csnv.MaCoSo=cs.MaCoSo);

    -- MatKhauBam la gia tri ngau nhien, khong co mat khau dang nhap tuong ung.
    -- Chi tao de thoa rang buoc FK MaTaiKhoan cua phieu thu / diem danh / kho.
    INSERT dbo.TaiKhoan(MaNhanVien,TenDangNhap,MatKhauBam,PhaiDoiMatKhau)
    SELECT nv.MaNhanVien,v.TenDangNhap,
           CONVERT(VARCHAR(64),HASHBYTES('SHA2_256',CONVERT(VARCHAR(36),NEWID())),2),1
    FROM (VALUES
       ('seed.tt.operator@example.invalid','vinabacus_test_tt'),
       ('seed.mn.operator@example.invalid','vinabacus_test_mn')
    ) v(Email,TenDangNhap)
    JOIN dbo.NhanVien nv ON nv.Email=v.Email
    WHERE NOT EXISTS (SELECT 1 FROM dbo.TaiKhoan tk WHERE tk.TenDangNhap=v.TenDangNhap)
      AND NOT EXISTS (SELECT 1 FROM dbo.TaiKhoan tk WHERE tk.MaNhanVien=nv.MaNhanVien);

    DECLARE @TkTT INT=(SELECT tk.MaTaiKhoan FROM dbo.TaiKhoan tk JOIN dbo.NhanVien nv ON nv.MaNhanVien=tk.MaNhanVien
                        WHERE nv.Email='seed.tt.operator@example.invalid' AND tk.TenDangNhap='vinabacus_test_tt');
    DECLARE @TkMN INT=(SELECT tk.MaTaiKhoan FROM dbo.TaiKhoan tk JOIN dbo.NhanVien nv ON nv.MaNhanVien=tk.MaNhanVien
                        WHERE nv.Email='seed.mn.operator@example.invalid' AND tk.TenDangNhap='vinabacus_test_mn');
    IF @TkTT IS NULL OR @TkMN IS NULL
        THROW 57003, N'Tai khoan ky thuat da bi trung ten hoac gan sai nhan vien. Khong ghi du lieu.', 1;

    INSERT dbo.TaiKhoanCoSo(MaTaiKhoan,MaNhanVien,MaCoSo,MaVaiTro,LaCoSoMacDinh)
    SELECT tk.MaTaiKhoan,tk.MaNhanVien,cs.MaCoSo,@VaiTroDemo,1
    FROM (VALUES ('vinabacus_test_tt','TRUNG_TAM'),('vinabacus_test_mn','MAM_NON')) v(TenDangNhap,LoaiCoSo)
    JOIN dbo.TaiKhoan tk ON tk.TenDangNhap=v.TenDangNhap
    JOIN dbo.CoSo cs ON cs.LoaiCoSo=v.LoaiCoSo
    JOIN dbo.NhanVienCoSo nvc ON nvc.MaNhanVien=tk.MaNhanVien AND nvc.MaCoSo=cs.MaCoSo
    WHERE NOT EXISTS (SELECT 1 FROM dbo.TaiKhoanCoSo tcs WHERE tcs.MaTaiKhoan=tk.MaTaiKhoan AND tcs.MaCoSo=cs.MaCoSo);

    IF NOT EXISTS (SELECT 1 FROM dbo.TaiKhoanCoSo WHERE MaTaiKhoan=@TkTT AND MaCoSo=@TT)
       OR NOT EXISTS (SELECT 1 FROM dbo.TaiKhoanCoSo WHERE MaTaiKhoan=@TkMN AND MaCoSo=@MN)
        THROW 57004, N'Thieu lien ket tai khoan ki thuat va co so.', 1;

    DECLARE @GvTT1 INT=(SELECT MaNhanVien FROM dbo.NhanVien WHERE Email='seed.tt.gv01@example.invalid');
    DECLARE @GvTT2 INT=(SELECT MaNhanVien FROM dbo.NhanVien WHERE Email='seed.tt.gv02@example.invalid');
    DECLARE @GvMN1 INT=(SELECT MaNhanVien FROM dbo.NhanVien WHERE Email='seed.mn.gv01@example.invalid');
    DECLARE @GvMN2 INT=(SELECT MaNhanVien FROM dbo.NhanVien WHERE Email='seed.mn.gv02@example.invalid');

    /* --------------------------------------------------------------------
       B. HOC VIEN HAI CO SO (6 + 6, ngoai 4 ban ghi trong file goc)
    -------------------------------------------------------------------- */
    INSERT dbo.HocVien(MaCoSo,HoTen,NgaySinh,GioiTinh,TenNguoiLienHe,SoDienThoaiLienHe,DiaChi,GhiChu)
    SELECT cs.MaCoSo,v.HoTen,CONVERT(DATE,v.NgaySinh,112),v.GioiTinh,v.PhuHuynh,v.SoDT,N'Cần Thơ',@Tag
    FROM (VALUES
       ('TRUNG_TAM',N'[TEST] Nguyễn Gia Huy','20170312','NAM',N'[TEST] Phụ huynh Gia Huy','0000000101'),
       ('TRUNG_TAM',N'[TEST] Lê Minh Khôi','20160625','NAM',N'[TEST] Phụ huynh Minh Khôi','0000000102'),
       ('TRUNG_TAM',N'[TEST] Phạm Hải Yến','20180118','NU',N'[TEST] Phụ huynh Hải Yến','0000000103'),
       ('TRUNG_TAM',N'[TEST] Trần Nhật Minh','20150921','NAM',N'[TEST] Phụ huynh Nhật Minh','0000000104'),
       ('TRUNG_TAM',N'[TEST] Võ Bảo Ngọc','20170330','NU',N'[TEST] Phụ huynh Bảo Ngọc','0000000105'),
       ('TRUNG_TAM',N'[TEST] Hồ Thảo Vy','20160813','NU',N'[TEST] Phụ huynh Thảo Vy','0000000106'),
       ('MAM_NON',N'[TEST] Trịnh Minh Nhật','20220612','NAM',N'[TEST] Phụ huynh Minh Nhật','0000000201'),
       ('MAM_NON',N'[TEST] Phan Thiên An','20220507','NU',N'[TEST] Phụ huynh Thiên An','0000000202'),
       ('MAM_NON',N'[TEST] Lê Ngọc Hân','20210819','NU',N'[TEST] Phụ huynh Ngọc Hân','0000000203'),
       ('MAM_NON',N'[TEST] Nguyễn Đức Nam','20210326','NAM',N'[TEST] Phụ huynh Đức Nam','0000000204'),
       ('MAM_NON',N'[TEST] Trần Diệu Anh','20200602','NU',N'[TEST] Phụ huynh Diệu Anh','0000000205'),
       ('MAM_NON',N'[TEST] Phạm Minh Phúc','20200911','NAM',N'[TEST] Phụ huynh Minh Phúc','0000000206')
    ) v(LoaiCoSo,HoTen,NgaySinh,GioiTinh,PhuHuynh,SoDT)
    JOIN dbo.CoSo cs ON cs.LoaiCoSo=v.LoaiCoSo
    WHERE NOT EXISTS (SELECT 1 FROM dbo.HocVien hv
                      WHERE hv.MaCoSo=cs.MaCoSo AND hv.SoDienThoaiLienHe=v.SoDT AND hv.GhiChu=@Tag);

    /* --------------------------------------------------------------------
       C. TUYEN SINH HAI CO SO + LICH SU TU VAN + YEU CAU XEP LOP
    -------------------------------------------------------------------- */
    INSERT dbo.KhachTiemNang(MaCoSo,HoTen,SoDienThoai,TenNguoiLienHe,NhuCau,TrangThai)
    SELECT cs.MaCoSo,v.HoTen,v.SoDT,N'[TEST] Phụ huynh',v.NhuCau,v.TrangThai
    FROM (VALUES
       ('TRUNG_TAM',N'[TEST] Tiềm năng Toán A','0000000301',N'[TEST] Đăng ký Toán tư duy','CHO_XEP_LOP'),
       ('TRUNG_TAM',N'[TEST] Tiềm năng Anh văn B','0000000302',N'[TEST] Tìm hiểu Cambridge','TIEP_NHAN'),
       ('MAM_NON',N'[TEST] Tiềm năng lớp Mầm','0000000401',N'[TEST] Đăng ký Mầm non','CHO_XEP_LOP'),
       ('MAM_NON',N'[TEST] Tiềm năng lớp Lá','0000000402',N'[TEST] Tư vấn năm học','TIEP_NHAN')
    ) v(LoaiCoSo,HoTen,SoDT,NhuCau,TrangThai)
    JOIN dbo.CoSo cs ON cs.LoaiCoSo=v.LoaiCoSo
    WHERE NOT EXISTS (SELECT 1 FROM dbo.KhachTiemNang k WHERE k.MaCoSo=cs.MaCoSo AND k.SoDienThoai=v.SoDT);

    INSERT dbo.LichSuTuVan(MaKhach,MaTaiKhoan,NoiDung,KetQua,NgayHenTiep)
    SELECT k.MaKhach,CASE WHEN k.MaCoSo=@TT THEN @TkTT ELSE @TkMN END,
           @Tag+N' Cuộc gọi tư vấn thử nghiệm',N'Đã ghi nhận nhu cầu','20261123'
    FROM dbo.KhachTiemNang k
    WHERE k.SoDienThoai IN ('0000000301','0000000401')
      AND k.MaCoSo IN (@TT,@MN)
      AND NOT EXISTS (SELECT 1 FROM dbo.LichSuTuVan tv WHERE tv.MaKhach=k.MaKhach AND tv.NoiDung=@Tag+N' Cuộc gọi tư vấn thử nghiệm');

    DECLARE @Kid1 INT=(SELECT TOP(1) MaKhoaHoc FROM dbo.KhoaHoc WHERE MaCoSo=@TT AND TenKhoaHoc=N'[MẪU] Kid 1');
    DECLARE @Cambridge INT=(SELECT TOP(1) MaKhoaHoc FROM dbo.KhoaHoc WHERE MaCoSo=@TT AND TenKhoaHoc=N'[MẪU] Cambridge 1A');
    IF @Kid1 IS NULL OR @Cambridge IS NULL
        THROW 57005, N'Thieu khoa hoc Kid 1 hoac Cambridge 1A cua file SQL goc.', 1;

    INSERT dbo.YeuCauXepLop(MaKhach,MaHocVien,MaCoSo,MaKhoaHoc,NhuCau,TrangThai)
    SELECT k.MaKhach,hv.MaHocVien,k.MaCoSo,
           CASE WHEN k.MaCoSo=@TT THEN @Kid1 ELSE NULL END,
           @Tag+N' Yêu cầu xếp lớp', 'CHO_XEP_LOP'
    FROM (VALUES
       ('0000000301','0000000101','TRUNG_TAM'),
       ('0000000401','0000000201','MAM_NON')
    ) v(SoDTKhach,SoDTHocVien,LoaiCoSo)
    JOIN dbo.CoSo cs ON cs.LoaiCoSo=v.LoaiCoSo
    JOIN dbo.KhachTiemNang k ON k.SoDienThoai=v.SoDTKhach AND k.MaCoSo=cs.MaCoSo
    JOIN dbo.HocVien hv ON hv.SoDienThoaiLienHe=v.SoDTHocVien AND hv.MaCoSo=cs.MaCoSo AND hv.GhiChu=@Tag
    WHERE NOT EXISTS (SELECT 1 FROM dbo.YeuCauXepLop y
                      WHERE y.MaKhach=k.MaKhach AND y.MaHocVien=hv.MaHocVien AND y.MaCoSo=cs.MaCoSo);

    /* --------------------------------------------------------------------
       D. TRUNG TAM: LOP, LICH, BUOI, DANG KY, XEP LOP, DIEM DANH
    -------------------------------------------------------------------- */
    INSERT dbo.LopTrungTam(MaCoSo,MaKhoaHoc,TenLop,MaGiaoVien,NgayKhaiGiang)
    SELECT @TT,v.MaKhoaHoc,v.TenLop,v.MaGiaoVien,CONVERT(DATE,v.NgayKG,112)
    FROM (VALUES
       (@Kid1,N'[TEST] Lớp Toán tư duy B',@GvTT1,'20261103'),
       (@Cambridge,N'[TEST] Lớp Tiếng Anh B',@GvTT2,'20261102')
    ) v(MaKhoaHoc,TenLop,MaGiaoVien,NgayKG)
    WHERE NOT EXISTS (SELECT 1 FROM dbo.LopTrungTam l WHERE l.MaCoSo=@TT AND l.TenLop=v.TenLop);

    INSERT dbo.LichHocLop(MaLop,MaPhong,ThuTrongTuan,GioBatDau,GioKetThuc,TuNgay)
    SELECT l.MaLop,v.MaPhong,v.Thu,CONVERT(TIME(0),v.BatDau),CONVERT(TIME(0),v.KetThuc),CONVERT(DATE,v.TuNgay,112)
    FROM (VALUES
       (@P1,N'[TEST] Lớp Toán tư duy B',3,'17:30','19:00','20261103'),
       (@P2,N'[TEST] Lớp Toán tư duy B',5,'17:30','19:00','20261103'),
       (@P1,N'[TEST] Lớp Tiếng Anh B',4,'17:00','18:30','20261102'),
       (@P2,N'[TEST] Lớp Tiếng Anh B',7,'08:00','09:30','20261102')
    ) v(MaPhong,TenLop,Thu,BatDau,KetThuc,TuNgay)
    JOIN dbo.LopTrungTam l ON l.MaCoSo=@TT AND l.TenLop=v.TenLop
    WHERE NOT EXISTS (SELECT 1 FROM dbo.LichHocLop lich
                      WHERE lich.MaLop=l.MaLop AND lich.ThuTrongTuan=v.Thu
                        AND lich.TuNgay=CONVERT(DATE,v.TuNgay,112));

    INSERT dbo.BuoiHoc(MaLop,MaLichHoc,NgayHoc,SoCa,TrangThai,GhiChu)
    SELECT l.MaLop,lich.MaLichHoc,CONVERT(DATE,v.NgayHoc,112),1,'DA_DIEN_RA',@Tag
    FROM (VALUES
       (N'[TEST] Lớp Toán tư duy B','20261110',3),
       (N'[TEST] Lớp Toán tư duy B','20261112',5),
       (N'[TEST] Lớp Tiếng Anh B','20261111',4),
       (N'[TEST] Lớp Tiếng Anh B','20261114',7)
    ) v(TenLop,NgayHoc,Thu)
    JOIN dbo.LopTrungTam l ON l.MaCoSo=@TT AND l.TenLop=v.TenLop
    JOIN dbo.LichHocLop lich ON lich.MaLop=l.MaLop AND lich.ThuTrongTuan=v.Thu
    WHERE NOT EXISTS (SELECT 1 FROM dbo.BuoiHoc bh
                      WHERE bh.MaLop=l.MaLop AND bh.NgayHoc=CONVERT(DATE,v.NgayHoc,112) AND bh.SoCa=1);

    INSERT dbo.DangKyKhoaHoc(MaHocVien,MaCoSo,MaKhoaHoc,NgayDangKy,NgayBatDau,NgayKetThucDuKien,
                             TongSoBuoiDangKy,HocPhiThoaThuan,SoTienGiam,HocPhiSauGiam,TrangThai,GhiChu)
    SELECT hv.MaHocVien,@TT,l.MaKhoaHoc,'20261101','20261103','20270331',
           kh.TongSoBuoi,kh.HocPhiMacDinh,0,kh.HocPhiMacDinh,'DANG_HOC',@Tag
    FROM (VALUES
       ('0000000101',N'[TEST] Lớp Toán tư duy B'),
       ('0000000102',N'[TEST] Lớp Toán tư duy B'),
       ('0000000103',N'[TEST] Lớp Toán tư duy B'),
       ('0000000104',N'[TEST] Lớp Tiếng Anh B'),
       ('0000000105',N'[TEST] Lớp Tiếng Anh B')
    ) v(SoDT,TenLop)
    JOIN dbo.HocVien hv ON hv.MaCoSo=@TT AND hv.SoDienThoaiLienHe=v.SoDT AND hv.GhiChu=@Tag
    JOIN dbo.LopTrungTam l ON l.MaCoSo=@TT AND l.TenLop=v.TenLop
    JOIN dbo.KhoaHoc kh ON kh.MaKhoaHoc=l.MaKhoaHoc AND kh.MaCoSo=@TT
    WHERE NOT EXISTS (SELECT 1 FROM dbo.DangKyKhoaHoc dk
                      WHERE dk.MaHocVien=hv.MaHocVien AND dk.MaKhoaHoc=l.MaKhoaHoc AND dk.GhiChu=@Tag);

    INSERT dbo.XepLopTrungTam(MaDangKy,MaKhoaHoc,MaCoSo,MaLop,TuNgay,TrangThai,LyDo)
    SELECT dk.MaDangKy,l.MaKhoaHoc,@TT,l.MaLop,'20261103','HIEU_LUC',@Tag
    FROM (VALUES
       ('0000000101',N'[TEST] Lớp Toán tư duy B'),
       ('0000000102',N'[TEST] Lớp Toán tư duy B'),
       ('0000000103',N'[TEST] Lớp Toán tư duy B'),
       ('0000000104',N'[TEST] Lớp Tiếng Anh B'),
       ('0000000105',N'[TEST] Lớp Tiếng Anh B')
    ) v(SoDT,TenLop)
    JOIN dbo.HocVien hv ON hv.MaCoSo=@TT AND hv.SoDienThoaiLienHe=v.SoDT AND hv.GhiChu=@Tag
    JOIN dbo.LopTrungTam l ON l.MaCoSo=@TT AND l.TenLop=v.TenLop
    JOIN dbo.DangKyKhoaHoc dk ON dk.MaHocVien=hv.MaHocVien AND dk.MaKhoaHoc=l.MaKhoaHoc AND dk.MaCoSo=@TT AND dk.GhiChu=@Tag
    WHERE NOT EXISTS (SELECT 1 FROM dbo.XepLopTrungTam xl WHERE xl.MaDangKy=dk.MaDangKy AND xl.MaLop=l.MaLop);

    INSERT dbo.SapXepBuoiHoc(MaDangKy,MaHocVien,MaBuoiHoc,LoaiSapXep,TrangThai)
    SELECT dk.MaDangKy,hv.MaHocVien,bh.MaBuoiHoc,'CHINH_THUC','HOAN_THANH'
    FROM (VALUES
       ('0000000101',N'[TEST] Lớp Toán tư duy B'),
       ('0000000102',N'[TEST] Lớp Toán tư duy B'),
       ('0000000103',N'[TEST] Lớp Toán tư duy B'),
       ('0000000104',N'[TEST] Lớp Tiếng Anh B'),
       ('0000000105',N'[TEST] Lớp Tiếng Anh B')
    ) v(SoDT,TenLop)
    JOIN dbo.HocVien hv ON hv.MaCoSo=@TT AND hv.SoDienThoaiLienHe=v.SoDT AND hv.GhiChu=@Tag
    JOIN dbo.LopTrungTam l ON l.MaCoSo=@TT AND l.TenLop=v.TenLop
    JOIN dbo.DangKyKhoaHoc dk ON dk.MaHocVien=hv.MaHocVien AND dk.MaKhoaHoc=l.MaKhoaHoc AND dk.MaCoSo=@TT AND dk.GhiChu=@Tag
    JOIN dbo.BuoiHoc bh ON bh.MaLop=l.MaLop AND bh.GhiChu=@Tag
    WHERE NOT EXISTS (SELECT 1 FROM dbo.SapXepBuoiHoc sx
                      WHERE sx.MaDangKy=dk.MaDangKy AND sx.MaBuoiHoc=bh.MaBuoiHoc AND sx.LoaiSapXep='CHINH_THUC');

    INSERT dbo.DiemDanhTrungTam(MaSapXep,TrangThai,MaTaiKhoan,GhiChu)
    SELECT sx.MaSapXep,
           CASE WHEN hv.SoDienThoaiLienHe='0000000102' AND bh.NgayHoc='20261112'
                THEN 'VANG_CO_PHEP' ELSE 'CO_MAT' END,
           @TkTT,@Tag
    FROM dbo.SapXepBuoiHoc sx
    JOIN dbo.DangKyKhoaHoc dk ON dk.MaDangKy=sx.MaDangKy AND dk.GhiChu=@Tag AND dk.MaCoSo=@TT
    JOIN dbo.HocVien hv ON hv.MaHocVien=dk.MaHocVien AND hv.MaCoSo=@TT
    JOIN dbo.BuoiHoc bh ON bh.MaBuoiHoc=sx.MaBuoiHoc AND bh.GhiChu=@Tag
    WHERE NOT EXISTS (SELECT 1 FROM dbo.DiemDanhTrungTam dd WHERE dd.MaSapXep=sx.MaSapXep);

    /* --------------------------------------------------------------------
       E. MAM NON: XEP LOP, PHAN CONG, DIEM DANH, MUC PHI, TIEN AN
    -------------------------------------------------------------------- */
    DECLARE @NamHoc INT=(SELECT TOP(1) MaNamHoc FROM dbo.NamHoc
                         WHERE MaCoSo=@MN AND TuNgay<='20261101' AND DenNgay>='20261130' ORDER BY MaNamHoc);
    IF @NamHoc IS NULL
        THROW 57006, N'Thieu NamHoc Mam non bao phu thang 11/2026.', 1;
    IF NOT EXISTS (SELECT 1 FROM dbo.LopMamNon WHERE MaNamHoc=@NamHoc AND TenLop=N'[MẪU] Lớp Mầm')
       OR NOT EXISTS (SELECT 1 FROM dbo.LopMamNon WHERE MaNamHoc=@NamHoc AND TenLop=N'[MẪU] Lớp Chồi')
       OR NOT EXISTS (SELECT 1 FROM dbo.LopMamNon WHERE MaNamHoc=@NamHoc AND TenLop=N'[MẪU] Lớp Lá')
        THROW 57007, N'Thieu lop Mam/Choi/La mau trong nam hoc 2026-2027.', 1;

    INSERT dbo.PhanCongGiaoVienMamNon(MaLop,MaCoSo,MaNhanVien,TuNgay)
    SELECT l.MaLop,@MN,v.MaGV,'20260801'
    FROM (VALUES
       (N'[MẪU] Lớp Mầm',@GvMN1),
       (N'[MẪU] Lớp Chồi',@GvMN1),
       (N'[MẪU] Lớp Lá',@GvMN2)
    ) v(TenLop,MaGV)
    JOIN dbo.LopMamNon l ON l.MaNamHoc=@NamHoc AND l.MaCoSo=@MN AND l.TenLop=v.TenLop
    WHERE NOT EXISTS (SELECT 1 FROM dbo.PhanCongGiaoVienMamNon pc
                      WHERE pc.MaLop=l.MaLop AND pc.MaNhanVien=v.MaGV AND pc.MaCoSo=@MN AND pc.TuNgay='20260801');

    INSERT dbo.XepLopMamNon(MaHocVien,MaCoSo,MaLop,TuNgay,TrangThai,LyDo)
    SELECT hv.MaHocVien,@MN,l.MaLop,'20261001','HIEU_LUC',@Tag
    FROM (VALUES
       ('0000000201',N'[MẪU] Lớp Mầm'),('0000000202',N'[MẪU] Lớp Mầm'),
       ('0000000203',N'[MẪU] Lớp Chồi'),('0000000204',N'[MẪU] Lớp Chồi'),
       ('0000000205',N'[MẪU] Lớp Lá'),('0000000206',N'[MẪU] Lớp Lá')
    ) v(SoDT,TenLop)
    JOIN dbo.HocVien hv ON hv.MaCoSo=@MN AND hv.SoDienThoaiLienHe=v.SoDT AND hv.GhiChu=@Tag
    JOIN dbo.LopMamNon l ON l.MaNamHoc=@NamHoc AND l.MaCoSo=@MN AND l.TenLop=v.TenLop
    WHERE NOT EXISTS (SELECT 1 FROM dbo.XepLopMamNon xl
                      WHERE xl.MaHocVien=hv.MaHocVien AND xl.MaCoSo=@MN AND xl.MaLop=l.MaLop AND xl.TuNgay='20261001');

    INSERT dbo.ThangMamNon(MaCoSo,Nam,Thang,SoNgayChuan,TrangThai)
    SELECT @MN,2026,11,21,'MO'
    WHERE NOT EXISTS (SELECT 1 FROM dbo.ThangMamNon t WHERE t.MaCoSo=@MN AND t.Nam=2026 AND t.Thang=11);
    DECLARE @ThangMN INT=(SELECT MaThang FROM dbo.ThangMamNon WHERE MaCoSo=@MN AND Nam=2026 AND Thang=11);

    INSERT dbo.DiemDanhMamNon(MaHocVien,MaCoSo,MaXepLop,NgayHoc,TrangThai,MaTaiKhoanDiemDanh,GhiChu)
    SELECT hv.MaHocVien,@MN,xl.MaXepLop,CONVERT(DATE,v.NgayHoc,112),
           CASE WHEN hv.SoDienThoaiLienHe='0000000202' AND v.NgayHoc='20261112'
                THEN 'VANG_CO_PHEP' ELSE 'CO_MAT' END,@TkMN,@Tag
    FROM (VALUES ('20261110'),('20261112')) v(NgayHoc)
    CROSS JOIN dbo.HocVien hv
    JOIN dbo.XepLopMamNon xl ON xl.MaHocVien=hv.MaHocVien AND xl.MaCoSo=@MN AND xl.LyDo=@Tag
    WHERE hv.MaCoSo=@MN AND hv.GhiChu=@Tag
      AND hv.SoDienThoaiLienHe IN ('0000000201','0000000202','0000000203','0000000204','0000000205','0000000206')
      AND NOT EXISTS (SELECT 1 FROM dbo.DiemDanhMamNon d
                      WHERE d.MaHocVien=hv.MaHocVien AND d.MaCoSo=@MN AND d.NgayHoc=CONVERT(DATE,v.NgayHoc,112));

    INSERT dbo.MucHocPhiLop(MaLop,ThangBatDau,HocPhiThang,MaTaiKhoanThietLap)
    SELECT l.MaLop,'20261101',v.HocPhi,@TkMN
    FROM (VALUES
       (N'[MẪU] Lớp Mầm',CAST(1500000 AS DECIMAL(18,2))),
       (N'[MẪU] Lớp Chồi',CAST(1750000 AS DECIMAL(18,2))),
       (N'[MẪU] Lớp Lá',CAST(1950000 AS DECIMAL(18,2)))
    ) v(TenLop,HocPhi)
    JOIN dbo.LopMamNon l ON l.MaNamHoc=@NamHoc AND l.MaCoSo=@MN AND l.TenLop=v.TenLop
    WHERE NOT EXISTS (SELECT 1 FROM dbo.MucHocPhiLop p WHERE p.MaLop=l.MaLop AND p.ThangBatDau='20261101');

    INSERT dbo.DonGiaTienAn(MaLop,ThangBatDau,DonGiaNgay,MaTaiKhoanThietLap)
    SELECT l.MaLop,'20261101',v.DonGia,@TkMN
    FROM (VALUES
       (N'[MẪU] Lớp Mầm',CAST(35000 AS DECIMAL(18,2))),
       (N'[MẪU] Lớp Chồi',CAST(40000 AS DECIMAL(18,2))),
       (N'[MẪU] Lớp Lá',CAST(45000 AS DECIMAL(18,2)))
    ) v(TenLop,DonGia)
    JOIN dbo.LopMamNon l ON l.MaNamHoc=@NamHoc AND l.MaCoSo=@MN AND l.TenLop=v.TenLop
    WHERE NOT EXISTS (SELECT 1 FROM dbo.DonGiaTienAn gia WHERE gia.MaLop=l.MaLop AND gia.ThangBatDau='20261101');

    -- Bang tinh phi THU NGHIEM o trang thai NHAP.
    -- Hoc phi thang du; tien an tinh theo 2 ngay diem danh MAU (khong phai ca thang).
    INSERT dbo.BangTinhPhiMamNon(MaHocVien,MaCoSo,MaThang,TongHocPhi,TongTienAn,TrangThai)
    SELECT hv.MaHocVien,@MN,@ThangMN,phi.HocPhiThang,
           CAST(gia.DonGiaNgay * (
                 SELECT COUNT(*) FROM dbo.DiemDanhMamNon dd
                 WHERE dd.MaHocVien=hv.MaHocVien AND dd.MaCoSo=@MN
                   AND dd.NgayHoc IN ('20261110','20261112') AND dd.TrangThai='CO_MAT'
           ) AS DECIMAL(18,2)),'NHAP'
    FROM dbo.HocVien hv
    JOIN dbo.XepLopMamNon xl ON xl.MaHocVien=hv.MaHocVien AND xl.MaCoSo=@MN AND xl.LyDo=@Tag
    JOIN dbo.MucHocPhiLop phi ON phi.MaLop=xl.MaLop AND phi.ThangBatDau='20261101'
    JOIN dbo.DonGiaTienAn gia ON gia.MaLop=xl.MaLop AND gia.ThangBatDau='20261101'
    WHERE hv.MaCoSo=@MN AND hv.GhiChu=@Tag
      AND hv.SoDienThoaiLienHe IN ('0000000201','0000000202','0000000203','0000000204','0000000205','0000000206')
      AND NOT EXISTS (SELECT 1 FROM dbo.BangTinhPhiMamNon b WHERE b.MaHocVien=hv.MaHocVien AND b.MaThang=@ThangMN);

    INSERT dbo.ChiTietHocPhiMamNon(MaBangTinh,MaXepLop,MaHocVien,MaCoSo,NgayBatDauPhanBo,NgayKetThucPhanBo,
                                   SoNgayTinhPhi,SoNgayChuan,MucHocPhiThang,ThanhTien)
    SELECT b.MaBangTinh,xl.MaXepLop,hv.MaHocVien,@MN,'20261101','20261130',21,21,phi.HocPhiThang,phi.HocPhiThang
    FROM dbo.HocVien hv
    JOIN dbo.XepLopMamNon xl ON xl.MaHocVien=hv.MaHocVien AND xl.MaCoSo=@MN AND xl.LyDo=@Tag
    JOIN dbo.BangTinhPhiMamNon b ON b.MaHocVien=hv.MaHocVien AND b.MaThang=@ThangMN AND b.MaCoSo=@MN
    JOIN dbo.MucHocPhiLop phi ON phi.MaLop=xl.MaLop AND phi.ThangBatDau='20261101'
    WHERE hv.MaCoSo=@MN AND hv.GhiChu=@Tag
      AND NOT EXISTS (SELECT 1 FROM dbo.ChiTietHocPhiMamNon c WHERE c.MaBangTinh=b.MaBangTinh AND c.MaXepLop=xl.MaXepLop);

    INSERT dbo.ChiTietTienAnMamNon(MaBangTinh,MaXepLop,MaHocVien,MaCoSo,MaLop,MaDonGiaTienAn,TuNgay,DenNgay,
                                   SoNgayTinhTienAn,DonGiaApDung,ThanhTien)
    SELECT b.MaBangTinh,xl.MaXepLop,hv.MaHocVien,@MN,xl.MaLop,gia.MaDonGiaTienAn,'20261101','20261130',
           ca.SoNgay,gia.DonGiaNgay,CAST(ca.SoNgay*gia.DonGiaNgay AS DECIMAL(18,2))
    FROM dbo.HocVien hv
    JOIN dbo.XepLopMamNon xl ON xl.MaHocVien=hv.MaHocVien AND xl.MaCoSo=@MN AND xl.LyDo=@Tag
    JOIN dbo.BangTinhPhiMamNon b ON b.MaHocVien=hv.MaHocVien AND b.MaThang=@ThangMN AND b.MaCoSo=@MN
    JOIN dbo.DonGiaTienAn gia ON gia.MaLop=xl.MaLop AND gia.ThangBatDau='20261101'
    CROSS APPLY (SELECT COUNT(*) AS SoNgay FROM dbo.DiemDanhMamNon dd
                 WHERE dd.MaHocVien=hv.MaHocVien AND dd.MaCoSo=@MN
                   AND dd.NgayHoc IN ('20261110','20261112') AND dd.TrangThai='CO_MAT') ca
    WHERE hv.MaCoSo=@MN AND hv.GhiChu=@Tag
      AND NOT EXISTS (SELECT 1 FROM dbo.ChiTietTienAnMamNon c WHERE c.MaBangTinh=b.MaBangTinh AND c.MaXepLop=xl.MaXepLop);

    /* --------------------------------------------------------------------
       F. TAI CHINH: KHOAN THU + 1 PHIEU THU TT / 1 PHIEU THU MN
       Gia tri thu khong vuot so tien goc. Chi tiet = tong tren phieu.
    -------------------------------------------------------------------- */
    INSERT dbo.KhoanPhaiThu(MaHocVien,MaCoSo,LoaiKhoan,MaDangKy,SoTienGoc,HanThanhToan)
    SELECT dk.MaHocVien,@TT,'HOC_PHI_KHOA',dk.MaDangKy,dk.HocPhiSauGiam,'20261210'
    FROM dbo.DangKyKhoaHoc dk
    WHERE dk.MaCoSo=@TT AND dk.GhiChu=@Tag
      AND NOT EXISTS (SELECT 1 FROM dbo.KhoanPhaiThu k WHERE k.MaDangKy=dk.MaDangKy AND k.LoaiKhoan='HOC_PHI_KHOA');

    INSERT dbo.KhoanPhaiThu(MaHocVien,MaCoSo,LoaiKhoan,MaBangTinh,SoTienGoc,HanThanhToan)
    SELECT b.MaHocVien,@MN,v.LoaiKhoan,b.MaBangTinh,
           CASE WHEN v.LoaiKhoan='HOC_PHI_THANG' THEN b.TongHocPhi ELSE b.TongTienAn END,'20261210'
    FROM dbo.BangTinhPhiMamNon b
    CROSS JOIN (VALUES ('HOC_PHI_THANG'),('TIEN_AN')) v(LoaiKhoan)
    JOIN dbo.HocVien hv ON hv.MaHocVien=b.MaHocVien AND hv.MaCoSo=@MN AND hv.GhiChu=@Tag
    WHERE b.MaCoSo=@MN AND b.MaThang=@ThangMN
      AND NOT EXISTS (SELECT 1 FROM dbo.KhoanPhaiThu k
                      WHERE k.MaBangTinh=b.MaBangTinh AND k.LoaiKhoan=v.LoaiKhoan);

    DECLARE @HVTt1 INT=(SELECT MaHocVien FROM dbo.HocVien WHERE MaCoSo=@TT AND SoDienThoaiLienHe='0000000101' AND GhiChu=@Tag);
    DECLARE @HVMn1 INT=(SELECT MaHocVien FROM dbo.HocVien WHERE MaCoSo=@MN AND SoDienThoaiLienHe='0000000201' AND GhiChu=@Tag);
    DECLARE @ThuTT INT=(SELECT TOP(1) k.MaKhoanThu FROM dbo.KhoanPhaiThu k
                        JOIN dbo.DangKyKhoaHoc dk ON dk.MaDangKy=k.MaDangKy
                        WHERE dk.MaHocVien=@HVTt1 AND k.MaCoSo=@TT AND k.LoaiKhoan='HOC_PHI_KHOA' AND dk.GhiChu=@Tag);
    DECLARE @ThuMN_HocPhi INT=(SELECT TOP(1) k.MaKhoanThu FROM dbo.KhoanPhaiThu k JOIN dbo.BangTinhPhiMamNon b ON b.MaBangTinh=k.MaBangTinh
                                WHERE k.MaHocVien=@HVMn1 AND k.MaCoSo=@MN AND b.MaThang=@ThangMN AND k.LoaiKhoan='HOC_PHI_THANG');
    DECLARE @ThuMN_TienAn INT=(SELECT TOP(1) k.MaKhoanThu FROM dbo.KhoanPhaiThu k JOIN dbo.BangTinhPhiMamNon b ON b.MaBangTinh=k.MaBangTinh
                                WHERE k.MaHocVien=@HVMn1 AND k.MaCoSo=@MN AND b.MaThang=@ThangMN AND k.LoaiKhoan='TIEN_AN');
    DECLARE @TienAnMN DECIMAL(18,2)=(SELECT SoTienGoc FROM dbo.KhoanPhaiThu WHERE MaKhoanThu=@ThuMN_TienAn);
    IF @HVTt1 IS NULL OR @HVMn1 IS NULL OR @ThuTT IS NULL OR @ThuMN_HocPhi IS NULL OR @ThuMN_TienAn IS NULL
        THROW 57008, N'Thieu hoc vien hoac khoan phai thu cho phieu thu mau.', 1;

    INSERT dbo.PhieuThu(SoPhieuThu,MaHocVien,MaCoSo,TongTien,HinhThucThanhToan,MaTaiKhoan,TrangThai,MaGiaoDichNghiepVu,GhiChu)
    SELECT 'DEMO-THU-TT-001',@HVTt1,@TT,CAST(500000 AS DECIMAL(18,2)),'CHUYEN_KHOAN',@TkTT,'XAC_NHAN',
           'VINABACUS-DEMO-THU-TT-001',@Tag
    WHERE NOT EXISTS (SELECT 1 FROM dbo.PhieuThu p WHERE p.SoPhieuThu='DEMO-THU-TT-001');

    INSERT dbo.PhieuThu(SoPhieuThu,MaHocVien,MaCoSo,TongTien,HinhThucThanhToan,MaTaiKhoan,TrangThai,MaGiaoDichNghiepVu,GhiChu)
    SELECT 'DEMO-THU-MN-001',@HVMn1,@MN,CAST(500000 AS DECIMAL(18,2))+@TienAnMN,
           'TIEN_MAT',@TkMN,'XAC_NHAN','VINABACUS-DEMO-THU-MN-001',@Tag
    WHERE NOT EXISTS (SELECT 1 FROM dbo.PhieuThu p WHERE p.SoPhieuThu='DEMO-THU-MN-001');

    DECLARE @PhieuTT INT=(SELECT MaPhieuThu FROM dbo.PhieuThu WHERE SoPhieuThu='DEMO-THU-TT-001' AND MaHocVien=@HVTt1 AND MaCoSo=@TT);
    DECLARE @PhieuMN INT=(SELECT MaPhieuThu FROM dbo.PhieuThu WHERE SoPhieuThu='DEMO-THU-MN-001' AND MaHocVien=@HVMn1 AND MaCoSo=@MN);
    IF @PhieuTT IS NULL OR @PhieuMN IS NULL
        THROW 57009, N'Ma phieu thu demo da ton tai voi hoc vien / co so khac. Dung ghi du lieu.', 1;

    INSERT dbo.ChiTietPhieuThu(MaPhieuThu,MaKhoanThu,MaHocVien,MaCoSo,SoTienPhanBo,LoaiBienDong,MaGiaoDichNghiepVu)
    SELECT @PhieuTT,@ThuTT,@HVTt1,@TT,CAST(500000 AS DECIMAL(18,2)),'PHAN_BO','VINABACUS-DEMO-CT-THU-TT-001'
    WHERE NOT EXISTS (SELECT 1 FROM dbo.ChiTietPhieuThu c WHERE c.MaPhieuThu=@PhieuTT AND c.MaKhoanThu=@ThuTT AND c.LoaiBienDong='PHAN_BO');

    INSERT dbo.ChiTietPhieuThu(MaPhieuThu,MaKhoanThu,MaHocVien,MaCoSo,SoTienPhanBo,LoaiBienDong,MaGiaoDichNghiepVu)
    SELECT @PhieuMN,@ThuMN_HocPhi,@HVMn1,@MN,CAST(500000 AS DECIMAL(18,2)),'PHAN_BO','VINABACUS-DEMO-CT-THU-MN-HP-001'
    WHERE NOT EXISTS (SELECT 1 FROM dbo.ChiTietPhieuThu c WHERE c.MaPhieuThu=@PhieuMN AND c.MaKhoanThu=@ThuMN_HocPhi AND c.LoaiBienDong='PHAN_BO');

    INSERT dbo.ChiTietPhieuThu(MaPhieuThu,MaKhoanThu,MaHocVien,MaCoSo,SoTienPhanBo,LoaiBienDong,MaGiaoDichNghiepVu)
    SELECT @PhieuMN,@ThuMN_TienAn,@HVMn1,@MN,@TienAnMN,'PHAN_BO','VINABACUS-DEMO-CT-THU-MN-AN-001'
    WHERE NOT EXISTS (SELECT 1 FROM dbo.ChiTietPhieuThu c WHERE c.MaPhieuThu=@PhieuMN AND c.MaKhoanThu=@ThuMN_TienAn AND c.LoaiBienDong='PHAN_BO');

    /* --------------------------------------------------------------------
       G. KHO: HANG HOA, KY KHO, PHIEU TON DAU + CHI TIET (KHONG XUAT AM)
    -------------------------------------------------------------------- */
    INSERT dbo.HangHoa(MaCoSo,MaSanPham,TenHangHoa,DonViTinh,DonGiaSi,DonGiaXuatBan)
    SELECT cs.MaCoSo,v.MaSanPham,v.TenHangHoa,v.DonViTinh,v.GiaSi,v.GiaBan
    FROM (VALUES
       ('TRUNG_TAM','DEMO-TT-02',N'[TEST] Bộ toán tư duy',N'Bộ',CAST(150000 AS DECIMAL(18,2)),CAST(200000 AS DECIMAL(18,2))),
       ('TRUNG_TAM','DEMO-TT-03',N'[TEST] Tập tiếng Anh',N'Quyển',CAST(17000 AS DECIMAL(18,2)),CAST(25000 AS DECIMAL(18,2))),
       ('MAM_NON','DEMO-MN-02',N'[TEST] Hộp bút màu',N'Hộp',CAST(32000 AS DECIMAL(18,2)),CAST(48000 AS DECIMAL(18,2))),
       ('MAM_NON','DEMO-MN-03',N'[TEST] Giấy thủ công',N'Tập',CAST(10000 AS DECIMAL(18,2)),CAST(15000 AS DECIMAL(18,2)))
    ) v(LoaiCoSo,MaSanPham,TenHangHoa,DonViTinh,GiaSi,GiaBan)
    JOIN dbo.CoSo cs ON cs.LoaiCoSo=v.LoaiCoSo
    WHERE NOT EXISTS (SELECT 1 FROM dbo.HangHoa h WHERE h.MaCoSo=cs.MaCoSo AND h.MaSanPham=v.MaSanPham);

    INSERT dbo.KyKho(MaCoSo,Nam,Thang,TrangThai)
    SELECT cs.MaCoSo,2026,11,'MO' FROM dbo.CoSo cs WHERE cs.MaCoSo IN (@TT,@MN)
      AND NOT EXISTS (SELECT 1 FROM dbo.KyKho ky WHERE ky.MaCoSo=cs.MaCoSo AND ky.Nam=2026 AND ky.Thang=11);

    INSERT dbo.PhieuKho(MaCoSo,MaKyKho,LoaiPhieu,MaTaiKhoan,TrangThai,MaGiaoDichNghiepVu,GhiChu)
    SELECT cs.MaCoSo,ky.MaKyKho,'TON_DAU',
           CASE WHEN cs.MaCoSo=@TT THEN @TkTT ELSE @TkMN END,'XAC_NHAN',
           CASE WHEN cs.MaCoSo=@TT THEN 'VINABACUS-DEMO-KHO-TT-001' ELSE 'VINABACUS-DEMO-KHO-MN-001' END,@Tag
    FROM dbo.CoSo cs
    JOIN dbo.KyKho ky ON ky.MaCoSo=cs.MaCoSo AND ky.Nam=2026 AND ky.Thang=11
    WHERE cs.MaCoSo IN (@TT,@MN)
      AND NOT EXISTS (SELECT 1 FROM dbo.PhieuKho pk
                      WHERE pk.MaGiaoDichNghiepVu=CASE WHEN cs.MaCoSo=@TT THEN 'VINABACUS-DEMO-KHO-TT-001' ELSE 'VINABACUS-DEMO-KHO-MN-001' END);

    INSERT dbo.ChiTietPhieuKho(MaPhieuKho,MaCoSo,MaHangHoa,SoLuong,DonGiaSiTaiThoiDiem,DonGiaXuatBanTaiThoiDiem,GhiChu)
    SELECT pk.MaPhieuKho,pk.MaCoSo,h.MaHangHoa,
           CAST(CASE WHEN h.MaSanPham LIKE '%-01' THEN 20 ELSE 12 END AS DECIMAL(18,3)),
           h.DonGiaSi,h.DonGiaXuatBan,@Tag
    FROM dbo.PhieuKho pk
    JOIN dbo.HangHoa h ON h.MaCoSo=pk.MaCoSo
    WHERE pk.MaGiaoDichNghiepVu IN ('VINABACUS-DEMO-KHO-TT-001','VINABACUS-DEMO-KHO-MN-001')
      AND h.MaSanPham IN ('DEMO-TT-01','DEMO-TT-02','DEMO-TT-03','DEMO-MN-01','DEMO-MN-02','DEMO-MN-03')
      AND NOT EXISTS (SELECT 1 FROM dbo.ChiTietPhieuKho c WHERE c.MaPhieuKho=pk.MaPhieuKho AND c.MaHangHoa=h.MaHangHoa);


    /* --------------------------------------------------------------------
       I. BO SUNG KICH BAN NGÀY NGHI, HOC PHI RIENG, SUA DIEM DANH
       De UI co du lieu xem chi tiet nhung chua mo quy trinh phan quyen.
    -------------------------------------------------------------------- */
    INSERT dbo.NgayNghiChung(MaCoSo,NgayNghi,LyDo)
    SELECT @MN,CONVERT(DATE,'20261120',112),@Tag+N' Ngay sinh hoat chuyen de (nghi hoc gia dinh)'
    WHERE NOT EXISTS (SELECT 1 FROM dbo.NgayNghiChung WHERE MaCoSo=@MN AND NgayNghi='20261120');

    INSERT dbo.HocPhiRiengMamNon(MaHocVien,MaLop,ThangBatDau,HocPhiRieng,LyDo,MaTaiKhoanDuyet)
    SELECT hv.MaHocVien,xl.MaLop,'20261201',CAST(1450000 AS DECIMAL(18,2)),
           @Tag+N' Muc hoc phi thang 12 cua hoc sinh minh hoa',@TkMN
    FROM dbo.HocVien hv
    JOIN dbo.XepLopMamNon xl ON xl.MaHocVien=hv.MaHocVien AND xl.MaCoSo=@MN AND xl.LyDo=@Tag
    WHERE hv.MaCoSo=@MN AND hv.SoDienThoaiLienHe='0000000203' AND hv.GhiChu=@Tag
      AND NOT EXISTS (SELECT 1 FROM dbo.HocPhiRiengMamNon x
                      WHERE x.MaHocVien=hv.MaHocVien AND x.MaLop=xl.MaLop AND x.ThangBatDau='20261201');

    INSERT dbo.LichSuSuaDiemDanhMamNon(MaDiemDanh,TrangThaiTruoc,TrangThaiSau,LyDo,MaTaiKhoan,GhiChuSau)
    SELECT d.MaDiemDanh,'CHUA_DIEM_DANH','VANG_CO_PHEP',
           @Tag+N' Minh hoa lich su dieu chinh diem danh',@TkMN,
           N'Phu huynh xin nghi'
    FROM dbo.DiemDanhMamNon d
    JOIN dbo.HocVien hv ON hv.MaHocVien=d.MaHocVien AND hv.MaCoSo=@MN
    WHERE d.MaCoSo=@MN AND d.NgayHoc='20261112' AND d.TrangThai='VANG_CO_PHEP'
      AND hv.SoDienThoaiLienHe='0000000202' AND hv.GhiChu=@Tag
      AND NOT EXISTS (SELECT 1 FROM dbo.LichSuSuaDiemDanhMamNon ls
                      WHERE ls.MaDiemDanh=d.MaDiemDanh
                        AND ls.LyDo=@Tag+N' Minh hoa lich su dieu chinh diem danh');

    /* --------------------------------------------------------------------
       J. GOI NAM + XEP LOP CHUYEN CUNG KHOA (TRUNG TAM)
       Chi mo phong quy trinh; khong tao khoàn thu trung cho goi nam.
    -------------------------------------------------------------------- */
    DECLARE @HV6 INT=(SELECT MaHocVien FROM dbo.HocVien
                      WHERE MaCoSo=@TT AND SoDienThoaiLienHe='0000000106' AND GhiChu=@Tag);
    DECLARE @GiamGoiNam INT=(SELECT TOP(1) MaChuongTrinh FROM dbo.ChuongTrinhGiamGia
                             WHERE MaCoSo=@TT AND PhamViApDung='GOI_NAM'
                             ORDER BY MaChuongTrinh);
    IF @HV6 IS NULL OR @GiamGoiNam IS NULL
        THROW 57021, N'Thieu hoc vien test thu 6 hoac chuong trinh giam gia goi nam.',1;

    INSERT dbo.DangKyGoiNam(MaHocVien,MaCoSo,TenGoi,NgayDangKy,NgayBatDau,NgayKetThuc,
                            TongHocPhiGoc,MaChuongTrinh,SoTienGiam,HocPhiSauGiam,
                            MaTaiKhoanTao,MaGiaoDichNghiepVu,GhiChu)
    SELECT @HV6,@TT,N'[TEST] Goi nam Toan tu duy','20261101','20261101','20271031',
           CAST(6000000 AS DECIMAL(18,2)),@GiamGoiNam,
           CAST(900000 AS DECIMAL(18,2)),CAST(5100000 AS DECIMAL(18,2)),
           @TkTT,'VINABACUS-DEMO-GOI-NAM-001',@Tag
    WHERE NOT EXISTS (SELECT 1 FROM dbo.DangKyGoiNam
                      WHERE MaGiaoDichNghiepVu='VINABACUS-DEMO-GOI-NAM-001');

    DECLARE @GoiNam INT=(SELECT MaDangKyGoiNam FROM dbo.DangKyGoiNam
                        WHERE MaHocVien=@HV6 AND MaCoSo=@TT
                          AND MaGiaoDichNghiepVu='VINABACUS-DEMO-GOI-NAM-001');
    IF @GoiNam IS NULL THROW 57022,N'Goi nam demo trung ma giao dich voi du lieu khac.',1;

    INSERT dbo.DangKyKhoaHoc(MaHocVien,MaCoSo,MaKhoaHoc,MaDangKyGoiNam,SoThuTuTrongGoi,
                             NgayDangKy,NgayBatDau,NgayKetThucDuKien,TongSoBuoiDangKy,
                             HocPhiThoaThuan,SoTienGiam,HocPhiSauGiam,TrangThai,GhiChu)
    SELECT @HV6,@TT,@Kid1,@GoiNam,1,'20261101','20261103','20270331',
           k.TongSoBuoi,k.HocPhiMacDinh,0,k.HocPhiMacDinh,'DANG_HOC',
           @Tag+N' Khoa nam 1'
    FROM dbo.KhoaHoc k WHERE k.MaKhoaHoc=@Kid1 AND k.MaCoSo=@TT
      AND NOT EXISTS (SELECT 1 FROM dbo.DangKyKhoaHoc
                      WHERE MaDangKyGoiNam=@GoiNam AND SoThuTuTrongGoi=1);

    DECLARE @DkGoi INT=(SELECT MaDangKy FROM dbo.DangKyKhoaHoc
                       WHERE MaDangKyGoiNam=@GoiNam AND SoThuTuTrongGoi=1 AND MaHocVien=@HV6 AND MaCoSo=@TT);
    DECLARE @LopTTB INT=(SELECT MaLop FROM dbo.LopTrungTam
                        WHERE MaCoSo=@TT AND TenLop=N'[TEST] Lớp Toán tư duy B' AND MaKhoaHoc=@Kid1);
    DECLARE @LopTTA INT=(SELECT MaLop FROM dbo.LopTrungTam
                        WHERE MaCoSo=@TT AND TenLop=N'[MẪU] Lớp Toán A' AND MaKhoaHoc=@Kid1);
    IF @DkGoi IS NULL OR @LopTTB IS NULL OR @LopTTA IS NULL
        THROW 57023, N'Thieu khoa dang ky goi nam hoac lop Toan A/B.',1;

    INSERT dbo.XepLopTrungTam(MaDangKy,MaKhoaHoc,MaCoSo,MaLop,TuNgay,DenNgay,TrangThai,LyDo)
    SELECT @DkGoi,@Kid1,@TT,@LopTTB,'20261103','20261114','KET_THUC',@Tag+N' Lop cu'
    WHERE NOT EXISTS (SELECT 1 FROM dbo.XepLopTrungTam
                      WHERE MaDangKy=@DkGoi AND MaLop=@LopTTB);

    INSERT dbo.XepLopTrungTam(MaDangKy,MaKhoaHoc,MaCoSo,MaLop,TuNgay,TrangThai,LyDo)
    SELECT @DkGoi,@Kid1,@TT,@LopTTA,'20261115','HIEU_LUC',@Tag+N' Lop moi'
    WHERE NOT EXISTS (SELECT 1 FROM dbo.XepLopTrungTam
                      WHERE MaDangKy=@DkGoi AND MaLop=@LopTTA);

    DECLARE @XepCu INT=(SELECT MaXepLop FROM dbo.XepLopTrungTam
                       WHERE MaDangKy=@DkGoi AND MaLop=@LopTTB AND TrangThai='KET_THUC');
    DECLARE @XepMoi INT=(SELECT MaXepLop FROM dbo.XepLopTrungTam
                        WHERE MaDangKy=@DkGoi AND MaLop=@LopTTA AND TrangThai='HIEU_LUC');
    IF @XepCu IS NULL OR @XepMoi IS NULL
        THROW 57024,N'Thieu cap xep lop cu / moi cho tinh huong chuyen lop demo.',1;

    INSERT dbo.ChuyenLopTrungTam(MaCoSo,MaDangKyCu,MaDangKyDich,MaXepLopCu,MaXepLopMoi,
                                 LoaiChuyen,NgayChuyen,HocPhiCuSauGiam,TongSoBuoiCu,SoBuoiDaSuDung,
                                 GiaTriChuaSuDung,HocPhiMoiApDung,LyDo,MaTaiKhoan,MaGiaoDichNghiepVu)
    SELECT @TT,@DkGoi,@DkGoi,@XepCu,@XepMoi,'CUNG_KHOA','20261115',
           CAST(2000000 AS DECIMAL(18,2)),32,2,
           CAST(1875000 AS DECIMAL(18,2)),CAST(2000000 AS DECIMAL(18,2)),
           @Tag+N' Chuyen lop vi trung lich',@TkTT,'VINABACUS-DEMO-CHUYEN-LOP-001'
    WHERE NOT EXISTS (SELECT 1 FROM dbo.ChuyenLopTrungTam
                      WHERE MaGiaoDichNghiepVu='VINABACUS-DEMO-CHUYEN-LOP-001');

    /* --------------------------------------------------------------------
       K. DIEU CHINH THU (MINH HOA THAO TAC GHI CHU GIẢM PHAI THU)
    -------------------------------------------------------------------- */
    DECLARE @KhoanCanDieuChinh INT=(
        SELECT TOP(1) k.MaKhoanThu FROM dbo.KhoanPhaiThu k
        JOIN dbo.HocVien hv ON hv.MaHocVien=k.MaHocVien AND hv.MaCoSo=k.MaCoSo
        WHERE k.MaCoSo=@TT AND hv.SoDienThoaiLienHe='0000000102' AND hv.GhiChu=@Tag
          AND k.LoaiKhoan='HOC_PHI_KHOA'
        ORDER BY k.MaKhoanThu);
    IF @KhoanCanDieuChinh IS NULL THROW 57025,N'Thieu khoan thu cho dieu chinh demo.',1;

    INSERT dbo.DieuChinhKhoanThu(MaKhoanThu,SoTienDieuChinh,LoaiDieuChinh,LyDo,MaTaiKhoan,
                                 TrangThai,MaGiaoDichNghiepVu)
    SELECT @KhoanCanDieuChinh,CAST(-50000 AS DECIMAL(18,2)),'KHAC',
           @Tag+N' Giam 50.000 dong do sai sot nhap lieu',@TkTT,
           'XAC_NHAN','VINABACUS-DEMO-DIEU-CHINH-001'
    WHERE NOT EXISTS (SELECT 1 FROM dbo.DieuChinhKhoanThu
                      WHERE MaGiaoDichNghiepVu='VINABACUS-DEMO-DIEU-CHINH-001');

    /* --------------------------------------------------------------------
       L. KHO: PHIEU NHAP, XUAT + KIEM KE NHÁP HAI CO SO
       Chua sinh SoLieuChotKho: KyKho dang MO, khong gia lap so da chot.
    -------------------------------------------------------------------- */
    INSERT dbo.PhieuKho(MaCoSo,MaKyKho,LoaiPhieu,MaTaiKhoan,TrangThai,MaGiaoDichNghiepVu,GhiChu)
    SELECT cs.MaCoSo,ky.MaKyKho,v.LoaiPhieu,
           CASE WHEN cs.LoaiCoSo='TRUNG_TAM' THEN @TkTT ELSE @TkMN END,
           'XAC_NHAN',v.MaGD,@Tag
    FROM (VALUES
       ('TRUNG_TAM','NHAP','VINABACUS-DEMO-NHAP-TT-001'),
       ('TRUNG_TAM','XUAT_HS','VINABACUS-DEMO-XUAT-TT-001'),
       ('MAM_NON','NHAP','VINABACUS-DEMO-NHAP-MN-001'),
       ('MAM_NON','XUAT_HS','VINABACUS-DEMO-XUAT-MN-001')
    ) v(LoaiCoSo,LoaiPhieu,MaGD)
    JOIN dbo.CoSo cs ON cs.LoaiCoSo=v.LoaiCoSo
    JOIN dbo.KyKho ky ON ky.MaCoSo=cs.MaCoSo AND ky.Nam=2026 AND ky.Thang=11
    WHERE NOT EXISTS (SELECT 1 FROM dbo.PhieuKho p WHERE p.MaGiaoDichNghiepVu=v.MaGD);

    INSERT dbo.ChiTietPhieuKho(MaPhieuKho,MaCoSo,MaHangHoa,SoLuong,
                               DonGiaSiTaiThoiDiem,DonGiaXuatBanTaiThoiDiem,GhiChu)
    SELECT p.MaPhieuKho,p.MaCoSo,h.MaHangHoa,
           CAST(CASE WHEN p.LoaiPhieu='NHAP' THEN 5 ELSE 2 END AS DECIMAL(18,3)),
           h.DonGiaSi,h.DonGiaXuatBan,@Tag
    FROM dbo.PhieuKho p
    JOIN dbo.HangHoa h ON h.MaCoSo=p.MaCoSo
       AND h.MaSanPham=CASE WHEN p.MaCoSo=@TT THEN 'DEMO-TT-02' ELSE 'DEMO-MN-02' END
    WHERE p.MaGiaoDichNghiepVu IN (
       'VINABACUS-DEMO-NHAP-TT-001','VINABACUS-DEMO-XUAT-TT-001',
       'VINABACUS-DEMO-NHAP-MN-001','VINABACUS-DEMO-XUAT-MN-001')
      AND NOT EXISTS (SELECT 1 FROM dbo.ChiTietPhieuKho c
                      WHERE c.MaPhieuKho=p.MaPhieuKho AND c.MaHangHoa=h.MaHangHoa);

    INSERT dbo.KiemKeKho(MaKyKho,MaCoSo,NgayKiemKe,MaTaiKhoan,TrangThai,MaGiaoDichNghiepVu,GhiChu)
    SELECT ky.MaKyKho,cs.MaCoSo,CONVERT(DATETIME2(0),'2026-11-16T15:00:00',126),
           CASE WHEN cs.LoaiCoSo='TRUNG_TAM' THEN @TkTT ELSE @TkMN END,
           'NHAP',CASE WHEN cs.LoaiCoSo='TRUNG_TAM' THEN 'VINABACUS-DEMO-KIEM-KE-TT-001'
                      ELSE 'VINABACUS-DEMO-KIEM-KE-MN-001' END,@Tag
    FROM dbo.CoSo cs JOIN dbo.KyKho ky ON ky.MaCoSo=cs.MaCoSo AND ky.Nam=2026 AND ky.Thang=11
    WHERE cs.MaCoSo IN (@TT,@MN)
      AND NOT EXISTS (SELECT 1 FROM dbo.KiemKeKho kk
                      WHERE kk.MaGiaoDichNghiepVu=CASE WHEN cs.LoaiCoSo='TRUNG_TAM'
                          THEN 'VINABACUS-DEMO-KIEM-KE-TT-001' ELSE 'VINABACUS-DEMO-KIEM-KE-MN-001' END);

    INSERT dbo.ChiTietKiemKe(MaKiemKe,MaCoSo,MaHangHoa,TonHeThong,TonThucTe)
    SELECT kk.MaKiemKe,kk.MaCoSo,h.MaHangHoa,
           CAST(CASE WHEN h.MaSanPham LIKE '%-01' THEN 20
                     WHEN h.MaSanPham LIKE '%-02' THEN 15 ELSE 12 END AS DECIMAL(18,3)),
           CAST(CASE WHEN h.MaSanPham LIKE '%-01' THEN 20
                     WHEN h.MaSanPham LIKE '%-02' THEN 15 ELSE 12 END AS DECIMAL(18,3))
    FROM dbo.KiemKeKho kk JOIN dbo.HangHoa h ON h.MaCoSo=kk.MaCoSo
    WHERE kk.MaGiaoDichNghiepVu IN ('VINABACUS-DEMO-KIEM-KE-TT-001','VINABACUS-DEMO-KIEM-KE-MN-001')
      AND h.MaSanPham IN ('DEMO-TT-01','DEMO-TT-02','DEMO-TT-03','DEMO-MN-01','DEMO-MN-02','DEMO-MN-03')
      AND NOT EXISTS (SELECT 1 FROM dbo.ChiTietKiemKe c
                      WHERE c.MaKiemKe=kk.MaKiemKe AND c.MaHangHoa=h.MaHangHoa);

    /* --------------------------------------------------------------------
       M. KIEM TRA BO SUNG TRUOC KHI COMMIT
    -------------------------------------------------------------------- */
    IF (SELECT COUNT(*) FROM dbo.ChuyenLopTrungTam
        WHERE MaGiaoDichNghiepVu='VINABACUS-DEMO-CHUYEN-LOP-001')<>1
        THROW 57026,N'Chua tao duoc tinh huong chuyen lop Trung tam.',1;
    IF (SELECT COUNT(*) FROM dbo.PhieuKho WHERE MaGiaoDichNghiepVu IN (
       'VINABACUS-DEMO-NHAP-TT-001','VINABACUS-DEMO-XUAT-TT-001',
       'VINABACUS-DEMO-NHAP-MN-001','VINABACUS-DEMO-XUAT-MN-001'))<>4
        THROW 57027,N'Thieu phieu nhap xuat kho mau.',1;

    /* --------------------------------------------------------------------
       H. KIEM TRA TRONG GIAO DICH (LOI -> ROLLBACK TAT CA)
    -------------------------------------------------------------------- */
    IF EXISTS (
        SELECT 1 FROM dbo.PhieuThu p
        LEFT JOIN dbo.ChiTietPhieuThu c ON c.MaPhieuThu=p.MaPhieuThu AND c.LoaiBienDong='PHAN_BO'
        WHERE p.SoPhieuThu IN ('DEMO-THU-TT-001','DEMO-THU-MN-001')
        GROUP BY p.MaPhieuThu,p.TongTien
        HAVING COALESCE(SUM(c.SoTienPhanBo),0)<>p.TongTien
    ) THROW 57010, N'Tong tien PhieuThu demo khong bang tong ChiTietPhieuThu.', 1;

    IF EXISTS (
        SELECT 1 FROM dbo.BangTinhPhiMamNon b
        JOIN dbo.HocVien h ON h.MaHocVien=b.MaHocVien AND h.GhiChu=@Tag
        WHERE b.MaCoSo=@MN AND b.MaThang=@ThangMN
          AND (b.TongHocPhi<>(SELECT COALESCE(SUM(x.ThanhTien),0) FROM dbo.ChiTietHocPhiMamNon x WHERE x.MaBangTinh=b.MaBangTinh)
            OR b.TongTienAn<>(SELECT COALESCE(SUM(x.ThanhTien),0) FROM dbo.ChiTietTienAnMamNon x WHERE x.MaBangTinh=b.MaBangTinh))
    ) THROW 57011, N'BangTinhPhiMamNon demo chenh lech chi tiet.', 1;

    COMMIT TRANSACTION;
    PRINT N'OK: Bo sung du lieu test cho Trung tam, Mam non (khong doi cau truc / khong xoa du lieu).';
    PRINT N'Tiep theo: chay 03_KIEM_TRA_DU_LIEU.sql.';
END TRY
BEGIN CATCH
    IF XACT_STATE()<>0 ROLLBACK TRANSACTION;
    DECLARE @Msg NVARCHAR(2048)=ERROR_MESSAGE();
    PRINT N'DA ROLLBACK: '+@Msg;
    THROW;
END CATCH;
GO
SET NOEXEC OFF;
GO

