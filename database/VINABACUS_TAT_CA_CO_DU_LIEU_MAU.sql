-- VINABACUS: CAC FILE GHEP THEO THU TU
-- CHI CAI MOI TREN SQL SERVER 2017+; KHONG CHAY DE NANG CAP CSDL DA CO DU LIEU
-- Chua duoc kiem thu tren SQL Server thuc te.

-- VINABACUS - ban cau truc day du; GIU NGUYEN FILE GOC VINABACUS(2).sql
-- >>> 01_TAO_BANG.sql
-- VINABACUS / Microsoft SQL Server 2017+
-- Database schema: 55 application tables. No data or permissions are dropped.
-- IMPORTANT: Execute with a backup; test in non-production first.
-- Times stored in UTC for DATETIME2 fields (application converts to Asia/Ho_Chi_Minh).
SET NOCOUNT ON;
IF DB_ID(N'VINABACUS') IS NULL
    EXEC(N'CREATE DATABASE [VINABACUS]');
GO
USE [VINABACUS];
GO
-- Fresh installation only. Never silently overwrite or partially upgrade populated databases.
IF OBJECT_ID(N'dbo.CoSo',N'U') IS NOT NULL
    THROW 53000,N'VINABACUS already has application tables. Do NOT run fresh installer; use migration review.',1;
GO

PRINT N'Tao bang CoSo';
IF OBJECT_ID(N'dbo.CoSo', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[CoSo] (
        MaCoSo INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        LoaiCoSo VARCHAR(20) NOT NULL CHECK (LoaiCoSo IN ('MAM_NON','TRUNG_TAM')),
        TenCoSo NVARCHAR(200) NOT NULL,
        LogoCoSo NVARCHAR(500) NULL,
        TrangThai BIT NOT NULL DEFAULT (1),
        CONSTRAINT UQ_CoSo_Loai UNIQUE (LoaiCoSo)
    );
END;
GO

PRINT N'Tao bang CaiDatHeThong';
IF OBJECT_ID(N'dbo.CaiDatHeThong', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[CaiDatHeThong] (
        MaCaiDat TINYINT NOT NULL PRIMARY KEY CHECK (MaCaiDat=1),
        TenUngDung NVARCHAR(200) NOT NULL,
        LogoUngDung NVARCHAR(500) NULL
    );
END;
GO

PRINT N'Tao bang NhanVien';
IF OBJECT_ID(N'dbo.NhanVien', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[NhanVien] (
        MaNhanVien INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        HoTen NVARCHAR(150) NOT NULL,
        NgaySinh DATE NULL,
        GioiTinh VARCHAR(15) NULL CHECK (GioiTinh IN ('NAM','NU','KHAC')),
        SoDienThoai VARCHAR(20) NULL,
        Email VARCHAR(254) COLLATE Latin1_General_100_CI_AI NULL,
        EmailDaXacMinh BIT NOT NULL DEFAULT (0),
        CONSTRAINT CK_NhanVien_Email CHECK (Email IS NULL OR (CHARINDEX(' ',Email)=0 AND Email LIKE '%_@_%._%')),
        DiaChi NVARCHAR(400) NULL,
        TrangThai BIT NOT NULL DEFAULT (1)
    );
END;
GO

PRINT N'Tao bang NhanVienCoSo';
IF OBJECT_ID(N'dbo.NhanVienCoSo', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[NhanVienCoSo] (
        MaNhanVienCoSo INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaNhanVien INT NOT NULL,
        MaCoSo INT NOT NULL,
        TrangThai BIT NOT NULL DEFAULT (1),
        CONSTRAINT UQ_NhanVienCoSo UNIQUE (MaNhanVien,MaCoSo)
    );
END;
GO

PRINT N'Tao bang TaiKhoan';
IF OBJECT_ID(N'dbo.TaiKhoan', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[TaiKhoan] (
        MaTaiKhoan INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaNhanVien INT NOT NULL,
        TenDangNhap VARCHAR(100) COLLATE Latin1_General_100_CI_AI NOT NULL,
        MatKhauBam VARCHAR(255) NOT NULL,
        TrangThai BIT NOT NULL DEFAULT (1),
        PhaiDoiMatKhau BIT NOT NULL DEFAULT (1),
        NgayTao DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
        CONSTRAINT CK_TaiKhoan_TenDangNhap CHECK (LEN(LTRIM(RTRIM(TenDangNhap))) >= 3 AND LTRIM(RTRIM(TenDangNhap))=TenDangNhap AND CHARINDEX('@',TenDangNhap)=0),
        CONSTRAINT UQ_TaiKhoan_MaNhanVien UNIQUE (MaNhanVien),
        CONSTRAINT UQ_TaiKhoan_TenDangNhap UNIQUE (TenDangNhap),
        CONSTRAINT UQ_TaiKhoan_TaiKhoanNhanVien UNIQUE (MaTaiKhoan,MaNhanVien)
    );
END;
GO

-- Yeu cau OTP: chi dich vu may chu tin cay duoc ghi, xac minh va hoan tat.
-- BamOTP la gia tri HMAC/hashed duoc tao o may chu; khong luu OTP ro hoac bi mat Gmail.
PRINT N'Tao bang YeuCauKhoiPhucMatKhau';
IF OBJECT_ID(N'dbo.YeuCauKhoiPhucMatKhau',N'U') IS NULL
BEGIN
    CREATE TABLE dbo.YeuCauKhoiPhucMatKhau (
        MaYeuCauKhoiPhuc INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaTaiKhoan INT NOT NULL,
        MaYeuCauNgoai UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID(),
        BamOTP VARBINARY(32) NOT NULL,
        NgayTao DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
        NgayHetHan DATETIME2(0) NOT NULL,
        SoLanNhapSai TINYINT NOT NULL DEFAULT 0 CHECK (SoLanNhapSai BETWEEN 0 AND 5),
        LanGuiCuoi DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
        TrangThai VARCHAR(20) NOT NULL DEFAULT 'CHO_XAC_THUC'
            CHECK (TrangThai IN ('CHO_XAC_THUC','DA_XAC_MINH','DA_SU_DUNG','HET_HAN','HUY')),
        MaPhienDatLai UNIQUEIDENTIFIER NULL,
        HetHanPhienDatLai DATETIME2(0) NULL,
        NgayXacMinh DATETIME2(0) NULL,
        NgayHoanTat DATETIME2(0) NULL,
        CONSTRAINT UQ_YeuCauKhoiPhuc_Ngoai UNIQUE (MaYeuCauNgoai),
        CONSTRAINT CK_YeuCauKhoiPhuc_ThoiHan CHECK (NgayHetHan>NgayTao),
        CONSTRAINT CK_YeuCauKhoiPhuc_Phien CHECK (
          (MaPhienDatLai IS NULL AND HetHanPhienDatLai IS NULL) OR
          (MaPhienDatLai IS NOT NULL AND HetHanPhienDatLai IS NOT NULL))
    );
END;
GO

PRINT N'Tao bang VaiTro';
IF OBJECT_ID(N'dbo.VaiTro', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[VaiTro] (
        MaVaiTro INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaVaiTroChu VARCHAR(30) NOT NULL UNIQUE CHECK (MaVaiTroChu IN ('ADMIN','TO_TRUONG','GIAO_VIEN')),
        TenVaiTro NVARCHAR(100) NOT NULL
    );
END;
GO

PRINT N'Tao bang TaiKhoanCoSo';
IF OBJECT_ID(N'dbo.TaiKhoanCoSo', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[TaiKhoanCoSo] (
        MaTaiKhoanCoSo INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaTaiKhoan INT NOT NULL,
        MaNhanVien INT NOT NULL,
        MaCoSo INT NOT NULL,
        MaVaiTro INT NOT NULL,
        LaCoSoMacDinh BIT NOT NULL DEFAULT (0),
        TrangThai BIT NOT NULL DEFAULT (1),
        CONSTRAINT UQ_TaiKhoanCoSo UNIQUE (MaTaiKhoan,MaCoSo)
    );
END;
GO

PRINT N'Tao bang Quyen';
IF OBJECT_ID(N'dbo.Quyen', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[Quyen] (
        MaQuyen INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaQuyenChu VARCHAR(70) NOT NULL UNIQUE,
        TenQuyen NVARCHAR(150) NOT NULL
    );
END;
GO

PRINT N'Tao bang VaiTroQuyen';
IF OBJECT_ID(N'dbo.VaiTroQuyen', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[VaiTroQuyen] (
        MaVaiTroQuyen INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaVaiTro INT NOT NULL,
        MaQuyen INT NOT NULL,
        CONSTRAINT UQ_VaiTroQuyen UNIQUE (MaVaiTro,MaQuyen)
    );
END;
GO

PRINT N'Tao bang PhanQuyenTaiKhoan';
IF OBJECT_ID(N'dbo.PhanQuyenTaiKhoan', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[PhanQuyenTaiKhoan] (
        MaPhanQuyen INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaTaiKhoan INT NOT NULL,
        MaCoSo INT NOT NULL,
        MaQuyen INT NOT NULL,
        ChoPhep BIT NOT NULL,
        CONSTRAINT UQ_PhanQuyenTaiKhoan UNIQUE (MaTaiKhoan,MaCoSo,MaQuyen)
    );
END;
GO

PRINT N'Tao bang HocVien';
IF OBJECT_ID(N'dbo.HocVien', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[HocVien] (
        MaHocVien INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        HoTen NVARCHAR(150) NOT NULL,
        NgaySinh DATE NULL,
        GioiTinh VARCHAR(15) NULL CHECK (GioiTinh IN ('NAM','NU','KHAC')),
        TenNguoiLienHe NVARCHAR(150) NULL,
        SoDienThoaiLienHe VARCHAR(20) NULL,
        DiaChi NVARCHAR(400) NULL,
        GhiChu NVARCHAR(500) NULL,
        NgayTao DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
        TrangThaiHoSo VARCHAR(20) NOT NULL DEFAULT 'HOAT_DONG' CHECK (TrangThaiHoSo IN ('HOAT_DONG','NGUNG_SU_DUNG'))
    );
END;
GO

PRINT N'Tao bang MonHoc';
IF OBJECT_ID(N'dbo.MonHoc', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[MonHoc] (
        MaMonHoc INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaCoSo INT NOT NULL,
        TenMonHoc NVARCHAR(150) NOT NULL,
        GhiChu NVARCHAR(500) NULL,
        TrangThai BIT NOT NULL DEFAULT (1),
        CONSTRAINT UQ_MonHoc_CoSoTen UNIQUE (MaCoSo,TenMonHoc)
    );
END;
GO

PRINT N'Tao bang KhoaHoc';
IF OBJECT_ID(N'dbo.KhoaHoc', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[KhoaHoc] (
        MaKhoaHoc INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaCoSo INT NOT NULL,
        MaMonHoc INT NOT NULL,
        TenKhoaHoc NVARCHAR(200) NOT NULL,
        TongSoBuoi INT NOT NULL CHECK (TongSoBuoi>0),
        HocPhiMacDinh DECIMAL(18,2) NOT NULL CHECK (HocPhiMacDinh>=0),
        TrangThai BIT NOT NULL DEFAULT (1),
        CONSTRAINT UQ_KhoaHoc_CoSo UNIQUE (MaKhoaHoc,MaCoSo),
        CONSTRAINT UQ_KhoaHoc_MonCoSo UNIQUE (MaKhoaHoc,MaMonHoc,MaCoSo)
    );
END;
GO

PRINT N'Tao bang LopTrungTam';
IF OBJECT_ID(N'dbo.LopTrungTam', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[LopTrungTam] (
        MaLop INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaCoSo INT NOT NULL,
        MaKhoaHoc INT NOT NULL,
        TenLop NVARCHAR(180) NOT NULL,
        MaGiaoVien INT NULL,
        NgayKhaiGiang DATE NULL,
        TrangThai VARCHAR(20) NOT NULL DEFAULT 'DANG_MO' CHECK (TrangThai IN ('DANG_MO','DA_DONG','TAM_DUNG')),
        CONSTRAINT UQ_LopTrungTam_LopKhoaCoSo UNIQUE (MaLop,MaKhoaHoc,MaCoSo)
    );
END;
GO

PRINT N'Tao bang LichHocLop';
IF OBJECT_ID(N'dbo.LichHocLop', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[LichHocLop] (
        MaLichHoc INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaLop INT NOT NULL,
        CONSTRAINT UQ_LichHocLop_LichLop UNIQUE (MaLichHoc,MaLop),
        ThuTrongTuan TINYINT NOT NULL CHECK (ThuTrongTuan BETWEEN 2 AND 8),
        GioBatDau TIME(0) NULL,
        GioKetThuc TIME(0) NULL,
        TuNgay DATE NOT NULL,
        DenNgay DATE NULL,
        TrangThai VARCHAR(15) NOT NULL DEFAULT 'HIEU_LUC' CHECK (TrangThai IN ('HIEU_LUC','HUY')),
        CHECK (DenNgay IS NULL OR DenNgay>=TuNgay),
        CHECK (GioKetThuc IS NULL OR GioBatDau IS NULL OR GioKetThuc>GioBatDau)
    );
END;
GO

PRINT N'Tao bang BuoiHoc';
IF OBJECT_ID(N'dbo.BuoiHoc', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[BuoiHoc] (
        MaBuoiHoc INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaLop INT NOT NULL,
        MaLichHoc INT NULL,
        NgayHoc DATE NOT NULL,
        SoCa SMALLINT NOT NULL DEFAULT (1) CHECK (SoCa>=1),
        TrangThai VARCHAR(20) NOT NULL DEFAULT 'DU_KIEN' CHECK (TrangThai IN ('DU_KIEN','DA_DIEN_RA','HUY')),
        GhiChu NVARCHAR(500) NULL,
        CONSTRAINT UQ_BuoiHoc_LopNgayCa UNIQUE (MaLop,NgayHoc,SoCa)
    );
END;
GO

PRINT N'Tao bang ChuongTrinhGiamGia';
IF OBJECT_ID(N'dbo.ChuongTrinhGiamGia', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[ChuongTrinhGiamGia] (
        MaChuongTrinh INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaCoSo INT NOT NULL,
        TenChuongTrinh NVARCHAR(180) NOT NULL,
        PhamViApDung VARCHAR(12) NOT NULL DEFAULT 'KHOA_HOC'
            CHECK (PhamViApDung IN ('KHOA_HOC','GOI_NAM')),
        LoaiGiam VARCHAR(15) NOT NULL CHECK (LoaiGiam IN ('PHAN_TRAM','SO_TIEN')),
        GiaTriGiam DECIMAL(18,2) NOT NULL CHECK (GiaTriGiam>=0),
        TuNgay DATE NOT NULL,
        DenNgay DATE NOT NULL,
        TrangThai BIT NOT NULL DEFAULT (1),
        CHECK (DenNgay>=TuNgay),
        CHECK (LoaiGiam<>'PHAN_TRAM' OR GiaTriGiam<=100),
        CONSTRAINT UQ_ChuongTrinhGiamGia_CoSo UNIQUE (MaChuongTrinh,MaCoSo)
    );
END;
GO

PRINT N'Tao bang GiamGiaKhoaHoc';
IF OBJECT_ID(N'dbo.GiamGiaKhoaHoc', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[GiamGiaKhoaHoc] (
        MaGiamGiaKhoa INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaChuongTrinh INT NOT NULL,
        MaKhoaHoc INT NOT NULL,
        MaCoSo INT NOT NULL,
        CONSTRAINT UQ_GiamGiaKhoaHoc UNIQUE (MaChuongTrinh,MaKhoaHoc)
    );
END;
GO

-- Mot goi nam = mot cam ket hoc 12 thang; moi khoa van la DangKyKhoaHoc doc lap.
-- Tong hoc phi la snapshot; khoan phai thu CHI o tung khoa, KHONG tao khoan thu o goi.
PRINT N'Tao bang DangKyGoiNam';
IF OBJECT_ID(N'dbo.DangKyGoiNam',N'U') IS NULL
BEGIN
    CREATE TABLE dbo.DangKyGoiNam (
        MaDangKyGoiNam INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaHocVien INT NOT NULL,
        MaCoSo INT NOT NULL,
        MaYeuCau INT NULL,
        TenGoi NVARCHAR(200) NOT NULL,
        NgayDangKy DATE NOT NULL,
        NgayBatDau DATE NOT NULL,
        NgayKetThuc DATE NOT NULL,
        TongHocPhiGoc DECIMAL(18,2) NOT NULL CHECK (TongHocPhiGoc>=0),
        MaChuongTrinh INT NULL,
        SoTienGiam DECIMAL(18,2) NOT NULL DEFAULT 0 CHECK (SoTienGiam>=0),
        HocPhiSauGiam DECIMAL(18,2) NOT NULL CHECK (HocPhiSauGiam>=0),
        TrangThai VARCHAR(25) NOT NULL DEFAULT 'DANG_HOC'
            CHECK (TrangThai IN ('DANG_HOC','HOAN_THANH','NGHI_HAN','HUY')),
        MaTaiKhoanTao INT NOT NULL,
        MaGiaoDichNghiepVu VARCHAR(64) NOT NULL UNIQUE,
        NgayTao DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
        GhiChu NVARCHAR(500) NULL,
        CONSTRAINT UQ_DangKyGoiNam_HocVienCoSo UNIQUE (MaDangKyGoiNam,MaHocVien,MaCoSo),
        CONSTRAINT CK_DangKyGoiNam_Tong CHECK
            (HocPhiSauGiam=TongHocPhiGoc-SoTienGiam AND SoTienGiam<=TongHocPhiGoc),
        CONSTRAINT CK_DangKyGoiNam_Nam CHECK
            (NgayKetThuc=DATEADD(DAY,-1,DATEADD(YEAR,1,NgayBatDau)))
    );
END;
GO

PRINT N'Tao bang DangKyKhoaHoc';
IF OBJECT_ID(N'dbo.DangKyKhoaHoc', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[DangKyKhoaHoc] (
        MaDangKy INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaHocVien INT NOT NULL,
        MaCoSo INT NOT NULL,
        MaKhoaHoc INT NOT NULL,
        MaDangKyGoiNam INT NULL,
        SoThuTuTrongGoi INT NULL,
        MaYeuCau INT NULL,
        NgayDangKy DATE NOT NULL,
        NgayBatDau DATE NOT NULL,
        NgayKetThucDuKien DATE NULL,
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
        CONSTRAINT CK_DangKyKhoaHoc_GoiNam CHECK
           ((MaDangKyGoiNam IS NULL AND SoThuTuTrongGoi IS NULL) OR
            (MaDangKyGoiNam IS NOT NULL AND SoThuTuTrongGoi>0)),
        CHECK (NgayKetThucDuKien IS NULL OR NgayKetThucDuKien>=NgayBatDau)
    );
END;
GO

PRINT N'Tao bang XepLopTrungTam';
IF OBJECT_ID(N'dbo.XepLopTrungTam', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[XepLopTrungTam] (
        MaXepLop INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaDangKy INT NOT NULL,
        MaKhoaHoc INT NOT NULL,
        MaCoSo INT NOT NULL,
        MaLop INT NOT NULL,
        TuNgay DATE NOT NULL,
        DenNgay DATE NULL,
        TrangThai VARCHAR(20) NOT NULL DEFAULT 'HIEU_LUC' CHECK (TrangThai IN ('HIEU_LUC','KET_THUC','HUY')),
        LyDo NVARCHAR(500) NULL,
        CHECK (DenNgay IS NULL OR DenNgay>=TuNgay),
        CONSTRAINT UQ_XepLopTrungTam_DangKy UNIQUE (MaXepLop,MaDangKy)
    );
END;
GO

PRINT N'Tao bang SapXepBuoiHoc';
IF OBJECT_ID(N'dbo.SapXepBuoiHoc', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[SapXepBuoiHoc] (
        MaSapXep INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaDangKy INT NOT NULL,
        MaHocVien INT NOT NULL,
        MaBuoiHoc INT NOT NULL,
        LoaiSapXep VARCHAR(20) NOT NULL CHECK (LoaiSapXep IN ('CHINH_THUC','DOI_BUOI','HOC_BO_SUNG','HOC_BU')),
        MaSapXepNguon INT NULL,
        TrangThai VARCHAR(20) NOT NULL DEFAULT 'DA_XEP' CHECK (TrangThai IN ('DA_XEP','HOAN_THANH','HUY')),
        NgayTao DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
        CONSTRAINT UQ_SapXepBuoiHoc_NguonDangKy UNIQUE(MaSapXep,MaDangKy),
        CHECK (MaSapXepNguon IS NULL OR MaSapXepNguon<>MaSapXep),
        CHECK (LoaiSapXep NOT IN ('DOI_BUOI','HOC_BU') OR MaSapXepNguon IS NOT NULL)
    );
END;
GO

PRINT N'Tao bang DiemDanhTrungTam';
IF OBJECT_ID(N'dbo.DiemDanhTrungTam', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[DiemDanhTrungTam] (
        MaDiemDanh INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaSapXep INT NOT NULL UNIQUE,
        TrangThai VARCHAR(25) NOT NULL CHECK (TrangThai IN ('CO_MAT','VANG_CO_PHEP','VANG_KHONG_PHEP','CHUA_DIEM_DANH')),
        GhiChu NVARCHAR(500) NULL,
        MaTaiKhoan INT NOT NULL,
        NgayCapNhat DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME()
    );
END;
GO

PRINT N'Tao bang ChuyenLopTrungTam';
IF OBJECT_ID(N'dbo.ChuyenLopTrungTam', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[ChuyenLopTrungTam] (
        MaChuyenLop INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaCoSo INT NOT NULL,
        MaDangKyCu INT NOT NULL,
        MaDangKyMoi INT NULL,
        MaDangKyDich INT NOT NULL,
        MaXepLopCu INT NOT NULL,
        MaXepLopMoi INT NOT NULL,
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
        CHECK (MaDangKyDich=ISNULL(MaDangKyMoi,MaDangKyCu))
    );
END;
GO

PRINT N'Tao bang NamHoc';
IF OBJECT_ID(N'dbo.NamHoc', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[NamHoc] (
        MaNamHoc INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaCoSo INT NOT NULL,
        TenNamHoc NVARCHAR(50) NOT NULL,
        TuNgay DATE NOT NULL,
        DenNgay DATE NOT NULL,
        CHECK (DenNgay>=TuNgay),
        CONSTRAINT UQ_NamHoc_CoSoTen UNIQUE (MaCoSo,TenNamHoc),
        CONSTRAINT UQ_NamHoc_MaCoSo UNIQUE (MaNamHoc,MaCoSo)
    );
END;
GO

PRINT N'Tao bang LopMamNon';
IF OBJECT_ID(N'dbo.LopMamNon', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[LopMamNon] (
        MaLop INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaNamHoc INT NOT NULL,
        MaCoSo INT NOT NULL,
        TenLop NVARCHAR(150) NOT NULL,
        TrangThai BIT NOT NULL DEFAULT (1),
        CONSTRAINT UQ_LopMamNon_NamTen UNIQUE (MaNamHoc,TenLop),
        CONSTRAINT UQ_LopMamNon_CoSo UNIQUE (MaLop,MaCoSo)
    );
END;
GO

PRINT N'Tao bang PhanCongGiaoVienMamNon';
IF OBJECT_ID(N'dbo.PhanCongGiaoVienMamNon', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[PhanCongGiaoVienMamNon] (
        MaPhanCong INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaLop INT NOT NULL,
        MaCoSo INT NOT NULL,
        MaNhanVien INT NOT NULL,
        TuNgay DATE NOT NULL,
        DenNgay DATE NULL,
        CHECK (DenNgay IS NULL OR DenNgay>=TuNgay)
    );
END;
GO

PRINT N'Tao bang XepLopMamNon';
IF OBJECT_ID(N'dbo.XepLopMamNon', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[XepLopMamNon] (
        MaXepLop INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaHocVien INT NOT NULL,
        MaCoSo INT NOT NULL,
        MaLop INT NOT NULL,
        MaYeuCau INT NULL,
        TuNgay DATE NOT NULL,
        DenNgay DATE NULL,
        NgayGhiNhan DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
        TrangThai VARCHAR(20) NOT NULL DEFAULT 'HIEU_LUC' CHECK (TrangThai IN ('HIEU_LUC','CHUYEN_LOP','NGHI_HAN','HUY')),
        LyDo NVARCHAR(500) NULL,
        CHECK (DenNgay IS NULL OR DenNgay>=TuNgay),
        CONSTRAINT UQ_XepLopMamNon_HocVienCoSo UNIQUE (MaXepLop,MaHocVien,MaCoSo),
        CONSTRAINT UQ_XepLopMamNon_Lop UNIQUE (MaXepLop,MaLop)
    );
END;
GO

PRINT N'Tao bang NgayNghiChung';
IF OBJECT_ID(N'dbo.NgayNghiChung', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[NgayNghiChung] (
        MaNgayNghi INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaCoSo INT NOT NULL,
        NgayNghi DATE NOT NULL,
        LyDo NVARCHAR(300) NOT NULL,
        CONSTRAINT UQ_NgayNghiChung UNIQUE (MaCoSo,NgayNghi)
    );
END;
GO

PRINT N'Tao bang ThangMamNon';
IF OBJECT_ID(N'dbo.ThangMamNon', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[ThangMamNon] (
        MaThang INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaCoSo INT NOT NULL,
        Nam SMALLINT NOT NULL CHECK (Nam BETWEEN 2000 AND 2100),
        Thang TINYINT NOT NULL CHECK (Thang BETWEEN 1 AND 12),
        SoNgayChuan TINYINT NOT NULL CHECK (SoNgayChuan BETWEEN 0 AND 31),
        LyDoDieuChinhNgay NVARCHAR(500) NULL,
        TrangThai VARCHAR(15) NOT NULL DEFAULT 'MO' CHECK (TrangThai IN ('MO','CHOT')),
        NgayChot DATETIME2(0) NULL,
        MaNguoiChot INT NULL,
        CONSTRAINT UQ_ThangMamNon_CoSoKy UNIQUE (MaCoSo,Nam,Thang),
        CONSTRAINT UQ_ThangMamNon_MaCoSo UNIQUE (MaThang,MaCoSo)
    );
END;
GO

PRINT N'Tao bang DiemDanhMamNon';
IF OBJECT_ID(N'dbo.DiemDanhMamNon', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[DiemDanhMamNon] (
        MaDiemDanh INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaHocVien INT NOT NULL,
        MaCoSo INT NOT NULL,
        CONSTRAINT UQ_DiemDanhMamNon_DiemHocVienCoSo UNIQUE (MaDiemDanh,MaHocVien,MaCoSo),
        MaXepLop INT NOT NULL,
        NgayHoc DATE NOT NULL,
        TrangThai VARCHAR(25) NOT NULL DEFAULT 'CHUA_DIEM_DANH' CHECK (TrangThai IN ('CHUA_DIEM_DANH','CO_MAT','VANG_CO_PHEP','VANG_KHONG_PHEP')),
        GhiChu NVARCHAR(500) NULL,
        MaTaiKhoanDiemDanh INT NOT NULL,
        ThoiDiemCapNhat DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
        CONSTRAINT UQ_DiemDanhMamNon_Ngay UNIQUE (MaHocVien,MaCoSo,NgayHoc)
    );
END;
GO

PRINT N'Tao bang LichSuSuaDiemDanhMamNon';
IF OBJECT_ID(N'dbo.LichSuSuaDiemDanhMamNon', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[LichSuSuaDiemDanhMamNon] (
        MaLichSu INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaDiemDanh INT NOT NULL,
        TrangThaiTruoc VARCHAR(25) NOT NULL,
        TrangThaiSau VARCHAR(25) NOT NULL,
        GhiChuTruoc NVARCHAR(500) NULL,
        GhiChuSau NVARCHAR(500) NULL,
        LyDo NVARCHAR(500) NOT NULL,
        MaTaiKhoan INT NOT NULL,
        NgaySua DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME()
    );
END;
GO

PRINT N'Tao bang MucHocPhiLop';
IF OBJECT_ID(N'dbo.MucHocPhiLop', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[MucHocPhiLop] (
        MaMucHocPhi INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaLop INT NOT NULL,
        ThangBatDau DATE NOT NULL,
        HocPhiThang DECIMAL(18,2) NOT NULL CHECK (HocPhiThang>=0),
        MaTaiKhoanThietLap INT NOT NULL,
        NgayTao DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
        CONSTRAINT UQ_MucHocPhiLop UNIQUE (MaLop,ThangBatDau),
        CHECK (DAY(ThangBatDau)=1)
    );
END;
GO

PRINT N'Tao bang HocPhiRiengMamNon';
IF OBJECT_ID(N'dbo.HocPhiRiengMamNon', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[HocPhiRiengMamNon] (
        MaHocPhiRieng INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaHocVien INT NOT NULL,
        MaLop INT NOT NULL,
        ThangBatDau DATE NOT NULL,
        HocPhiRieng DECIMAL(18,2) NOT NULL CHECK (HocPhiRieng>=0),
        LyDo NVARCHAR(500) NOT NULL,
        MaTaiKhoanDuyet INT NOT NULL,
        NgayTao DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
        CONSTRAINT UQ_HocPhiRiengMamNon UNIQUE (MaHocVien,MaLop,ThangBatDau),
        CHECK (DAY(ThangBatDau)=1)
    );
END;
GO

PRINT N'Tao bang DonGiaTienAn';
IF OBJECT_ID(N'dbo.DonGiaTienAn', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[DonGiaTienAn] (
        MaDonGiaTienAn INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaLop INT NOT NULL,
        ThangBatDau DATE NOT NULL,
        DonGiaNgay DECIMAL(18,2) NOT NULL CHECK (DonGiaNgay>=0),
        MaTaiKhoanThietLap INT NOT NULL,
        NgayTao DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
        CONSTRAINT UQ_DonGiaTienAn UNIQUE (MaLop,ThangBatDau),
        CONSTRAINT UQ_DonGiaTienAn_MaLop UNIQUE (MaDonGiaTienAn,MaLop),
        CHECK (DAY(ThangBatDau)=1)
    );
END;
GO

PRINT N'Tao bang BangTinhPhiMamNon';
IF OBJECT_ID(N'dbo.BangTinhPhiMamNon', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[BangTinhPhiMamNon] (
        MaBangTinh INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaHocVien INT NOT NULL,
        MaCoSo INT NOT NULL,
        MaThang INT NOT NULL,
        NgayNhapHocTrongThang DATE NULL,
        NgayNghiHanTrongThang DATE NULL,
        TongHocPhi DECIMAL(18,2) NOT NULL DEFAULT (0) CHECK (TongHocPhi>=0),
        TongTienAn DECIMAL(18,2) NOT NULL DEFAULT (0) CHECK (TongTienAn>=0),
        TrangThai VARCHAR(15) NOT NULL DEFAULT 'NHAP' CHECK (TrangThai IN ('NHAP','CHOT')),
        NgayTinh DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
        NgayChot DATETIME2(0) NULL,
        CONSTRAINT UQ_BangTinhPhiMamNon_HocVienThang UNIQUE (MaHocVien,MaThang),
        CONSTRAINT UQ_BangTinhPhiMamNon_HocVienCoSo UNIQUE (MaBangTinh,MaHocVien,MaCoSo)
    );
END;
GO

PRINT N'Tao bang ChiTietHocPhiMamNon';
IF OBJECT_ID(N'dbo.ChiTietHocPhiMamNon', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[ChiTietHocPhiMamNon] (
        MaChiTietHocPhi INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaBangTinh INT NOT NULL,
        MaXepLop INT NOT NULL,
        MaHocVien INT NOT NULL,
        MaCoSo INT NOT NULL,
        NgayBatDauPhanBo DATE NOT NULL,
        NgayKetThucPhanBo DATE NOT NULL,
        SoNgayTinhPhi INT NOT NULL CHECK (SoNgayTinhPhi>=0),
        SoNgayChuan INT NOT NULL CHECK (SoNgayChuan>0),
        MucHocPhiThang DECIMAL(18,2) NOT NULL CHECK (MucHocPhiThang>=0),
        ThanhTien DECIMAL(18,2) NOT NULL CHECK (ThanhTien>=0),
        CHECK (NgayKetThucPhanBo>=NgayBatDauPhanBo)
    );
END;
GO

PRINT N'Tao bang ChiTietTienAnMamNon';
IF OBJECT_ID(N'dbo.ChiTietTienAnMamNon', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[ChiTietTienAnMamNon] (
        MaChiTietTienAn INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaBangTinh INT NOT NULL,
        MaXepLop INT NOT NULL,
        MaHocVien INT NOT NULL,
        MaCoSo INT NOT NULL,
        MaLop INT NOT NULL,
        MaDonGiaTienAn INT NOT NULL,
        TuNgay DATE NOT NULL,
        DenNgay DATE NOT NULL,
        SoNgayTinhTienAn INT NOT NULL CHECK (SoNgayTinhTienAn>=0),
        DonGiaApDung DECIMAL(18,2) NOT NULL CHECK (DonGiaApDung>=0),
        ThanhTien DECIMAL(18,2) NOT NULL CHECK (ThanhTien>=0),
        CHECK (DenNgay>=TuNgay)
    );
END;
GO

PRINT N'Tao bang PhatSinhTienAn';
IF OBJECT_ID(N'dbo.PhatSinhTienAn', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[PhatSinhTienAn] (
        MaPhatSinhTienAn INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaHocVien INT NOT NULL,
        MaCoSo INT NOT NULL,
        LoaiBienDong VARCHAR(25) NOT NULL CHECK (LoaiBienDong IN ('TAO','SU_DUNG','HOAN','HOAN_TAC_TAO','HOAN_TAC_SU_DUNG')),
        SoTien DECIMAL(18,2) NOT NULL CHECK (SoTien>0),
        MaDiemDanh INT NULL,
        MaBangTinh INT NULL,
        MaKhoanThuDich INT NULL,
        MaPhieuHoan INT NULL,
        MaPhatSinhGoc INT NULL,
        MaGiaoDichNghiepVu VARCHAR(64) NOT NULL UNIQUE,
        NgayPhatSinh DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
        MaTaiKhoan INT NOT NULL,
        LyDo NVARCHAR(500) NOT NULL
    );
END;
GO

PRINT N'Tao bang KhachTiemNang';
IF OBJECT_ID(N'dbo.KhachTiemNang', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[KhachTiemNang] (
        MaKhach INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaCoSo INT NOT NULL,
        CONSTRAINT UQ_KhachTiemNang_KhachCoSo UNIQUE (MaKhach,MaCoSo),
        HoTen NVARCHAR(150) NOT NULL,
        SoDienThoai VARCHAR(20) NULL,
        TenNguoiLienHe NVARCHAR(150) NULL,
        NhuCau NVARCHAR(500) NULL,
        TrangThai VARCHAR(30) NOT NULL DEFAULT 'TIEP_NHAN' CHECK (TrangThai IN ('TIEP_NHAN','HOC_THU','CHO_XAC_NHAN','CHO_XEP_LOP','DA_CHUYEN_DOI','KHONG_DANG_KY')),
        DaHocThu BIT NOT NULL DEFAULT (0),
        MaNhanVienPhuTrach INT NULL,
        NgayTiepNhan DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
        GhiChu NVARCHAR(500) NULL
    );
END;
GO

PRINT N'Tao bang LichSuTuVan';
IF OBJECT_ID(N'dbo.LichSuTuVan', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[LichSuTuVan] (
        MaTuVan INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaKhach INT NOT NULL,
        MaTaiKhoan INT NOT NULL,
        NgayTuVan DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
        NoiDung NVARCHAR(1000) NOT NULL,
        KetQua NVARCHAR(500) NULL,
        NgayHenTiep DATE NULL
    );
END;
GO

PRINT N'Tao bang YeuCauXepLop';
IF OBJECT_ID(N'dbo.YeuCauXepLop', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[YeuCauXepLop] (
        MaYeuCau INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaKhach INT NOT NULL,
        MaHocVien INT NOT NULL,
        MaCoSo INT NOT NULL,
        MaKhoaHoc INT NULL,
        NhuCau NVARCHAR(500) NULL,
        TrangThai VARCHAR(25) NOT NULL DEFAULT 'CHO_XEP_LOP' CHECK (TrangThai IN ('CHO_XEP_LOP','HOAN_TAT','HUY')),
        NgayXacNhan DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
        NgayHoanTat DATETIME2(0) NULL,
        MaGiaoDichHoanTat VARCHAR(64) NULL,
        CONSTRAINT UQ_YeuCauXepLop_MaCoSo UNIQUE (MaYeuCau,MaCoSo),
        CONSTRAINT UQ_YeuCauXepLop_HocVienCoSo UNIQUE (MaYeuCau,MaHocVien,MaCoSo)
    );
END;
GO

PRINT N'Tao bang KhoanPhaiThu';
IF OBJECT_ID(N'dbo.KhoanPhaiThu', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[KhoanPhaiThu] (
        MaKhoanThu INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaHocVien INT NOT NULL,
        MaCoSo INT NOT NULL,
        LoaiKhoan VARCHAR(25) NOT NULL CHECK (LoaiKhoan IN ('HOC_PHI_KHOA','HOC_PHI_THANG','TIEN_AN')),
        MaDangKy INT NULL,
        MaBangTinh INT NULL,
        SoTienGoc DECIMAL(18,2) NOT NULL CHECK (SoTienGoc>=0),
        NgayPhatSinh DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
        HanThanhToan DATE NULL,
        TrangThai VARCHAR(15) NOT NULL DEFAULT 'HIEU_LUC' CHECK (TrangThai IN ('HIEU_LUC','HUY')),
        CONSTRAINT UQ_KhoanPhaiThu_HocVienCoSo UNIQUE (MaKhoanThu,MaHocVien,MaCoSo),
        CHECK ((LoaiKhoan='HOC_PHI_KHOA' AND MaDangKy IS NOT NULL AND MaBangTinh IS NULL) OR
               (LoaiKhoan IN ('HOC_PHI_THANG','TIEN_AN') AND MaBangTinh IS NOT NULL AND MaDangKy IS NULL))
    );
END;
GO

PRINT N'Tao bang DieuChinhKhoanThu';
IF OBJECT_ID(N'dbo.DieuChinhKhoanThu', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[DieuChinhKhoanThu] (
        MaDieuChinh INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaKhoanThu INT NOT NULL,
        SoTienDieuChinh DECIMAL(18,2) NOT NULL CHECK (SoTienDieuChinh<>0),
        LoaiDieuChinh VARCHAR(30) NOT NULL CHECK (LoaiDieuChinh IN ('CHUYEN_LOP','CHUYEN_KHOA','SAU_CHOT','KHAC')),
        MaThangNguon INT NULL,
        MaThangGhiNhan INT NULL,
        LyDo NVARCHAR(500) NOT NULL,
        MaTaiKhoan INT NOT NULL,
        NgayDieuChinh DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
        TrangThai VARCHAR(15) NOT NULL DEFAULT 'XAC_NHAN' CHECK (TrangThai IN ('NHAP','XAC_NHAN','HUY')),
        MaGiaoDichNghiepVu VARCHAR(64) NOT NULL UNIQUE
    );
END;
GO

PRINT N'Tao bang PhieuThu';
IF OBJECT_ID(N'dbo.PhieuThu', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[PhieuThu] (
        MaPhieuThu INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        SoPhieuThu VARCHAR(40) NOT NULL UNIQUE,
        MaHocVien INT NOT NULL,
        MaCoSo INT NOT NULL,
        TongTien DECIMAL(18,2) NOT NULL CHECK (TongTien>0),
        NgayThu DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
        HinhThucThanhToan VARCHAR(25) NOT NULL CHECK (HinhThucThanhToan IN ('TIEN_MAT','CHUYEN_KHOAN','KHAC')),
        MaTaiKhoan INT NOT NULL,
        TrangThai VARCHAR(15) NOT NULL DEFAULT 'XAC_NHAN' CHECK (TrangThai IN ('NHAP','XAC_NHAN','HUY')),
        NgayHuy DATETIME2(0) NULL,
        MaTaiKhoanHuy INT NULL,
        LyDoHuy NVARCHAR(500) NULL,
        MaGiaoDichNghiepVu VARCHAR(64) NOT NULL UNIQUE,
        GhiChu NVARCHAR(500) NULL,
        CONSTRAINT UQ_PhieuThu_HocVienCoSo UNIQUE (MaPhieuThu,MaHocVien,MaCoSo)
    );
END;
GO

PRINT N'Tao bang ChiTietPhieuThu';
IF OBJECT_ID(N'dbo.ChiTietPhieuThu', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[ChiTietPhieuThu] (
        MaChiTietPhieuThu INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaPhieuThu INT NOT NULL,
        MaKhoanThu INT NOT NULL,
        MaHocVien INT NOT NULL,
        MaCoSo INT NOT NULL,
        SoTienPhanBo DECIMAL(18,2) NOT NULL CHECK (SoTienPhanBo>0),
        LoaiBienDong VARCHAR(25) NOT NULL CHECK (LoaiBienDong IN ('PHAN_BO','HOAN_TAC_PHAN_BO')),
        MaChiTietNguon INT NULL,
        MaDieuChinh INT NULL,
        MaGiaoDichNghiepVu VARCHAR(64) NOT NULL UNIQUE,
        NgayGhiNhan DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
        CONSTRAINT UQ_ChiTietPhieuThu_NguonDayDu UNIQUE (MaChiTietPhieuThu,MaPhieuThu,MaKhoanThu,MaHocVien,MaCoSo),
        CONSTRAINT UQ_ChiTietPhieuThu_NguonTinDung UNIQUE (MaChiTietPhieuThu,MaKhoanThu,MaHocVien,MaCoSo),
        CHECK ((LoaiBienDong='PHAN_BO' AND MaChiTietNguon IS NULL) OR
               (LoaiBienDong='HOAN_TAC_PHAN_BO' AND MaChiTietNguon IS NOT NULL))
    );
END;
GO

PRINT N'Tao bang PhatSinhTinDungHocPhi';
IF OBJECT_ID(N'dbo.PhatSinhTinDungHocPhi', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[PhatSinhTinDungHocPhi] (
        MaPhatSinh INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaHocVien INT NOT NULL,
        MaCoSo INT NOT NULL,
        LoaiTinDung VARCHAR(20) NOT NULL CHECK (LoaiTinDung IN ('MAM_NON','TRUNG_TAM')),
        LoaiBienDong VARCHAR(25) NOT NULL CHECK (LoaiBienDong IN ('TAO','SU_DUNG','HOAN_TAC_TAO','HOAN_TAC_SU_DUNG')),
        MaKhoanThuNguon INT NULL,
        MaChiTietHoanTacNguon INT NULL,
        MaKhoanThuDich INT NULL,
        MaPhatSinhGoc INT NULL,
        SoTien DECIMAL(18,2) NOT NULL CHECK (SoTien>0),
        MaGiaoDichNghiepVu VARCHAR(64) NOT NULL UNIQUE,
        NgayPhatSinh DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
        MaTaiKhoan INT NOT NULL,
        LyDo NVARCHAR(500) NOT NULL,
        CHECK ((LoaiBienDong='TAO' AND MaKhoanThuNguon IS NOT NULL AND MaChiTietHoanTacNguon IS NOT NULL AND MaKhoanThuDich IS NULL)
            OR (LoaiBienDong='SU_DUNG' AND MaKhoanThuDich IS NOT NULL AND MaPhatSinhGoc IS NULL)
            OR (LoaiBienDong='HOAN_TAC_TAO' AND MaPhatSinhGoc IS NOT NULL)
            OR (LoaiBienDong='HOAN_TAC_SU_DUNG' AND MaPhatSinhGoc IS NOT NULL AND MaKhoanThuDich IS NOT NULL)),
        CONSTRAINT UQ_TinDung_MaHocVienCoSo UNIQUE (MaPhatSinh,MaHocVien,MaCoSo)
    );
END;
GO

PRINT N'Tao bang PhieuHoanTienAn';
IF OBJECT_ID(N'dbo.PhieuHoanTienAn', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[PhieuHoanTienAn] (
        MaPhieuHoan INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaHocVien INT NOT NULL,
        MaCoSo INT NOT NULL,
        SoTienHoan DECIMAL(18,2) NOT NULL CHECK (SoTienHoan>0),
        NgayHoan DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
        LyDo NVARCHAR(500) NOT NULL,
        MaTaiKhoan INT NOT NULL,
        TrangThai VARCHAR(15) NOT NULL DEFAULT 'XAC_NHAN' CHECK (TrangThai IN ('NHAP','XAC_NHAN','HUY')),
        MaGiaoDichNghiepVu VARCHAR(64) NOT NULL UNIQUE,
        CONSTRAINT UQ_PhieuHoanTienAn_HocVienCoSo UNIQUE (MaPhieuHoan,MaHocVien,MaCoSo)
    );
END;
GO

PRINT N'Tao bang BuTruChuyenLop';
IF OBJECT_ID(N'dbo.BuTruChuyenLop', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[BuTruChuyenLop] (
        MaBuTru INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaChuyenLop INT NOT NULL,
        MaKhoanThuNguon INT NOT NULL,
        MaKhoanThuDich INT NOT NULL,
        MaPhatSinhTinDung INT NULL,
        SoTienBuTru DECIMAL(18,2) NOT NULL CHECK (SoTienBuTru>0),
        NgayBuTru DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
        TrangThai VARCHAR(15) NOT NULL DEFAULT 'XAC_NHAN' CHECK (TrangThai IN ('NHAP','XAC_NHAN','HUY')),
        MaGiaoDichNghiepVu VARCHAR(64) NOT NULL UNIQUE,
        CHECK (MaKhoanThuNguon<>MaKhoanThuDich)
    );
END;
GO

PRINT N'Tao bang HangHoa';
IF OBJECT_ID(N'dbo.HangHoa', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[HangHoa] (
        MaHangHoa INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaCoSo INT NOT NULL,
        MaSanPham VARCHAR(40) NOT NULL,
        TenHangHoa NVARCHAR(200) NOT NULL,
        DonViTinh NVARCHAR(40) NOT NULL,
        DonGiaSi DECIMAL(18,2) NOT NULL DEFAULT (0) CHECK (DonGiaSi>=0),
        DonGiaXuatBan DECIMAL(18,2) NOT NULL DEFAULT (0) CHECK (DonGiaXuatBan>=0),
        TrangThai BIT NOT NULL DEFAULT (1),
        CONSTRAINT UQ_HangHoa_CoSoMa UNIQUE (MaCoSo,MaSanPham),
        CONSTRAINT UQ_HangHoa_HangCoSo UNIQUE (MaHangHoa,MaCoSo)
    );
END;
GO

PRINT N'Tao bang KyKho';
IF OBJECT_ID(N'dbo.KyKho', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[KyKho] (
        MaKyKho INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaCoSo INT NOT NULL,
        Nam SMALLINT NOT NULL CHECK (Nam BETWEEN 2000 AND 2100),
        Thang TINYINT NOT NULL CHECK (Thang BETWEEN 1 AND 12),
        TrangThai VARCHAR(15) NOT NULL DEFAULT 'MO' CHECK (TrangThai IN ('MO','CHOT')),
        NgayChot DATETIME2(0) NULL,
        MaTaiKhoanChot INT NULL,
        CONSTRAINT UQ_KyKho_CoSoThang UNIQUE (MaCoSo,Nam,Thang),
        CONSTRAINT UQ_KyKho_KyCoSo UNIQUE (MaKyKho,MaCoSo)
    );
END;
GO

PRINT N'Tao bang KiemKeKho';
IF OBJECT_ID(N'dbo.KiemKeKho', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[KiemKeKho] (
        MaKiemKe INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaKyKho INT NOT NULL,
        MaCoSo INT NOT NULL,
        NgayKiemKe DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
        MaTaiKhoan INT NOT NULL,
        TrangThai VARCHAR(15) NOT NULL DEFAULT 'NHAP' CHECK (TrangThai IN ('NHAP','XAC_NHAN','HUY')),
        MaGiaoDichNghiepVu VARCHAR(64) NOT NULL UNIQUE,
        GhiChu NVARCHAR(500) NULL,
        CONSTRAINT UQ_KiemKeKho_CoSo UNIQUE (MaKiemKe,MaCoSo)
    );
END;
GO

PRINT N'Tao bang ChiTietKiemKe';
IF OBJECT_ID(N'dbo.ChiTietKiemKe', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[ChiTietKiemKe] (
        MaChiTietKiemKe INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaKiemKe INT NOT NULL,
        MaCoSo INT NOT NULL,
        MaHangHoa INT NOT NULL,
        TonHeThong DECIMAL(18,3) NOT NULL CHECK (TonHeThong>=0),
        TonThucTe DECIMAL(18,3) NOT NULL CHECK (TonThucTe>=0),
        CONSTRAINT UQ_ChiTietKiemKe_Hang UNIQUE (MaKiemKe,MaHangHoa)
    );
END;
GO

PRINT N'Tao bang PhieuKho';
IF OBJECT_ID(N'dbo.PhieuKho', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[PhieuKho] (
        MaPhieuKho INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaCoSo INT NOT NULL,
        MaKyKho INT NOT NULL,
        MaKiemKe INT NULL,
        LoaiPhieu VARCHAR(25) NOT NULL CHECK (LoaiPhieu IN ('TON_DAU','NHAP','XUAT_HS','XUAT_CS','DIEU_CHINH')),
        NgayLap DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
        MaTaiKhoan INT NOT NULL,
        TrangThai VARCHAR(15) NOT NULL DEFAULT 'XAC_NHAN' CHECK (TrangThai IN ('NHAP','XAC_NHAN','HUY')),
        MaGiaoDichNghiepVu VARCHAR(64) NOT NULL UNIQUE,
        GhiChu NVARCHAR(500) NULL,
        CONSTRAINT UQ_PhieuKho_CoSo UNIQUE (MaPhieuKho,MaCoSo),
        CHECK ((LoaiPhieu='DIEU_CHINH' AND MaKiemKe IS NOT NULL) OR (LoaiPhieu<>'DIEU_CHINH' AND MaKiemKe IS NULL))
    );
END;
GO

PRINT N'Tao bang ChiTietPhieuKho';
IF OBJECT_ID(N'dbo.ChiTietPhieuKho', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[ChiTietPhieuKho] (
        MaChiTiet INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaPhieuKho INT NOT NULL,
        MaCoSo INT NOT NULL,
        MaHangHoa INT NOT NULL,
        SoLuong DECIMAL(18,3) NOT NULL CHECK (SoLuong<>0),
        DonGiaSiTaiThoiDiem DECIMAL(18,2) NOT NULL CHECK (DonGiaSiTaiThoiDiem>=0),
        DonGiaXuatBanTaiThoiDiem DECIMAL(18,2) NOT NULL CHECK (DonGiaXuatBanTaiThoiDiem>=0),
        GhiChu NVARCHAR(500) NULL,
        CONSTRAINT UQ_ChiTietPhieuKho_Hang UNIQUE (MaPhieuKho,MaHangHoa)
    );
END;
GO

PRINT N'Tao bang SoLieuChotKho';
IF OBJECT_ID(N'dbo.SoLieuChotKho', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[SoLieuChotKho] (
        MaSoLieu INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MaKyKho INT NOT NULL,
        MaCoSo INT NOT NULL,
        MaHangHoa INT NOT NULL,
        MaSanPham VARCHAR(40) NOT NULL,
        TenHangHoa NVARCHAR(200) NOT NULL,
        DonViTinh NVARCHAR(40) NOT NULL,
        DonGiaSi DECIMAL(18,2) NOT NULL CHECK (DonGiaSi>=0),
        DonGiaXuatBan DECIMAL(18,2) NOT NULL CHECK (DonGiaXuatBan>=0),
        TonDau DECIMAL(18,3) NOT NULL CHECK (TonDau>=0),
        Nhap DECIMAL(18,3) NOT NULL CHECK (Nhap>=0),
        XuatHS DECIMAL(18,3) NOT NULL CHECK (XuatHS>=0),
        XuatCS DECIMAL(18,3) NOT NULL CHECK (XuatCS>=0),
        DieuChinhKiemKe DECIMAL(18,3) NOT NULL DEFAULT (0),
        TonCuoi DECIMAL(18,3) NOT NULL CHECK (TonCuoi>=0),
        GhiChu NVARCHAR(500) NULL,
        CONSTRAINT UQ_SoLieuChotKho UNIQUE (MaKyKho,MaHangHoa)
    );
END;
GO

-- Additional candidate keys required by composite FKs
IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE name=N'UQ_MonHoc_MonCoSo' AND parent_object_id=OBJECT_ID(N'dbo.MonHoc'))
    ALTER TABLE dbo.MonHoc ADD CONSTRAINT UQ_MonHoc_MonCoSo UNIQUE(MaMonHoc,MaCoSo);
GO
-- <<< 01_TAO_BANG.sql

-- >>> 02_RANG_BUOC_CHI_MUC.sql
USE [VINABACUS];
GO
-- Composite and regular foreign keys. All tables must exist first.
-- Bo sung quan he cho goi nam va khoi phuc mat khau.
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_YeuCauKhoiPhuc_TaiKhoan')
    ALTER TABLE dbo.YeuCauKhoiPhucMatKhau WITH CHECK ADD CONSTRAINT FK_YeuCauKhoiPhuc_TaiKhoan
        FOREIGN KEY(MaTaiKhoan) REFERENCES dbo.TaiKhoan(MaTaiKhoan);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_DangKyGoiNam_HocVien')
    ALTER TABLE dbo.DangKyGoiNam WITH CHECK ADD CONSTRAINT FK_DangKyGoiNam_HocVien
        FOREIGN KEY(MaHocVien) REFERENCES dbo.HocVien(MaHocVien);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_DangKyGoiNam_CoSo')
    ALTER TABLE dbo.DangKyGoiNam WITH CHECK ADD CONSTRAINT FK_DangKyGoiNam_CoSo
        FOREIGN KEY(MaCoSo) REFERENCES dbo.CoSo(MaCoSo);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_DangKyGoiNam_YeuCau')
    ALTER TABLE dbo.DangKyGoiNam WITH CHECK ADD CONSTRAINT FK_DangKyGoiNam_YeuCau
        FOREIGN KEY(MaYeuCau,MaHocVien,MaCoSo)
        REFERENCES dbo.YeuCauXepLop(MaYeuCau,MaHocVien,MaCoSo);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_DangKyGoiNam_ChuongTrinh')
    ALTER TABLE dbo.DangKyGoiNam WITH CHECK ADD CONSTRAINT FK_DangKyGoiNam_ChuongTrinh
        FOREIGN KEY(MaChuongTrinh,MaCoSo)
        REFERENCES dbo.ChuongTrinhGiamGia(MaChuongTrinh,MaCoSo);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_DangKyGoiNam_NguoiTao')
    ALTER TABLE dbo.DangKyGoiNam WITH CHECK ADD CONSTRAINT FK_DangKyGoiNam_NguoiTao
        FOREIGN KEY(MaTaiKhoanTao,MaCoSo)
        REFERENCES dbo.TaiKhoanCoSo(MaTaiKhoan,MaCoSo);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_DangKyKhoaHoc_GoiNam')
    ALTER TABLE dbo.DangKyKhoaHoc WITH CHECK ADD CONSTRAINT FK_DangKyKhoaHoc_GoiNam
        FOREIGN KEY(MaDangKyGoiNam,MaHocVien,MaCoSo)
        REFERENCES dbo.DangKyGoiNam(MaDangKyGoiNam,MaHocVien,MaCoSo);
GO
-- Chi mot nguoi co mot Email dang nhap; collation ten/email khong phan biet hoa thuong.
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'UX_NhanVien_Email' AND object_id=OBJECT_ID(N'dbo.NhanVien'))
    CREATE UNIQUE INDEX UX_NhanVien_Email ON dbo.NhanVien(Email) WHERE Email IS NOT NULL;
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'UX_DangKyGoiNam_YeuCau' AND object_id=OBJECT_ID(N'dbo.DangKyGoiNam'))
    CREATE UNIQUE INDEX UX_DangKyGoiNam_YeuCau ON dbo.DangKyGoiNam(MaYeuCau) WHERE MaYeuCau IS NOT NULL;
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'UX_DangKyKhoaHoc_GoiNamThuTu' AND object_id=OBJECT_ID(N'dbo.DangKyKhoaHoc'))
    CREATE UNIQUE INDEX UX_DangKyKhoaHoc_GoiNamThuTu ON dbo.DangKyKhoaHoc(MaDangKyGoiNam,SoThuTuTrongGoi)
       WHERE MaDangKyGoiNam IS NOT NULL;
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'IX_DangKyGoiNam_HocVien' AND object_id=OBJECT_ID(N'dbo.DangKyGoiNam'))
    CREATE INDEX IX_DangKyGoiNam_HocVien ON dbo.DangKyGoiNam(MaCoSo,MaHocVien,TrangThai,NgayDangKy);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'IX_YeuCauKhoiPhuc_TaiKhoan' AND object_id=OBJECT_ID(N'dbo.YeuCauKhoiPhucMatKhau'))
    CREATE INDEX IX_YeuCauKhoiPhuc_TaiKhoan ON dbo.YeuCauKhoiPhucMatKhau(MaTaiKhoan,TrangThai,NgayHetHan);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'UX_YeuCauKhoiPhuc_Phien' AND object_id=OBJECT_ID(N'dbo.YeuCauKhoiPhucMatKhau'))
    CREATE UNIQUE INDEX UX_YeuCauKhoiPhuc_Phien ON dbo.YeuCauKhoiPhucMatKhau(MaPhienDatLai)
       WHERE MaPhienDatLai IS NOT NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_NhanVienCoSo_MaNhanVien' AND parent_object_id=OBJECT_ID(N'dbo.NhanVienCoSo'))
    ALTER TABLE dbo.[NhanVienCoSo] WITH CHECK ADD CONSTRAINT [FK_NhanVienCoSo_MaNhanVien] FOREIGN KEY ([MaNhanVien]) REFERENCES dbo.[NhanVien] ([MaNhanVien]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_NhanVienCoSo_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.NhanVienCoSo'))
    ALTER TABLE dbo.[NhanVienCoSo] WITH CHECK ADD CONSTRAINT [FK_NhanVienCoSo_MaCoSo] FOREIGN KEY ([MaCoSo]) REFERENCES dbo.[CoSo] ([MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_TaiKhoan_MaNhanVien' AND parent_object_id=OBJECT_ID(N'dbo.TaiKhoan'))
    ALTER TABLE dbo.[TaiKhoan] WITH CHECK ADD CONSTRAINT [FK_TaiKhoan_MaNhanVien] FOREIGN KEY ([MaNhanVien]) REFERENCES dbo.[NhanVien] ([MaNhanVien]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_TaiKhoanCoSo_MaTaiKhoan_MaNhanVien' AND parent_object_id=OBJECT_ID(N'dbo.TaiKhoanCoSo'))
    ALTER TABLE dbo.[TaiKhoanCoSo] WITH CHECK ADD CONSTRAINT [FK_TaiKhoanCoSo_MaTaiKhoan_MaNhanVien] FOREIGN KEY ([MaTaiKhoan],[MaNhanVien]) REFERENCES dbo.[TaiKhoan] ([MaTaiKhoan],[MaNhanVien]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_TaiKhoanCoSo_MaNhanVien_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.TaiKhoanCoSo'))
    ALTER TABLE dbo.[TaiKhoanCoSo] WITH CHECK ADD CONSTRAINT [FK_TaiKhoanCoSo_MaNhanVien_MaCoSo] FOREIGN KEY ([MaNhanVien],[MaCoSo]) REFERENCES dbo.[NhanVienCoSo] ([MaNhanVien],[MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_TaiKhoanCoSo_MaVaiTro' AND parent_object_id=OBJECT_ID(N'dbo.TaiKhoanCoSo'))
    ALTER TABLE dbo.[TaiKhoanCoSo] WITH CHECK ADD CONSTRAINT [FK_TaiKhoanCoSo_MaVaiTro] FOREIGN KEY ([MaVaiTro]) REFERENCES dbo.[VaiTro] ([MaVaiTro]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_VaiTroQuyen_MaVaiTro' AND parent_object_id=OBJECT_ID(N'dbo.VaiTroQuyen'))
    ALTER TABLE dbo.[VaiTroQuyen] WITH CHECK ADD CONSTRAINT [FK_VaiTroQuyen_MaVaiTro] FOREIGN KEY ([MaVaiTro]) REFERENCES dbo.[VaiTro] ([MaVaiTro]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_VaiTroQuyen_MaQuyen' AND parent_object_id=OBJECT_ID(N'dbo.VaiTroQuyen'))
    ALTER TABLE dbo.[VaiTroQuyen] WITH CHECK ADD CONSTRAINT [FK_VaiTroQuyen_MaQuyen] FOREIGN KEY ([MaQuyen]) REFERENCES dbo.[Quyen] ([MaQuyen]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_PhanQuyenTaiKhoan_MaTaiKhoan_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.PhanQuyenTaiKhoan'))
    ALTER TABLE dbo.[PhanQuyenTaiKhoan] WITH CHECK ADD CONSTRAINT [FK_PhanQuyenTaiKhoan_MaTaiKhoan_MaCoSo] FOREIGN KEY ([MaTaiKhoan],[MaCoSo]) REFERENCES dbo.[TaiKhoanCoSo] ([MaTaiKhoan],[MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_PhanQuyenTaiKhoan_MaQuyen' AND parent_object_id=OBJECT_ID(N'dbo.PhanQuyenTaiKhoan'))
    ALTER TABLE dbo.[PhanQuyenTaiKhoan] WITH CHECK ADD CONSTRAINT [FK_PhanQuyenTaiKhoan_MaQuyen] FOREIGN KEY ([MaQuyen]) REFERENCES dbo.[Quyen] ([MaQuyen]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_MonHoc_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.MonHoc'))
    ALTER TABLE dbo.[MonHoc] WITH CHECK ADD CONSTRAINT [FK_MonHoc_MaCoSo] FOREIGN KEY ([MaCoSo]) REFERENCES dbo.[CoSo] ([MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_KhoaHoc_MaMonHoc_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.KhoaHoc'))
    ALTER TABLE dbo.[KhoaHoc] WITH CHECK ADD CONSTRAINT [FK_KhoaHoc_MaMonHoc_MaCoSo] FOREIGN KEY ([MaMonHoc],[MaCoSo]) REFERENCES dbo.[MonHoc] ([MaMonHoc],[MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_KhoaHoc_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.KhoaHoc'))
    ALTER TABLE dbo.[KhoaHoc] WITH CHECK ADD CONSTRAINT [FK_KhoaHoc_MaCoSo] FOREIGN KEY ([MaCoSo]) REFERENCES dbo.[CoSo] ([MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_LopTrungTam_MaKhoaHoc_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.LopTrungTam'))
    ALTER TABLE dbo.[LopTrungTam] WITH CHECK ADD CONSTRAINT [FK_LopTrungTam_MaKhoaHoc_MaCoSo] FOREIGN KEY ([MaKhoaHoc],[MaCoSo]) REFERENCES dbo.[KhoaHoc] ([MaKhoaHoc],[MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_LopTrungTam_MaGiaoVien_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.LopTrungTam'))
    ALTER TABLE dbo.[LopTrungTam] WITH CHECK ADD CONSTRAINT [FK_LopTrungTam_MaGiaoVien_MaCoSo] FOREIGN KEY ([MaGiaoVien],[MaCoSo]) REFERENCES dbo.[NhanVienCoSo] ([MaNhanVien],[MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_LichHocLop_MaLop' AND parent_object_id=OBJECT_ID(N'dbo.LichHocLop'))
    ALTER TABLE dbo.[LichHocLop] WITH CHECK ADD CONSTRAINT [FK_LichHocLop_MaLop] FOREIGN KEY ([MaLop]) REFERENCES dbo.[LopTrungTam] ([MaLop]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_BuoiHoc_MaLop' AND parent_object_id=OBJECT_ID(N'dbo.BuoiHoc'))
    ALTER TABLE dbo.[BuoiHoc] WITH CHECK ADD CONSTRAINT [FK_BuoiHoc_MaLop] FOREIGN KEY ([MaLop]) REFERENCES dbo.[LopTrungTam] ([MaLop]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_BuoiHoc_MaLichHoc_MaLop' AND parent_object_id=OBJECT_ID(N'dbo.BuoiHoc'))
    ALTER TABLE dbo.[BuoiHoc] WITH CHECK ADD CONSTRAINT [FK_BuoiHoc_MaLichHoc_MaLop] FOREIGN KEY ([MaLichHoc],[MaLop]) REFERENCES dbo.[LichHocLop] ([MaLichHoc],[MaLop]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_ChuongTrinhGiamGia_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.ChuongTrinhGiamGia'))
    ALTER TABLE dbo.[ChuongTrinhGiamGia] WITH CHECK ADD CONSTRAINT [FK_ChuongTrinhGiamGia_MaCoSo] FOREIGN KEY ([MaCoSo]) REFERENCES dbo.[CoSo] ([MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_GiamGiaKhoaHoc_MaChuongTrinh_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.GiamGiaKhoaHoc'))
    ALTER TABLE dbo.[GiamGiaKhoaHoc] WITH CHECK ADD CONSTRAINT [FK_GiamGiaKhoaHoc_MaChuongTrinh_MaCoSo] FOREIGN KEY ([MaChuongTrinh],[MaCoSo]) REFERENCES dbo.[ChuongTrinhGiamGia] ([MaChuongTrinh],[MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_GiamGiaKhoaHoc_MaKhoaHoc_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.GiamGiaKhoaHoc'))
    ALTER TABLE dbo.[GiamGiaKhoaHoc] WITH CHECK ADD CONSTRAINT [FK_GiamGiaKhoaHoc_MaKhoaHoc_MaCoSo] FOREIGN KEY ([MaKhoaHoc],[MaCoSo]) REFERENCES dbo.[KhoaHoc] ([MaKhoaHoc],[MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_DangKyKhoaHoc_MaHocVien' AND parent_object_id=OBJECT_ID(N'dbo.DangKyKhoaHoc'))
    ALTER TABLE dbo.[DangKyKhoaHoc] WITH CHECK ADD CONSTRAINT [FK_DangKyKhoaHoc_MaHocVien] FOREIGN KEY ([MaHocVien]) REFERENCES dbo.[HocVien] ([MaHocVien]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_DangKyKhoaHoc_MaKhoaHoc_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.DangKyKhoaHoc'))
    ALTER TABLE dbo.[DangKyKhoaHoc] WITH CHECK ADD CONSTRAINT [FK_DangKyKhoaHoc_MaKhoaHoc_MaCoSo] FOREIGN KEY ([MaKhoaHoc],[MaCoSo]) REFERENCES dbo.[KhoaHoc] ([MaKhoaHoc],[MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_DangKyKhoaHoc_MaYeuCau_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.DangKyKhoaHoc'))
    ALTER TABLE dbo.[DangKyKhoaHoc] WITH CHECK ADD CONSTRAINT [FK_DangKyKhoaHoc_MaYeuCau_MaCoSo] FOREIGN KEY ([MaYeuCau],[MaCoSo]) REFERENCES dbo.[YeuCauXepLop] ([MaYeuCau],[MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_DangKyKhoaHoc_MaChuongTrinh_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.DangKyKhoaHoc'))
    ALTER TABLE dbo.[DangKyKhoaHoc] WITH CHECK ADD CONSTRAINT [FK_DangKyKhoaHoc_MaChuongTrinh_MaCoSo] FOREIGN KEY ([MaChuongTrinh],[MaCoSo]) REFERENCES dbo.[ChuongTrinhGiamGia] ([MaChuongTrinh],[MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_XepLopTrungTam_MaDangKy_MaKhoaHoc_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.XepLopTrungTam'))
    ALTER TABLE dbo.[XepLopTrungTam] WITH CHECK ADD CONSTRAINT [FK_XepLopTrungTam_MaDangKy_MaKhoaHoc_MaCoSo] FOREIGN KEY ([MaDangKy],[MaKhoaHoc],[MaCoSo]) REFERENCES dbo.[DangKyKhoaHoc] ([MaDangKy],[MaKhoaHoc],[MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_XepLopTrungTam_MaLop_MaKhoaHoc_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.XepLopTrungTam'))
    ALTER TABLE dbo.[XepLopTrungTam] WITH CHECK ADD CONSTRAINT [FK_XepLopTrungTam_MaLop_MaKhoaHoc_MaCoSo] FOREIGN KEY ([MaLop],[MaKhoaHoc],[MaCoSo]) REFERENCES dbo.[LopTrungTam] ([MaLop],[MaKhoaHoc],[MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_SapXepBuoiHoc_MaDangKy_MaHocVien' AND parent_object_id=OBJECT_ID(N'dbo.SapXepBuoiHoc'))
    ALTER TABLE dbo.[SapXepBuoiHoc] WITH CHECK ADD CONSTRAINT [FK_SapXepBuoiHoc_MaDangKy_MaHocVien] FOREIGN KEY ([MaDangKy],[MaHocVien]) REFERENCES dbo.[DangKyKhoaHoc] ([MaDangKy],[MaHocVien]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_SapXepBuoiHoc_MaBuoiHoc' AND parent_object_id=OBJECT_ID(N'dbo.SapXepBuoiHoc'))
    ALTER TABLE dbo.[SapXepBuoiHoc] WITH CHECK ADD CONSTRAINT [FK_SapXepBuoiHoc_MaBuoiHoc] FOREIGN KEY ([MaBuoiHoc]) REFERENCES dbo.[BuoiHoc] ([MaBuoiHoc]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_SapXepBuoiHoc_MaSapXepNguon' AND parent_object_id=OBJECT_ID(N'dbo.SapXepBuoiHoc'))
    ALTER TABLE dbo.[SapXepBuoiHoc] WITH CHECK ADD CONSTRAINT [FK_SapXepBuoiHoc_MaSapXepNguon] FOREIGN KEY ([MaSapXepNguon]) REFERENCES dbo.[SapXepBuoiHoc] ([MaSapXep]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_DiemDanhTrungTam_MaSapXep' AND parent_object_id=OBJECT_ID(N'dbo.DiemDanhTrungTam'))
    ALTER TABLE dbo.[DiemDanhTrungTam] WITH CHECK ADD CONSTRAINT [FK_DiemDanhTrungTam_MaSapXep] FOREIGN KEY ([MaSapXep]) REFERENCES dbo.[SapXepBuoiHoc] ([MaSapXep]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_DiemDanhTrungTam_MaTaiKhoan' AND parent_object_id=OBJECT_ID(N'dbo.DiemDanhTrungTam'))
    ALTER TABLE dbo.[DiemDanhTrungTam] WITH CHECK ADD CONSTRAINT [FK_DiemDanhTrungTam_MaTaiKhoan] FOREIGN KEY ([MaTaiKhoan]) REFERENCES dbo.[TaiKhoan] ([MaTaiKhoan]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_ChuyenLopTrungTam_MaDangKyCu' AND parent_object_id=OBJECT_ID(N'dbo.ChuyenLopTrungTam'))
    ALTER TABLE dbo.[ChuyenLopTrungTam] WITH CHECK ADD CONSTRAINT [FK_ChuyenLopTrungTam_MaDangKyCu] FOREIGN KEY ([MaDangKyCu]) REFERENCES dbo.[DangKyKhoaHoc] ([MaDangKy]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_ChuyenLopTrungTam_MaDangKyDich' AND parent_object_id=OBJECT_ID(N'dbo.ChuyenLopTrungTam'))
    ALTER TABLE dbo.[ChuyenLopTrungTam] WITH CHECK ADD CONSTRAINT [FK_ChuyenLopTrungTam_MaDangKyDich] FOREIGN KEY ([MaDangKyDich]) REFERENCES dbo.[DangKyKhoaHoc] ([MaDangKy]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_ChuyenLopTrungTam_MaDangKyMoi' AND parent_object_id=OBJECT_ID(N'dbo.ChuyenLopTrungTam'))
    ALTER TABLE dbo.[ChuyenLopTrungTam] WITH CHECK ADD CONSTRAINT [FK_ChuyenLopTrungTam_MaDangKyMoi] FOREIGN KEY ([MaDangKyMoi]) REFERENCES dbo.[DangKyKhoaHoc] ([MaDangKy]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_ChuyenLopTrungTam_MaXepLopCu_MaDangKyCu' AND parent_object_id=OBJECT_ID(N'dbo.ChuyenLopTrungTam'))
    ALTER TABLE dbo.[ChuyenLopTrungTam] WITH CHECK ADD CONSTRAINT [FK_ChuyenLopTrungTam_MaXepLopCu_MaDangKyCu] FOREIGN KEY ([MaXepLopCu],[MaDangKyCu]) REFERENCES dbo.[XepLopTrungTam] ([MaXepLop],[MaDangKy]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_ChuyenLopTrungTam_MaXepLopMoi_MaDangKyDich' AND parent_object_id=OBJECT_ID(N'dbo.ChuyenLopTrungTam'))
    ALTER TABLE dbo.[ChuyenLopTrungTam] WITH CHECK ADD CONSTRAINT [FK_ChuyenLopTrungTam_MaXepLopMoi_MaDangKyDich] FOREIGN KEY ([MaXepLopMoi],[MaDangKyDich]) REFERENCES dbo.[XepLopTrungTam] ([MaXepLop],[MaDangKy]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_ChuyenLopTrungTam_MaTaiKhoan' AND parent_object_id=OBJECT_ID(N'dbo.ChuyenLopTrungTam'))
    ALTER TABLE dbo.[ChuyenLopTrungTam] WITH CHECK ADD CONSTRAINT [FK_ChuyenLopTrungTam_MaTaiKhoan] FOREIGN KEY ([MaTaiKhoan]) REFERENCES dbo.[TaiKhoan] ([MaTaiKhoan]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_NamHoc_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.NamHoc'))
    ALTER TABLE dbo.[NamHoc] WITH CHECK ADD CONSTRAINT [FK_NamHoc_MaCoSo] FOREIGN KEY ([MaCoSo]) REFERENCES dbo.[CoSo] ([MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_LopMamNon_MaNamHoc_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.LopMamNon'))
    ALTER TABLE dbo.[LopMamNon] WITH CHECK ADD CONSTRAINT [FK_LopMamNon_MaNamHoc_MaCoSo] FOREIGN KEY ([MaNamHoc],[MaCoSo]) REFERENCES dbo.[NamHoc] ([MaNamHoc],[MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_PhanCongGiaoVienMamNon_MaLop_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.PhanCongGiaoVienMamNon'))
    ALTER TABLE dbo.[PhanCongGiaoVienMamNon] WITH CHECK ADD CONSTRAINT [FK_PhanCongGiaoVienMamNon_MaLop_MaCoSo] FOREIGN KEY ([MaLop],[MaCoSo]) REFERENCES dbo.[LopMamNon] ([MaLop],[MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_PhanCongGiaoVienMamNon_MaNhanVien_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.PhanCongGiaoVienMamNon'))
    ALTER TABLE dbo.[PhanCongGiaoVienMamNon] WITH CHECK ADD CONSTRAINT [FK_PhanCongGiaoVienMamNon_MaNhanVien_MaCoSo] FOREIGN KEY ([MaNhanVien],[MaCoSo]) REFERENCES dbo.[NhanVienCoSo] ([MaNhanVien],[MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_XepLopMamNon_MaHocVien' AND parent_object_id=OBJECT_ID(N'dbo.XepLopMamNon'))
    ALTER TABLE dbo.[XepLopMamNon] WITH CHECK ADD CONSTRAINT [FK_XepLopMamNon_MaHocVien] FOREIGN KEY ([MaHocVien]) REFERENCES dbo.[HocVien] ([MaHocVien]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_XepLopMamNon_MaLop_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.XepLopMamNon'))
    ALTER TABLE dbo.[XepLopMamNon] WITH CHECK ADD CONSTRAINT [FK_XepLopMamNon_MaLop_MaCoSo] FOREIGN KEY ([MaLop],[MaCoSo]) REFERENCES dbo.[LopMamNon] ([MaLop],[MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_XepLopMamNon_MaYeuCau_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.XepLopMamNon'))
    ALTER TABLE dbo.[XepLopMamNon] WITH CHECK ADD CONSTRAINT [FK_XepLopMamNon_MaYeuCau_MaCoSo] FOREIGN KEY ([MaYeuCau],[MaCoSo]) REFERENCES dbo.[YeuCauXepLop] ([MaYeuCau],[MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_NgayNghiChung_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.NgayNghiChung'))
    ALTER TABLE dbo.[NgayNghiChung] WITH CHECK ADD CONSTRAINT [FK_NgayNghiChung_MaCoSo] FOREIGN KEY ([MaCoSo]) REFERENCES dbo.[CoSo] ([MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_ThangMamNon_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.ThangMamNon'))
    ALTER TABLE dbo.[ThangMamNon] WITH CHECK ADD CONSTRAINT [FK_ThangMamNon_MaCoSo] FOREIGN KEY ([MaCoSo]) REFERENCES dbo.[CoSo] ([MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_ThangMamNon_MaNguoiChot' AND parent_object_id=OBJECT_ID(N'dbo.ThangMamNon'))
    ALTER TABLE dbo.[ThangMamNon] WITH CHECK ADD CONSTRAINT [FK_ThangMamNon_MaNguoiChot] FOREIGN KEY ([MaNguoiChot]) REFERENCES dbo.[TaiKhoan] ([MaTaiKhoan]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_DiemDanhMamNon_MaXepLop_MaHocVien_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.DiemDanhMamNon'))
    ALTER TABLE dbo.[DiemDanhMamNon] WITH CHECK ADD CONSTRAINT [FK_DiemDanhMamNon_MaXepLop_MaHocVien_MaCoSo] FOREIGN KEY ([MaXepLop],[MaHocVien],[MaCoSo]) REFERENCES dbo.[XepLopMamNon] ([MaXepLop],[MaHocVien],[MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_DiemDanhMamNon_MaTaiKhoanDiemDanh_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.DiemDanhMamNon'))
    ALTER TABLE dbo.[DiemDanhMamNon] WITH CHECK ADD CONSTRAINT [FK_DiemDanhMamNon_MaTaiKhoanDiemDanh_MaCoSo] FOREIGN KEY ([MaTaiKhoanDiemDanh],[MaCoSo]) REFERENCES dbo.[TaiKhoanCoSo] ([MaTaiKhoan],[MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_LichSuSuaDiemDanhMamNon_MaDiemDanh' AND parent_object_id=OBJECT_ID(N'dbo.LichSuSuaDiemDanhMamNon'))
    ALTER TABLE dbo.[LichSuSuaDiemDanhMamNon] WITH CHECK ADD CONSTRAINT [FK_LichSuSuaDiemDanhMamNon_MaDiemDanh] FOREIGN KEY ([MaDiemDanh]) REFERENCES dbo.[DiemDanhMamNon] ([MaDiemDanh]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_LichSuSuaDiemDanhMamNon_MaTaiKhoan' AND parent_object_id=OBJECT_ID(N'dbo.LichSuSuaDiemDanhMamNon'))
    ALTER TABLE dbo.[LichSuSuaDiemDanhMamNon] WITH CHECK ADD CONSTRAINT [FK_LichSuSuaDiemDanhMamNon_MaTaiKhoan] FOREIGN KEY ([MaTaiKhoan]) REFERENCES dbo.[TaiKhoan] ([MaTaiKhoan]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_MucHocPhiLop_MaLop' AND parent_object_id=OBJECT_ID(N'dbo.MucHocPhiLop'))
    ALTER TABLE dbo.[MucHocPhiLop] WITH CHECK ADD CONSTRAINT [FK_MucHocPhiLop_MaLop] FOREIGN KEY ([MaLop]) REFERENCES dbo.[LopMamNon] ([MaLop]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_MucHocPhiLop_MaTaiKhoanThietLap' AND parent_object_id=OBJECT_ID(N'dbo.MucHocPhiLop'))
    ALTER TABLE dbo.[MucHocPhiLop] WITH CHECK ADD CONSTRAINT [FK_MucHocPhiLop_MaTaiKhoanThietLap] FOREIGN KEY ([MaTaiKhoanThietLap]) REFERENCES dbo.[TaiKhoan] ([MaTaiKhoan]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_HocPhiRiengMamNon_MaHocVien' AND parent_object_id=OBJECT_ID(N'dbo.HocPhiRiengMamNon'))
    ALTER TABLE dbo.[HocPhiRiengMamNon] WITH CHECK ADD CONSTRAINT [FK_HocPhiRiengMamNon_MaHocVien] FOREIGN KEY ([MaHocVien]) REFERENCES dbo.[HocVien] ([MaHocVien]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_HocPhiRiengMamNon_MaLop' AND parent_object_id=OBJECT_ID(N'dbo.HocPhiRiengMamNon'))
    ALTER TABLE dbo.[HocPhiRiengMamNon] WITH CHECK ADD CONSTRAINT [FK_HocPhiRiengMamNon_MaLop] FOREIGN KEY ([MaLop]) REFERENCES dbo.[LopMamNon] ([MaLop]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_HocPhiRiengMamNon_MaTaiKhoanDuyet' AND parent_object_id=OBJECT_ID(N'dbo.HocPhiRiengMamNon'))
    ALTER TABLE dbo.[HocPhiRiengMamNon] WITH CHECK ADD CONSTRAINT [FK_HocPhiRiengMamNon_MaTaiKhoanDuyet] FOREIGN KEY ([MaTaiKhoanDuyet]) REFERENCES dbo.[TaiKhoan] ([MaTaiKhoan]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_DonGiaTienAn_MaLop' AND parent_object_id=OBJECT_ID(N'dbo.DonGiaTienAn'))
    ALTER TABLE dbo.[DonGiaTienAn] WITH CHECK ADD CONSTRAINT [FK_DonGiaTienAn_MaLop] FOREIGN KEY ([MaLop]) REFERENCES dbo.[LopMamNon] ([MaLop]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_DonGiaTienAn_MaTaiKhoanThietLap' AND parent_object_id=OBJECT_ID(N'dbo.DonGiaTienAn'))
    ALTER TABLE dbo.[DonGiaTienAn] WITH CHECK ADD CONSTRAINT [FK_DonGiaTienAn_MaTaiKhoanThietLap] FOREIGN KEY ([MaTaiKhoanThietLap]) REFERENCES dbo.[TaiKhoan] ([MaTaiKhoan]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_BangTinhPhiMamNon_MaHocVien' AND parent_object_id=OBJECT_ID(N'dbo.BangTinhPhiMamNon'))
    ALTER TABLE dbo.[BangTinhPhiMamNon] WITH CHECK ADD CONSTRAINT [FK_BangTinhPhiMamNon_MaHocVien] FOREIGN KEY ([MaHocVien]) REFERENCES dbo.[HocVien] ([MaHocVien]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_BangTinhPhiMamNon_MaThang_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.BangTinhPhiMamNon'))
    ALTER TABLE dbo.[BangTinhPhiMamNon] WITH CHECK ADD CONSTRAINT [FK_BangTinhPhiMamNon_MaThang_MaCoSo] FOREIGN KEY ([MaThang],[MaCoSo]) REFERENCES dbo.[ThangMamNon] ([MaThang],[MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_ChiTietHocPhiMamNon_MaBangTinh_MaHocVien_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.ChiTietHocPhiMamNon'))
    ALTER TABLE dbo.[ChiTietHocPhiMamNon] WITH CHECK ADD CONSTRAINT [FK_ChiTietHocPhiMamNon_MaBangTinh_MaHocVien_MaCoSo] FOREIGN KEY ([MaBangTinh],[MaHocVien],[MaCoSo]) REFERENCES dbo.[BangTinhPhiMamNon] ([MaBangTinh],[MaHocVien],[MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_ChiTietHocPhiMamNon_MaXepLop_MaHocVien_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.ChiTietHocPhiMamNon'))
    ALTER TABLE dbo.[ChiTietHocPhiMamNon] WITH CHECK ADD CONSTRAINT [FK_ChiTietHocPhiMamNon_MaXepLop_MaHocVien_MaCoSo] FOREIGN KEY ([MaXepLop],[MaHocVien],[MaCoSo]) REFERENCES dbo.[XepLopMamNon] ([MaXepLop],[MaHocVien],[MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_ChiTietTienAnMamNon_MaBangTinh_MaHocVien_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.ChiTietTienAnMamNon'))
    ALTER TABLE dbo.[ChiTietTienAnMamNon] WITH CHECK ADD CONSTRAINT [FK_ChiTietTienAnMamNon_MaBangTinh_MaHocVien_MaCoSo] FOREIGN KEY ([MaBangTinh],[MaHocVien],[MaCoSo]) REFERENCES dbo.[BangTinhPhiMamNon] ([MaBangTinh],[MaHocVien],[MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_ChiTietTienAnMamNon_MaXepLop_MaHocVien_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.ChiTietTienAnMamNon'))
    ALTER TABLE dbo.[ChiTietTienAnMamNon] WITH CHECK ADD CONSTRAINT [FK_ChiTietTienAnMamNon_MaXepLop_MaHocVien_MaCoSo] FOREIGN KEY ([MaXepLop],[MaHocVien],[MaCoSo]) REFERENCES dbo.[XepLopMamNon] ([MaXepLop],[MaHocVien],[MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_ChiTietTienAnMamNon_MaXepLop_MaLop' AND parent_object_id=OBJECT_ID(N'dbo.ChiTietTienAnMamNon'))
    ALTER TABLE dbo.[ChiTietTienAnMamNon] WITH CHECK ADD CONSTRAINT [FK_ChiTietTienAnMamNon_MaXepLop_MaLop] FOREIGN KEY ([MaXepLop],[MaLop]) REFERENCES dbo.[XepLopMamNon] ([MaXepLop],[MaLop]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_ChiTietTienAnMamNon_MaDonGiaTienAn_MaLop' AND parent_object_id=OBJECT_ID(N'dbo.ChiTietTienAnMamNon'))
    ALTER TABLE dbo.[ChiTietTienAnMamNon] WITH CHECK ADD CONSTRAINT [FK_ChiTietTienAnMamNon_MaDonGiaTienAn_MaLop] FOREIGN KEY ([MaDonGiaTienAn],[MaLop]) REFERENCES dbo.[DonGiaTienAn] ([MaDonGiaTienAn],[MaLop]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_KhachTiemNang_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.KhachTiemNang'))
    ALTER TABLE dbo.[KhachTiemNang] WITH CHECK ADD CONSTRAINT [FK_KhachTiemNang_MaCoSo] FOREIGN KEY ([MaCoSo]) REFERENCES dbo.[CoSo] ([MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_KhachTiemNang_MaNhanVienPhuTrach_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.KhachTiemNang'))
    ALTER TABLE dbo.[KhachTiemNang] WITH CHECK ADD CONSTRAINT [FK_KhachTiemNang_MaNhanVienPhuTrach_MaCoSo] FOREIGN KEY ([MaNhanVienPhuTrach],[MaCoSo]) REFERENCES dbo.[NhanVienCoSo] ([MaNhanVien],[MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_LichSuTuVan_MaKhach' AND parent_object_id=OBJECT_ID(N'dbo.LichSuTuVan'))
    ALTER TABLE dbo.[LichSuTuVan] WITH CHECK ADD CONSTRAINT [FK_LichSuTuVan_MaKhach] FOREIGN KEY ([MaKhach]) REFERENCES dbo.[KhachTiemNang] ([MaKhach]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_LichSuTuVan_MaTaiKhoan' AND parent_object_id=OBJECT_ID(N'dbo.LichSuTuVan'))
    ALTER TABLE dbo.[LichSuTuVan] WITH CHECK ADD CONSTRAINT [FK_LichSuTuVan_MaTaiKhoan] FOREIGN KEY ([MaTaiKhoan]) REFERENCES dbo.[TaiKhoan] ([MaTaiKhoan]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_YeuCauXepLop_MaKhach_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.YeuCauXepLop'))
    ALTER TABLE dbo.[YeuCauXepLop] WITH CHECK ADD CONSTRAINT [FK_YeuCauXepLop_MaKhach_MaCoSo] FOREIGN KEY ([MaKhach],[MaCoSo]) REFERENCES dbo.[KhachTiemNang] ([MaKhach],[MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_YeuCauXepLop_MaHocVien' AND parent_object_id=OBJECT_ID(N'dbo.YeuCauXepLop'))
    ALTER TABLE dbo.[YeuCauXepLop] WITH CHECK ADD CONSTRAINT [FK_YeuCauXepLop_MaHocVien] FOREIGN KEY ([MaHocVien]) REFERENCES dbo.[HocVien] ([MaHocVien]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_YeuCauXepLop_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.YeuCauXepLop'))
    ALTER TABLE dbo.[YeuCauXepLop] WITH CHECK ADD CONSTRAINT [FK_YeuCauXepLop_MaCoSo] FOREIGN KEY ([MaCoSo]) REFERENCES dbo.[CoSo] ([MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_YeuCauXepLop_MaKhoaHoc_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.YeuCauXepLop'))
    ALTER TABLE dbo.[YeuCauXepLop] WITH CHECK ADD CONSTRAINT [FK_YeuCauXepLop_MaKhoaHoc_MaCoSo] FOREIGN KEY ([MaKhoaHoc],[MaCoSo]) REFERENCES dbo.[KhoaHoc] ([MaKhoaHoc],[MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_KhoanPhaiThu_MaHocVien' AND parent_object_id=OBJECT_ID(N'dbo.KhoanPhaiThu'))
    ALTER TABLE dbo.[KhoanPhaiThu] WITH CHECK ADD CONSTRAINT [FK_KhoanPhaiThu_MaHocVien] FOREIGN KEY ([MaHocVien]) REFERENCES dbo.[HocVien] ([MaHocVien]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_KhoanPhaiThu_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.KhoanPhaiThu'))
    ALTER TABLE dbo.[KhoanPhaiThu] WITH CHECK ADD CONSTRAINT [FK_KhoanPhaiThu_MaCoSo] FOREIGN KEY ([MaCoSo]) REFERENCES dbo.[CoSo] ([MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_KhoanPhaiThu_MaDangKy_MaHocVien_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.KhoanPhaiThu'))
    ALTER TABLE dbo.[KhoanPhaiThu] WITH CHECK ADD CONSTRAINT [FK_KhoanPhaiThu_MaDangKy_MaHocVien_MaCoSo] FOREIGN KEY ([MaDangKy],[MaHocVien],[MaCoSo]) REFERENCES dbo.[DangKyKhoaHoc] ([MaDangKy],[MaHocVien],[MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_KhoanPhaiThu_MaBangTinh_MaHocVien_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.KhoanPhaiThu'))
    ALTER TABLE dbo.[KhoanPhaiThu] WITH CHECK ADD CONSTRAINT [FK_KhoanPhaiThu_MaBangTinh_MaHocVien_MaCoSo] FOREIGN KEY ([MaBangTinh],[MaHocVien],[MaCoSo]) REFERENCES dbo.[BangTinhPhiMamNon] ([MaBangTinh],[MaHocVien],[MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_DieuChinhKhoanThu_MaKhoanThu' AND parent_object_id=OBJECT_ID(N'dbo.DieuChinhKhoanThu'))
    ALTER TABLE dbo.[DieuChinhKhoanThu] WITH CHECK ADD CONSTRAINT [FK_DieuChinhKhoanThu_MaKhoanThu] FOREIGN KEY ([MaKhoanThu]) REFERENCES dbo.[KhoanPhaiThu] ([MaKhoanThu]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_DieuChinhKhoanThu_MaTaiKhoan' AND parent_object_id=OBJECT_ID(N'dbo.DieuChinhKhoanThu'))
    ALTER TABLE dbo.[DieuChinhKhoanThu] WITH CHECK ADD CONSTRAINT [FK_DieuChinhKhoanThu_MaTaiKhoan] FOREIGN KEY ([MaTaiKhoan]) REFERENCES dbo.[TaiKhoan] ([MaTaiKhoan]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_DieuChinhKhoanThu_MaThangNguon' AND parent_object_id=OBJECT_ID(N'dbo.DieuChinhKhoanThu'))
    ALTER TABLE dbo.[DieuChinhKhoanThu] WITH CHECK ADD CONSTRAINT [FK_DieuChinhKhoanThu_MaThangNguon] FOREIGN KEY ([MaThangNguon]) REFERENCES dbo.[ThangMamNon] ([MaThang]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_DieuChinhKhoanThu_MaThangGhiNhan' AND parent_object_id=OBJECT_ID(N'dbo.DieuChinhKhoanThu'))
    ALTER TABLE dbo.[DieuChinhKhoanThu] WITH CHECK ADD CONSTRAINT [FK_DieuChinhKhoanThu_MaThangGhiNhan] FOREIGN KEY ([MaThangGhiNhan]) REFERENCES dbo.[ThangMamNon] ([MaThang]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_PhieuThu_MaHocVien' AND parent_object_id=OBJECT_ID(N'dbo.PhieuThu'))
    ALTER TABLE dbo.[PhieuThu] WITH CHECK ADD CONSTRAINT [FK_PhieuThu_MaHocVien] FOREIGN KEY ([MaHocVien]) REFERENCES dbo.[HocVien] ([MaHocVien]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_PhieuThu_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.PhieuThu'))
    ALTER TABLE dbo.[PhieuThu] WITH CHECK ADD CONSTRAINT [FK_PhieuThu_MaCoSo] FOREIGN KEY ([MaCoSo]) REFERENCES dbo.[CoSo] ([MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_PhieuThu_MaTaiKhoan_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.PhieuThu'))
    ALTER TABLE dbo.[PhieuThu] WITH CHECK ADD CONSTRAINT [FK_PhieuThu_MaTaiKhoan_MaCoSo] FOREIGN KEY ([MaTaiKhoan],[MaCoSo]) REFERENCES dbo.[TaiKhoanCoSo] ([MaTaiKhoan],[MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_PhieuThu_MaTaiKhoanHuy' AND parent_object_id=OBJECT_ID(N'dbo.PhieuThu'))
    ALTER TABLE dbo.[PhieuThu] WITH CHECK ADD CONSTRAINT [FK_PhieuThu_MaTaiKhoanHuy] FOREIGN KEY ([MaTaiKhoanHuy]) REFERENCES dbo.[TaiKhoan] ([MaTaiKhoan]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_ChiTietPhieuThu_MaPhieuThu_MaHocVien_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.ChiTietPhieuThu'))
    ALTER TABLE dbo.[ChiTietPhieuThu] WITH CHECK ADD CONSTRAINT [FK_ChiTietPhieuThu_MaPhieuThu_MaHocVien_MaCoSo] FOREIGN KEY ([MaPhieuThu],[MaHocVien],[MaCoSo]) REFERENCES dbo.[PhieuThu] ([MaPhieuThu],[MaHocVien],[MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_ChiTietPhieuThu_MaKhoanThu_MaHocVien_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.ChiTietPhieuThu'))
    ALTER TABLE dbo.[ChiTietPhieuThu] WITH CHECK ADD CONSTRAINT [FK_ChiTietPhieuThu_MaKhoanThu_MaHocVien_MaCoSo] FOREIGN KEY ([MaKhoanThu],[MaHocVien],[MaCoSo]) REFERENCES dbo.[KhoanPhaiThu] ([MaKhoanThu],[MaHocVien],[MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_ChiTietPhieuThu_MaChiTietNguon' AND parent_object_id=OBJECT_ID(N'dbo.ChiTietPhieuThu'))
    ALTER TABLE dbo.[ChiTietPhieuThu] WITH CHECK ADD CONSTRAINT [FK_ChiTietPhieuThu_MaChiTietNguon] FOREIGN KEY ([MaChiTietNguon]) REFERENCES dbo.[ChiTietPhieuThu] ([MaChiTietPhieuThu]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_ChiTietPhieuThu_MaDieuChinh' AND parent_object_id=OBJECT_ID(N'dbo.ChiTietPhieuThu'))
    ALTER TABLE dbo.[ChiTietPhieuThu] WITH CHECK ADD CONSTRAINT [FK_ChiTietPhieuThu_MaDieuChinh] FOREIGN KEY ([MaDieuChinh]) REFERENCES dbo.[DieuChinhKhoanThu] ([MaDieuChinh]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_PhatSinhTinDungHocPhi_MaHocVien' AND parent_object_id=OBJECT_ID(N'dbo.PhatSinhTinDungHocPhi'))
    ALTER TABLE dbo.[PhatSinhTinDungHocPhi] WITH CHECK ADD CONSTRAINT [FK_PhatSinhTinDungHocPhi_MaHocVien] FOREIGN KEY ([MaHocVien]) REFERENCES dbo.[HocVien] ([MaHocVien]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_PhatSinhTinDungHocPhi_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.PhatSinhTinDungHocPhi'))
    ALTER TABLE dbo.[PhatSinhTinDungHocPhi] WITH CHECK ADD CONSTRAINT [FK_PhatSinhTinDungHocPhi_MaCoSo] FOREIGN KEY ([MaCoSo]) REFERENCES dbo.[CoSo] ([MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_PhatSinhTinDungHocPhi_MaKhoanThuNguon_MaHocVien_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.PhatSinhTinDungHocPhi'))
    ALTER TABLE dbo.[PhatSinhTinDungHocPhi] WITH CHECK ADD CONSTRAINT [FK_PhatSinhTinDungHocPhi_MaKhoanThuNguon_MaHocVien_MaCoSo] FOREIGN KEY ([MaKhoanThuNguon],[MaHocVien],[MaCoSo]) REFERENCES dbo.[KhoanPhaiThu] ([MaKhoanThu],[MaHocVien],[MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_PhatSinhTinDungHocPhi_MaKhoanThuDich_MaHocVien_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.PhatSinhTinDungHocPhi'))
    ALTER TABLE dbo.[PhatSinhTinDungHocPhi] WITH CHECK ADD CONSTRAINT [FK_PhatSinhTinDungHocPhi_MaKhoanThuDich_MaHocVien_MaCoSo] FOREIGN KEY ([MaKhoanThuDich],[MaHocVien],[MaCoSo]) REFERENCES dbo.[KhoanPhaiThu] ([MaKhoanThu],[MaHocVien],[MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_PhatSinhTinDungHocPhi_MaChiTietHoanTacNguon' AND parent_object_id=OBJECT_ID(N'dbo.PhatSinhTinDungHocPhi'))
    ALTER TABLE dbo.[PhatSinhTinDungHocPhi] WITH CHECK ADD CONSTRAINT [FK_PhatSinhTinDungHocPhi_MaChiTietHoanTacNguon] FOREIGN KEY ([MaChiTietHoanTacNguon]) REFERENCES dbo.[ChiTietPhieuThu] ([MaChiTietPhieuThu]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_PhatSinhTinDungHocPhi_MaPhatSinhGoc' AND parent_object_id=OBJECT_ID(N'dbo.PhatSinhTinDungHocPhi'))
    ALTER TABLE dbo.[PhatSinhTinDungHocPhi] WITH CHECK ADD CONSTRAINT [FK_PhatSinhTinDungHocPhi_MaPhatSinhGoc] FOREIGN KEY ([MaPhatSinhGoc]) REFERENCES dbo.[PhatSinhTinDungHocPhi] ([MaPhatSinh]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_PhatSinhTinDungHocPhi_MaTaiKhoan' AND parent_object_id=OBJECT_ID(N'dbo.PhatSinhTinDungHocPhi'))
    ALTER TABLE dbo.[PhatSinhTinDungHocPhi] WITH CHECK ADD CONSTRAINT [FK_PhatSinhTinDungHocPhi_MaTaiKhoan] FOREIGN KEY ([MaTaiKhoan]) REFERENCES dbo.[TaiKhoan] ([MaTaiKhoan]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_PhieuHoanTienAn_MaHocVien' AND parent_object_id=OBJECT_ID(N'dbo.PhieuHoanTienAn'))
    ALTER TABLE dbo.[PhieuHoanTienAn] WITH CHECK ADD CONSTRAINT [FK_PhieuHoanTienAn_MaHocVien] FOREIGN KEY ([MaHocVien]) REFERENCES dbo.[HocVien] ([MaHocVien]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_PhieuHoanTienAn_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.PhieuHoanTienAn'))
    ALTER TABLE dbo.[PhieuHoanTienAn] WITH CHECK ADD CONSTRAINT [FK_PhieuHoanTienAn_MaCoSo] FOREIGN KEY ([MaCoSo]) REFERENCES dbo.[CoSo] ([MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_PhieuHoanTienAn_MaTaiKhoan_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.PhieuHoanTienAn'))
    ALTER TABLE dbo.[PhieuHoanTienAn] WITH CHECK ADD CONSTRAINT [FK_PhieuHoanTienAn_MaTaiKhoan_MaCoSo] FOREIGN KEY ([MaTaiKhoan],[MaCoSo]) REFERENCES dbo.[TaiKhoanCoSo] ([MaTaiKhoan],[MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_BuTruChuyenLop_MaChuyenLop' AND parent_object_id=OBJECT_ID(N'dbo.BuTruChuyenLop'))
    ALTER TABLE dbo.[BuTruChuyenLop] WITH CHECK ADD CONSTRAINT [FK_BuTruChuyenLop_MaChuyenLop] FOREIGN KEY ([MaChuyenLop]) REFERENCES dbo.[ChuyenLopTrungTam] ([MaChuyenLop]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_BuTruChuyenLop_MaKhoanThuNguon' AND parent_object_id=OBJECT_ID(N'dbo.BuTruChuyenLop'))
    ALTER TABLE dbo.[BuTruChuyenLop] WITH CHECK ADD CONSTRAINT [FK_BuTruChuyenLop_MaKhoanThuNguon] FOREIGN KEY ([MaKhoanThuNguon]) REFERENCES dbo.[KhoanPhaiThu] ([MaKhoanThu]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_BuTruChuyenLop_MaKhoanThuDich' AND parent_object_id=OBJECT_ID(N'dbo.BuTruChuyenLop'))
    ALTER TABLE dbo.[BuTruChuyenLop] WITH CHECK ADD CONSTRAINT [FK_BuTruChuyenLop_MaKhoanThuDich] FOREIGN KEY ([MaKhoanThuDich]) REFERENCES dbo.[KhoanPhaiThu] ([MaKhoanThu]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_BuTruChuyenLop_MaPhatSinhTinDung' AND parent_object_id=OBJECT_ID(N'dbo.BuTruChuyenLop'))
    ALTER TABLE dbo.[BuTruChuyenLop] WITH CHECK ADD CONSTRAINT [FK_BuTruChuyenLop_MaPhatSinhTinDung] FOREIGN KEY ([MaPhatSinhTinDung]) REFERENCES dbo.[PhatSinhTinDungHocPhi] ([MaPhatSinh]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_PhatSinhTienAn_MaHocVien' AND parent_object_id=OBJECT_ID(N'dbo.PhatSinhTienAn'))
    ALTER TABLE dbo.[PhatSinhTienAn] WITH CHECK ADD CONSTRAINT [FK_PhatSinhTienAn_MaHocVien] FOREIGN KEY ([MaHocVien]) REFERENCES dbo.[HocVien] ([MaHocVien]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_PhatSinhTienAn_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.PhatSinhTienAn'))
    ALTER TABLE dbo.[PhatSinhTienAn] WITH CHECK ADD CONSTRAINT [FK_PhatSinhTienAn_MaCoSo] FOREIGN KEY ([MaCoSo]) REFERENCES dbo.[CoSo] ([MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_PhatSinhTienAn_MaDiemDanh_MaHocVien_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.PhatSinhTienAn'))
    ALTER TABLE dbo.[PhatSinhTienAn] WITH CHECK ADD CONSTRAINT [FK_PhatSinhTienAn_MaDiemDanh_MaHocVien_MaCoSo] FOREIGN KEY ([MaDiemDanh],[MaHocVien],[MaCoSo]) REFERENCES dbo.[DiemDanhMamNon] ([MaDiemDanh],[MaHocVien],[MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_PhatSinhTienAn_MaBangTinh_MaHocVien_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.PhatSinhTienAn'))
    ALTER TABLE dbo.[PhatSinhTienAn] WITH CHECK ADD CONSTRAINT [FK_PhatSinhTienAn_MaBangTinh_MaHocVien_MaCoSo] FOREIGN KEY ([MaBangTinh],[MaHocVien],[MaCoSo]) REFERENCES dbo.[BangTinhPhiMamNon] ([MaBangTinh],[MaHocVien],[MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_PhatSinhTienAn_MaKhoanThuDich_MaHocVien_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.PhatSinhTienAn'))
    ALTER TABLE dbo.[PhatSinhTienAn] WITH CHECK ADD CONSTRAINT [FK_PhatSinhTienAn_MaKhoanThuDich_MaHocVien_MaCoSo] FOREIGN KEY ([MaKhoanThuDich],[MaHocVien],[MaCoSo]) REFERENCES dbo.[KhoanPhaiThu] ([MaKhoanThu],[MaHocVien],[MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_PhatSinhTienAn_MaPhieuHoan_MaHocVien_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.PhatSinhTienAn'))
    ALTER TABLE dbo.[PhatSinhTienAn] WITH CHECK ADD CONSTRAINT [FK_PhatSinhTienAn_MaPhieuHoan_MaHocVien_MaCoSo] FOREIGN KEY ([MaPhieuHoan],[MaHocVien],[MaCoSo]) REFERENCES dbo.[PhieuHoanTienAn] ([MaPhieuHoan],[MaHocVien],[MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_PhatSinhTienAn_MaPhatSinhGoc' AND parent_object_id=OBJECT_ID(N'dbo.PhatSinhTienAn'))
    ALTER TABLE dbo.[PhatSinhTienAn] WITH CHECK ADD CONSTRAINT [FK_PhatSinhTienAn_MaPhatSinhGoc] FOREIGN KEY ([MaPhatSinhGoc]) REFERENCES dbo.[PhatSinhTienAn] ([MaPhatSinhTienAn]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_PhatSinhTienAn_MaTaiKhoan' AND parent_object_id=OBJECT_ID(N'dbo.PhatSinhTienAn'))
    ALTER TABLE dbo.[PhatSinhTienAn] WITH CHECK ADD CONSTRAINT [FK_PhatSinhTienAn_MaTaiKhoan] FOREIGN KEY ([MaTaiKhoan]) REFERENCES dbo.[TaiKhoan] ([MaTaiKhoan]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_HangHoa_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.HangHoa'))
    ALTER TABLE dbo.[HangHoa] WITH CHECK ADD CONSTRAINT [FK_HangHoa_MaCoSo] FOREIGN KEY ([MaCoSo]) REFERENCES dbo.[CoSo] ([MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_KyKho_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.KyKho'))
    ALTER TABLE dbo.[KyKho] WITH CHECK ADD CONSTRAINT [FK_KyKho_MaCoSo] FOREIGN KEY ([MaCoSo]) REFERENCES dbo.[CoSo] ([MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_KyKho_MaTaiKhoanChot' AND parent_object_id=OBJECT_ID(N'dbo.KyKho'))
    ALTER TABLE dbo.[KyKho] WITH CHECK ADD CONSTRAINT [FK_KyKho_MaTaiKhoanChot] FOREIGN KEY ([MaTaiKhoanChot]) REFERENCES dbo.[TaiKhoan] ([MaTaiKhoan]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_KiemKeKho_MaKyKho_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.KiemKeKho'))
    ALTER TABLE dbo.[KiemKeKho] WITH CHECK ADD CONSTRAINT [FK_KiemKeKho_MaKyKho_MaCoSo] FOREIGN KEY ([MaKyKho],[MaCoSo]) REFERENCES dbo.[KyKho] ([MaKyKho],[MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_KiemKeKho_MaTaiKhoan_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.KiemKeKho'))
    ALTER TABLE dbo.[KiemKeKho] WITH CHECK ADD CONSTRAINT [FK_KiemKeKho_MaTaiKhoan_MaCoSo] FOREIGN KEY ([MaTaiKhoan],[MaCoSo]) REFERENCES dbo.[TaiKhoanCoSo] ([MaTaiKhoan],[MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_ChiTietKiemKe_MaKiemKe_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.ChiTietKiemKe'))
    ALTER TABLE dbo.[ChiTietKiemKe] WITH CHECK ADD CONSTRAINT [FK_ChiTietKiemKe_MaKiemKe_MaCoSo] FOREIGN KEY ([MaKiemKe],[MaCoSo]) REFERENCES dbo.[KiemKeKho] ([MaKiemKe],[MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_ChiTietKiemKe_MaHangHoa_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.ChiTietKiemKe'))
    ALTER TABLE dbo.[ChiTietKiemKe] WITH CHECK ADD CONSTRAINT [FK_ChiTietKiemKe_MaHangHoa_MaCoSo] FOREIGN KEY ([MaHangHoa],[MaCoSo]) REFERENCES dbo.[HangHoa] ([MaHangHoa],[MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_PhieuKho_MaKyKho_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.PhieuKho'))
    ALTER TABLE dbo.[PhieuKho] WITH CHECK ADD CONSTRAINT [FK_PhieuKho_MaKyKho_MaCoSo] FOREIGN KEY ([MaKyKho],[MaCoSo]) REFERENCES dbo.[KyKho] ([MaKyKho],[MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_PhieuKho_MaKiemKe_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.PhieuKho'))
    ALTER TABLE dbo.[PhieuKho] WITH CHECK ADD CONSTRAINT [FK_PhieuKho_MaKiemKe_MaCoSo] FOREIGN KEY ([MaKiemKe],[MaCoSo]) REFERENCES dbo.[KiemKeKho] ([MaKiemKe],[MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_PhieuKho_MaTaiKhoan_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.PhieuKho'))
    ALTER TABLE dbo.[PhieuKho] WITH CHECK ADD CONSTRAINT [FK_PhieuKho_MaTaiKhoan_MaCoSo] FOREIGN KEY ([MaTaiKhoan],[MaCoSo]) REFERENCES dbo.[TaiKhoanCoSo] ([MaTaiKhoan],[MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_ChiTietPhieuKho_MaPhieuKho_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.ChiTietPhieuKho'))
    ALTER TABLE dbo.[ChiTietPhieuKho] WITH CHECK ADD CONSTRAINT [FK_ChiTietPhieuKho_MaPhieuKho_MaCoSo] FOREIGN KEY ([MaPhieuKho],[MaCoSo]) REFERENCES dbo.[PhieuKho] ([MaPhieuKho],[MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_ChiTietPhieuKho_MaHangHoa_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.ChiTietPhieuKho'))
    ALTER TABLE dbo.[ChiTietPhieuKho] WITH CHECK ADD CONSTRAINT [FK_ChiTietPhieuKho_MaHangHoa_MaCoSo] FOREIGN KEY ([MaHangHoa],[MaCoSo]) REFERENCES dbo.[HangHoa] ([MaHangHoa],[MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_SoLieuChotKho_MaKyKho_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.SoLieuChotKho'))
    ALTER TABLE dbo.[SoLieuChotKho] WITH CHECK ADD CONSTRAINT [FK_SoLieuChotKho_MaKyKho_MaCoSo] FOREIGN KEY ([MaKyKho],[MaCoSo]) REFERENCES dbo.[KyKho] ([MaKyKho],[MaCoSo]);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_SoLieuChotKho_MaHangHoa_MaCoSo' AND parent_object_id=OBJECT_ID(N'dbo.SoLieuChotKho'))
    ALTER TABLE dbo.[SoLieuChotKho] WITH CHECK ADD CONSTRAINT [FK_SoLieuChotKho_MaHangHoa_MaCoSo] FOREIGN KEY ([MaHangHoa],[MaCoSo]) REFERENCES dbo.[HangHoa] ([MaHangHoa],[MaCoSo]);
GO
-- Indexes and filtered unique indexes
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'UX_TaiKhoanCoSo_MacDinh' AND object_id=OBJECT_ID(N'dbo.TaiKhoanCoSo'))
    CREATE UNIQUE INDEX [UX_TaiKhoanCoSo_MacDinh] ON dbo.[TaiKhoanCoSo] (MaTaiKhoan) WHERE LaCoSoMacDinh=1 AND TrangThai=1;
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'UX_DangKyKhoaHoc_YeuCau' AND object_id=OBJECT_ID(N'dbo.DangKyKhoaHoc'))
    CREATE UNIQUE INDEX [UX_DangKyKhoaHoc_YeuCau] ON dbo.[DangKyKhoaHoc] (MaYeuCau) WHERE MaYeuCau IS NOT NULL;
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'UX_XepLopMamNon_YeuCau' AND object_id=OBJECT_ID(N'dbo.XepLopMamNon'))
    CREATE UNIQUE INDEX [UX_XepLopMamNon_YeuCau] ON dbo.[XepLopMamNon] (MaYeuCau) WHERE MaYeuCau IS NOT NULL;
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'UX_YeuCauXepLop_GiaoDich' AND object_id=OBJECT_ID(N'dbo.YeuCauXepLop'))
    CREATE UNIQUE INDEX [UX_YeuCauXepLop_GiaoDich] ON dbo.[YeuCauXepLop] (MaGiaoDichHoanTat) WHERE MaGiaoDichHoanTat IS NOT NULL;
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'UX_XepLopTrungTam_DangHieuLuc' AND object_id=OBJECT_ID(N'dbo.XepLopTrungTam'))
    CREATE UNIQUE INDEX [UX_XepLopTrungTam_DangHieuLuc] ON dbo.[XepLopTrungTam] (MaDangKy) WHERE TrangThai='HIEU_LUC' AND DenNgay IS NULL;
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'UX_XepLopMamNon_HocVienHieuLuc' AND object_id=OBJECT_ID(N'dbo.XepLopMamNon'))
    CREATE UNIQUE INDEX [UX_XepLopMamNon_HocVienHieuLuc] ON dbo.[XepLopMamNon] (MaHocVien,MaCoSo) WHERE TrangThai='HIEU_LUC' AND DenNgay IS NULL;
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'UX_SapXepBuoiHoc_HocVienBuoi' AND object_id=OBJECT_ID(N'dbo.SapXepBuoiHoc'))
    CREATE UNIQUE INDEX [UX_SapXepBuoiHoc_HocVienBuoi] ON dbo.[SapXepBuoiHoc] (MaHocVien,MaBuoiHoc) WHERE TrangThai IN ('DA_XEP','HOAN_THANH');
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'UX_SapXepBuoiHoc_HocBuNguon' AND object_id=OBJECT_ID(N'dbo.SapXepBuoiHoc'))
    CREATE UNIQUE INDEX [UX_SapXepBuoiHoc_HocBuNguon] ON dbo.[SapXepBuoiHoc] (MaDangKy,MaSapXepNguon) WHERE LoaiSapXep='HOC_BU' AND TrangThai='HOAN_THANH';
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'IX_SapXepBuoiHoc_DangKy' AND object_id=OBJECT_ID(N'dbo.SapXepBuoiHoc'))
    CREATE INDEX [IX_SapXepBuoiHoc_DangKy] ON dbo.[SapXepBuoiHoc] (MaDangKy,LoaiSapXep,TrangThai);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'UX_KhoanPhaiThu_HocPhiKhoa' AND object_id=OBJECT_ID(N'dbo.KhoanPhaiThu'))
    CREATE UNIQUE INDEX [UX_KhoanPhaiThu_HocPhiKhoa] ON dbo.[KhoanPhaiThu] (MaDangKy) WHERE LoaiKhoan='HOC_PHI_KHOA' AND MaDangKy IS NOT NULL;
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'UX_KhoanPhaiThu_BangLoai' AND object_id=OBJECT_ID(N'dbo.KhoanPhaiThu'))
    CREATE UNIQUE INDEX [UX_KhoanPhaiThu_BangLoai] ON dbo.[KhoanPhaiThu] (MaBangTinh,LoaiKhoan) WHERE MaBangTinh IS NOT NULL;
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'IX_ChiTietPhieuThu_Khoan' AND object_id=OBJECT_ID(N'dbo.ChiTietPhieuThu'))
    CREATE INDEX [IX_ChiTietPhieuThu_Khoan] ON dbo.[ChiTietPhieuThu] (MaKhoanThu,LoaiBienDong);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'IX_ChiTietPhieuThu_Nguon' AND object_id=OBJECT_ID(N'dbo.ChiTietPhieuThu'))
    CREATE INDEX [IX_ChiTietPhieuThu_Nguon] ON dbo.[ChiTietPhieuThu] (MaChiTietNguon) WHERE MaChiTietNguon IS NOT NULL;
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'UX_TinDung_HoanTacNguon' AND object_id=OBJECT_ID(N'dbo.PhatSinhTinDungHocPhi'))
    CREATE UNIQUE INDEX [UX_TinDung_HoanTacNguon] ON dbo.[PhatSinhTinDungHocPhi] (MaChiTietHoanTacNguon) WHERE LoaiBienDong='TAO' AND MaChiTietHoanTacNguon IS NOT NULL;
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'IX_TinDung_SoDu' AND object_id=OBJECT_ID(N'dbo.PhatSinhTinDungHocPhi'))
    CREATE INDEX [IX_TinDung_SoDu] ON dbo.[PhatSinhTinDungHocPhi] (MaHocVien,MaCoSo,LoaiTinDung,LoaiBienDong);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'IX_TienAn_SoDu' AND object_id=OBJECT_ID(N'dbo.PhatSinhTienAn'))
    CREATE INDEX [IX_TienAn_SoDu] ON dbo.[PhatSinhTienAn] (MaHocVien,MaCoSo,LoaiBienDong);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'UX_TienAn_DiemDanh' AND object_id=OBJECT_ID(N'dbo.PhatSinhTienAn'))
    CREATE UNIQUE INDEX [UX_TienAn_DiemDanh] ON dbo.[PhatSinhTienAn] (MaDiemDanh) WHERE LoaiBienDong='TAO' AND MaDiemDanh IS NOT NULL;
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'UX_PhieuKho_KiemKe' AND object_id=OBJECT_ID(N'dbo.PhieuKho'))
    CREATE UNIQUE INDEX [UX_PhieuKho_KiemKe] ON dbo.[PhieuKho] (MaKiemKe) WHERE LoaiPhieu='DIEU_CHINH' AND MaKiemKe IS NOT NULL;
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'IX_ChiTietPhieuKho_Hang' AND object_id=OBJECT_ID(N'dbo.ChiTietPhieuKho'))
    CREATE INDEX [IX_ChiTietPhieuKho_Hang] ON dbo.[ChiTietPhieuKho] (MaHangHoa,MaCoSo);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'IX_ChuyenLopTrungTam_DangKyCu' AND object_id=OBJECT_ID(N'dbo.ChuyenLopTrungTam'))
    CREATE INDEX [IX_ChuyenLopTrungTam_DangKyCu] ON dbo.[ChuyenLopTrungTam] (MaDangKyCu,NgayChuyen);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'IX_DiemDanhMamNon_CoSoNgay' AND object_id=OBJECT_ID(N'dbo.DiemDanhMamNon'))
    CREATE INDEX [IX_DiemDanhMamNon_CoSoNgay] ON dbo.[DiemDanhMamNon] (MaCoSo,NgayHoc);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'IX_PhieuThu_HocVienCoSoNgay' AND object_id=OBJECT_ID(N'dbo.PhieuThu'))
    CREATE INDEX [IX_PhieuThu_HocVienCoSoNgay] ON dbo.[PhieuThu] (MaHocVien,MaCoSo,NgayThu);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'IX_KhoanPhaiThu_HocVienCoSo' AND object_id=OBJECT_ID(N'dbo.KhoanPhaiThu'))
    CREATE INDEX [IX_KhoanPhaiThu_HocVienCoSo] ON dbo.[KhoanPhaiThu] (MaHocVien,MaCoSo,LoaiKhoan);
GO


-- Optimized relationships: reversal must match the exact same receipt/charge/student/facility.
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_ChiTietPhieuThu_HoanTacCungChungTu')
 ALTER TABLE dbo.ChiTietPhieuThu WITH CHECK ADD CONSTRAINT FK_ChiTietPhieuThu_HoanTacCungChungTu
 FOREIGN KEY(MaChiTietNguon,MaPhieuThu,MaKhoanThu,MaHocVien,MaCoSo)
 REFERENCES dbo.ChiTietPhieuThu(MaChiTietPhieuThu,MaPhieuThu,MaKhoanThu,MaHocVien,MaCoSo);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_TinDung_HoanTacDungNguon')
 ALTER TABLE dbo.PhatSinhTinDungHocPhi WITH CHECK ADD CONSTRAINT FK_TinDung_HoanTacDungNguon
 FOREIGN KEY(MaChiTietHoanTacNguon,MaKhoanThuNguon,MaHocVien,MaCoSo)
 REFERENCES dbo.ChiTietPhieuThu(MaChiTietPhieuThu,MaKhoanThu,MaHocVien,MaCoSo);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_SapXepBuoiHoc_NguonCungDangKy')
 ALTER TABLE dbo.SapXepBuoiHoc WITH CHECK ADD CONSTRAINT FK_SapXepBuoiHoc_NguonCungDangKy
 FOREIGN KEY(MaSapXepNguon,MaDangKy) REFERENCES dbo.SapXepBuoiHoc(MaSapXep,MaDangKy);
GO
-- Secondary search indexes supporting class rosters, attendance, bills and period reports.
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'IX_HocVien_Ten' AND object_id=OBJECT_ID(N'dbo.HocVien'))
 CREATE INDEX IX_HocVien_Ten ON dbo.HocVien(HoTen) INCLUDE(NgaySinh,SoDienThoaiLienHe);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'IX_DangKyKhoaHoc_CoSoHocVien' AND object_id=OBJECT_ID(N'dbo.DangKyKhoaHoc'))
 CREATE INDEX IX_DangKyKhoaHoc_CoSoHocVien ON dbo.DangKyKhoaHoc(MaCoSo,MaHocVien,TrangThai) INCLUDE(NgayBatDau,NgayKetThucDuKien);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'IX_BuoiHoc_LopNgay' AND object_id=OBJECT_ID(N'dbo.BuoiHoc'))
 CREATE INDEX IX_BuoiHoc_LopNgay ON dbo.BuoiHoc(MaLop,NgayHoc) INCLUDE(TrangThai);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'IX_XepLopMamNon_CoSoLop' AND object_id=OBJECT_ID(N'dbo.XepLopMamNon'))
 CREATE INDEX IX_XepLopMamNon_CoSoLop ON dbo.XepLopMamNon(MaCoSo,MaLop,TrangThai,TuNgay,DenNgay) INCLUDE(MaHocVien);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'IX_BangTinhPhiMamNon_Thang' AND object_id=OBJECT_ID(N'dbo.BangTinhPhiMamNon'))
 CREATE INDEX IX_BangTinhPhiMamNon_Thang ON dbo.BangTinhPhiMamNon(MaCoSo,MaThang,TrangThai) INCLUDE(MaHocVien,TongHocPhi,TongTienAn);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'IX_SapXepBuoiHoc_Buoi' AND object_id=OBJECT_ID(N'dbo.SapXepBuoiHoc'))
 CREATE INDEX IX_SapXepBuoiHoc_Buoi ON dbo.SapXepBuoiHoc(MaBuoiHoc,TrangThai) INCLUDE(MaDangKy,MaHocVien);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'IX_PhieuKho_KyTrangThai' AND object_id=OBJECT_ID(N'dbo.PhieuKho'))
 CREATE INDEX IX_PhieuKho_KyTrangThai ON dbo.PhieuKho(MaKyKho,TrangThai,LoaiPhieu) INCLUDE(MaCoSo);
GO
-- <<< 02_RANG_BUOC_CHI_MUC.sql

-- >>> 03_DANH_MUC.sql
USE [VINABACUS];
GO
-- Dữ liệu khởi tạo an toàn: không có tài khoản/mật khẩu mẫu.
-- Tên hiển thị có thể được Admin thay đổi; tên cơ sở dữ liệu vẫn là VINABACUS.
IF NOT EXISTS (SELECT 1 FROM dbo.CoSo WHERE LoaiCoSo='MAM_NON')
    INSERT dbo.CoSo(LoaiCoSo,TenCoSo) VALUES ('MAM_NON',N'Cơ sở Mầm non');
IF NOT EXISTS (SELECT 1 FROM dbo.CoSo WHERE LoaiCoSo='TRUNG_TAM')
    INSERT dbo.CoSo(LoaiCoSo,TenCoSo) VALUES ('TRUNG_TAM',N'Cơ sở Trung tâm');
IF NOT EXISTS (SELECT 1 FROM dbo.CaiDatHeThong WHERE MaCaiDat=1)
    INSERT dbo.CaiDatHeThong(MaCaiDat,TenUngDung) VALUES (1,N'VINABACUS');
GO
MERGE dbo.VaiTro WITH (HOLDLOCK) AS d
USING (VALUES ('ADMIN',N'Admin'),('TO_TRUONG',N'Tổ trưởng'),('GIAO_VIEN',N'Giáo viên')) AS s(MaVaiTroChu,TenVaiTro)
ON d.MaVaiTroChu=s.MaVaiTroChu
WHEN NOT MATCHED THEN INSERT (MaVaiTroChu,TenVaiTro) VALUES (s.MaVaiTroChu,s.TenVaiTro);
GO
MERGE dbo.Quyen WITH (HOLDLOCK) AS d
USING (VALUES
('XEM_HOC_VIEN',N'Xem hồ sơ học viên'),
('QUAN_LY_HOC_VIEN',N'Quản lý hồ sơ học viên'),
('XEM_BAO_CAO',N'Xem thống kê cơ sở'),
('DIEM_DANH',N'Điểm danh học viên thuộc lớp được phân công'),
('SUA_DIEM_DANH_DA_CHOT',N'Sửa điểm danh Mầm non sau chốt'),
('XEP_LOP',N'Xếp lớp chính thức'),
('CHUYEN_LOP',N'Chuyển lớp hoặc khóa'),
('THU_HOC_PHI',N'Xác nhận phiếu thu'),
('DIEU_CHINH_HOC_PHI',N'Điều chỉnh học phí và tín dụng'),
('XEM_CONG_NO',N'Xem công nợ học viên'),
('TUYEN_SINH',N'Tiếp nhận khách tiềm năng'),
('TAO_TAI_KHOAN',N'Tạo tài khoản trong cơ sở được phân quyền'),
('PHAN_QUYEN',N'Cấp quyền trong phạm vi được cho phép'),
('THAY_DOI_VAI_TRO',N'Chỉ Admin được nâng hoặc hạ vai trò tại cơ sở'),
('CAI_DAT_HE_THONG',N'Thay đổi tên ứng dụng và logo'),
('KHO_XEM',N'Xem tồn kho tại cơ sở'),
('KHO_NHAP',N'Nhập kho và tồn đầu'),
('KHO_XUAT',N'Xuất học viên và xuất cơ sở'),
('KHO_KIEM_KE',N'Kiểm kê và chốt kho'),
('KHO_HANG_HOA',N'Thêm sửa hàng hóa'),
('KHO_XUAT_EXCEL',N'Xuất tồn kho ra Excel'),
('HOAN_TIEN_AN',N'Hoàn tiền ăn dư cho học viên'),
('CHOT_THANG_MAM_NON',N'Chốt kỳ phí Mầm non')
) AS s(MaQuyenChu,TenQuyen)
ON d.MaQuyenChu=s.MaQuyenChu
WHEN NOT MATCHED THEN INSERT (MaQuyenChu,TenQuyen) VALUES (s.MaQuyenChu,s.TenQuyen);
GO
-- Admin có tất cả quyền; Tổ trưởng không có ba quyền Admin độc quyền;
-- Giáo viên chỉ mặc định được xem, điểm danh, xuất kho (và xem dashboard).
INSERT dbo.VaiTroQuyen(MaVaiTro,MaQuyen)
SELECT v.MaVaiTro,q.MaQuyen
FROM dbo.VaiTro AS v CROSS JOIN dbo.Quyen AS q
WHERE v.MaVaiTroChu='ADMIN'
  AND NOT EXISTS(SELECT 1 FROM dbo.VaiTroQuyen x WHERE x.MaVaiTro=v.MaVaiTro AND x.MaQuyen=q.MaQuyen);
INSERT dbo.VaiTroQuyen(MaVaiTro,MaQuyen)
SELECT v.MaVaiTro,q.MaQuyen
FROM dbo.VaiTro AS v CROSS JOIN dbo.Quyen AS q
WHERE v.MaVaiTroChu='TO_TRUONG'
  AND q.MaQuyenChu NOT IN ('SUA_DIEM_DANH_DA_CHOT','HOAN_TIEN_AN','CAI_DAT_HE_THONG','THAY_DOI_VAI_TRO')
  AND NOT EXISTS(SELECT 1 FROM dbo.VaiTroQuyen x WHERE x.MaVaiTro=v.MaVaiTro AND x.MaQuyen=q.MaQuyen);
INSERT dbo.VaiTroQuyen(MaVaiTro,MaQuyen)
SELECT v.MaVaiTro,q.MaQuyen
FROM dbo.VaiTro AS v CROSS JOIN dbo.Quyen AS q
WHERE v.MaVaiTroChu='GIAO_VIEN'
  AND q.MaQuyenChu IN ('XEM_HOC_VIEN','XEM_BAO_CAO','DIEM_DANH','KHO_XEM','KHO_XUAT')
  AND NOT EXISTS(SELECT 1 FROM dbo.VaiTroQuyen x WHERE x.MaVaiTro=v.MaVaiTro AND x.MaQuyen=q.MaQuyen);
GO
PRINT N'Khoi tao danh muc co so, vai tro va quyen: hoan tat.';
GO
-- <<< 03_DANH_MUC.sql

-- >>> 04_VIEW_GIAO_DICH_GOC.sql
USE [VINABACUS];
GO
-- Quyền trong một cơ sở: quyền cá nhân có thể ghi đè quyền từ vai trò.
CREATE OR ALTER FUNCTION dbo.fn_CoQuyen
(
    @MaTaiKhoan INT, @MaCoSo INT, @MaQuyenChu VARCHAR(70)
)
RETURNS BIT
AS
BEGIN
    DECLARE @KetQua BIT = 0;
    IF NOT EXISTS (
        SELECT 1 FROM dbo.TaiKhoan AS a
        JOIN dbo.TaiKhoanCoSo AS ac ON ac.MaTaiKhoan=a.MaTaiKhoan
        WHERE a.MaTaiKhoan=@MaTaiKhoan AND a.TrangThai=1
          AND ac.MaCoSo=@MaCoSo AND ac.TrangThai=1
    ) RETURN 0;
    -- Ba quyền độc quyền Admin không được mở cho vai trò khác qua ghi đè cá nhân.
    IF @MaQuyenChu IN ('HOAN_TIEN_AN','SUA_DIEM_DANH_DA_CHOT','CAI_DAT_HE_THONG','THAY_DOI_VAI_TRO')
       AND NOT EXISTS (
           SELECT 1 FROM dbo.TaiKhoanCoSo ac
           JOIN dbo.VaiTro vt ON vt.MaVaiTro=ac.MaVaiTro
           WHERE ac.MaTaiKhoan=@MaTaiKhoan AND ac.MaCoSo=@MaCoSo
             AND ac.TrangThai=1 AND vt.MaVaiTroChu='ADMIN'
       ) RETURN 0;
    SELECT @KetQua=COALESCE(
        (SELECT TOP (1) p.ChoPhep FROM dbo.PhanQuyenTaiKhoan p
         JOIN dbo.Quyen q ON q.MaQuyen=p.MaQuyen
         WHERE p.MaTaiKhoan=@MaTaiKhoan AND p.MaCoSo=@MaCoSo AND q.MaQuyenChu=@MaQuyenChu),
        (SELECT TOP (1) CONVERT(BIT,1)
         FROM dbo.TaiKhoanCoSo ac
         JOIN dbo.VaiTroQuyen vq ON vq.MaVaiTro=ac.MaVaiTro
         JOIN dbo.Quyen q ON q.MaQuyen=vq.MaQuyen
         WHERE ac.MaTaiKhoan=@MaTaiKhoan AND ac.MaCoSo=@MaCoSo
           AND ac.TrangThai=1 AND q.MaQuyenChu=@MaQuyenChu),
        CONVERT(BIT,0));
    RETURN @KetQua;
END;
GO
-- Công nợ = nghĩa vụ + điều chỉnh - phân bổ có hiệu lực - tín dụng đã sử dụng.
-- Nợ âm không được hiển thị; tiền vượt cần được đối chiếu riêng.
CREATE OR ALTER VIEW dbo.v_CongNoKhoanThu
AS
SELECT k.MaKhoanThu,k.MaHocVien,k.MaCoSo,k.LoaiKhoan,
       a.NghiaVu,
       COALESCE(p.DaPhanBo,CONVERT(DECIMAL(18,2),0)) AS DaPhanBo,
       COALESCE(t.DaDungTinDung,CONVERT(DECIMAL(18,2),0))+COALESCE(an.DaDungTienAn,CONVERT(DECIMAL(18,2),0)) AS DaDungTinDung,
       CAST(CASE WHEN a.NghiaVu-COALESCE(p.DaPhanBo,0)-COALESCE(t.DaDungTinDung,0)-COALESCE(an.DaDungTienAn,0)>0
          THEN a.NghiaVu-COALESCE(p.DaPhanBo,0)-COALESCE(t.DaDungTinDung,0)-COALESCE(an.DaDungTienAn,0) ELSE 0 END
          AS DECIMAL(18,2)) AS CongNoConLai,
       CAST(CASE WHEN a.NghiaVu-COALESCE(p.DaPhanBo,0)-COALESCE(t.DaDungTinDung,0)-COALESCE(an.DaDungTienAn,0)<0
          THEN COALESCE(p.DaPhanBo,0)+COALESCE(t.DaDungTinDung,0)+COALESCE(an.DaDungTienAn,0)-a.NghiaVu ELSE 0 END
          AS DECIMAL(18,2)) AS ThuVuotChuaDoiChieu
FROM dbo.KhoanPhaiThu AS k
OUTER APPLY (
    SELECT CAST(k.SoTienGoc+COALESCE((
        SELECT SUM(d.SoTienDieuChinh)
        FROM dbo.DieuChinhKhoanThu d
        WHERE d.MaKhoanThu=k.MaKhoanThu AND d.TrangThai='XAC_NHAN'
    ),0) AS DECIMAL(18,2)) AS NghiaVu
) AS a
OUTER APPLY (
    SELECT CAST(COALESCE(SUM(CASE WHEN c.LoaiBienDong='PHAN_BO'
                  THEN c.SoTienPhanBo ELSE -c.SoTienPhanBo END),0) AS DECIMAL(18,2)) AS DaPhanBo
    FROM dbo.ChiTietPhieuThu c JOIN dbo.PhieuThu p ON p.MaPhieuThu=c.MaPhieuThu
    WHERE c.MaKhoanThu=k.MaKhoanThu AND p.TrangThai='XAC_NHAN'
) AS p
OUTER APPLY (
    SELECT CAST(COALESCE(SUM(CASE WHEN t.LoaiBienDong='SU_DUNG' THEN t.SoTien
                      ELSE -t.SoTien END),0) AS DECIMAL(18,2)) AS DaDungTinDung
    FROM dbo.PhatSinhTinDungHocPhi t
    WHERE t.MaKhoanThuDich=k.MaKhoanThu AND t.LoaiBienDong IN ('SU_DUNG','HOAN_TAC_SU_DUNG')
) AS t
OUTER APPLY (
 SELECT CAST(COALESCE(SUM(CASE WHEN x.LoaiBienDong='SU_DUNG' THEN x.SoTien ELSE -x.SoTien END),0) AS DECIMAL(18,2)) AS DaDungTienAn
 FROM dbo.PhatSinhTienAn x
 WHERE x.MaKhoanThuDich=k.MaKhoanThu AND k.LoaiKhoan='TIEN_AN'
   AND x.LoaiBienDong IN ('SU_DUNG','HOAN_TAC_SU_DUNG')
) AS an;
GO
CREATE OR ALTER VIEW dbo.v_SoDuTinDungHocPhi
AS
SELECT MaHocVien,MaCoSo,LoaiTinDung,
       CAST(SUM(CASE WHEN LoaiBienDong IN ('TAO','HOAN_TAC_SU_DUNG')
           THEN SoTien ELSE -SoTien END) AS DECIMAL(18,2)) AS SoDu
FROM dbo.PhatSinhTinDungHocPhi
GROUP BY MaHocVien,MaCoSo,LoaiTinDung;
GO
CREATE OR ALTER VIEW dbo.v_TonKhoHienTai
AS
SELECT h.MaHangHoa,h.MaCoSo,h.MaSanPham,h.TenHangHoa,h.DonViTinh,
       CAST(COALESCE(SUM(CASE WHEN p.TrangThai='XAC_NHAN'
           THEN CASE WHEN p.LoaiPhieu IN ('XUAT_HS','XUAT_CS')
                THEN -c.SoLuong ELSE c.SoLuong END ELSE 0 END),0) AS DECIMAL(18,3)) AS TonHienTai
FROM dbo.HangHoa h
LEFT JOIN dbo.ChiTietPhieuKho c ON c.MaHangHoa=h.MaHangHoa AND c.MaCoSo=h.MaCoSo
LEFT JOIN dbo.PhieuKho p ON p.MaPhieuKho=c.MaPhieuKho AND p.MaCoSo=h.MaCoSo
GROUP BY h.MaHangHoa,h.MaCoSo,h.MaSanPham,h.TenHangHoa,h.DonViTinh;
GO
-- Một phiếu thu có thể phân bổ vào nhiều khoản cùng học viên/cơ sở.
IF TYPE_ID(N'dbo.TVP_PhanBoThu') IS NULL
    EXEC(N'CREATE TYPE dbo.TVP_PhanBoThu AS TABLE (MaKhoanThu INT NOT NULL PRIMARY KEY, SoTien DECIMAL(18,2) NOT NULL CHECK (SoTien>0))');
GO
IF TYPE_ID(N'dbo.TVP_HangKho') IS NULL
    EXEC(N'CREATE TYPE dbo.TVP_HangKho AS TABLE (MaHangHoa INT NOT NULL PRIMARY KEY, SoLuong DECIMAL(18,3) NOT NULL CHECK (SoLuong>0))');
GO
IF TYPE_ID(N'dbo.TVP_KiemKeKho') IS NULL
    EXEC(N'CREATE TYPE dbo.TVP_KiemKeKho AS TABLE (MaHangHoa INT NOT NULL PRIMARY KEY, TonThucTe DECIMAL(18,3) NOT NULL CHECK (TonThucTe>=0))');
GO
CREATE OR ALTER PROCEDURE dbo.usp_XacNhanPhieuThu
    @MaHocVien INT,@MaCoSo INT,@MaTaiKhoan INT,
    @SoPhieuThu VARCHAR(40),@HinhThucThanhToan VARCHAR(25),
    @MaGiaoDichNghiepVu VARCHAR(64),@PhanBo dbo.TVP_PhanBoThu READONLY,
    @GhiChu NVARCHAR(500)=NULL
AS
BEGIN
 SET NOCOUNT ON; SET XACT_ABORT ON;
 DECLARE @Tong DECIMAL(18,2),@MaPhieuThu INT,@MaKhoa INT,@TaiNguyen NVARCHAR(255);
 IF dbo.fn_CoQuyen(@MaTaiKhoan,@MaCoSo,'THU_HOC_PHI')<>1 THROW 51001,N'Không có quyền thu học phí tại cơ sở này.',1;
 SELECT @Tong=SUM(SoTien) FROM @PhanBo;
 IF @Tong IS NULL OR @Tong<=0 THROW 51002,N'Phiếu thu phải có ít nhất một khoản phân bổ dương.',1;
 BEGIN TRY
   SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
   BEGIN TRAN;
   SET @TaiNguyen=CONCAT('VINABACUS:TAI_CHINH:',@MaCoSo,':',@MaHocVien);
   EXEC @MaKhoa=sys.sp_getapplock @Resource=@TaiNguyen,
        @LockMode='Exclusive',@LockOwner='Transaction',@LockTimeout=10000;
   IF @MaKhoa<0 THROW 51003,N'Không khóa được giao dịch tài chính của học viên.',1;
   IF EXISTS(SELECT 1 FROM dbo.PhieuThu WHERE MaGiaoDichNghiepVu=@MaGiaoDichNghiepVu OR SoPhieuThu=@SoPhieuThu)
      THROW 51004,N'Phiếu thu hoặc giao dịch đã tồn tại.',1;
   IF EXISTS (
      SELECT 1 FROM @PhanBo x LEFT JOIN dbo.KhoanPhaiThu k WITH (UPDLOCK,HOLDLOCK)
        ON k.MaKhoanThu=x.MaKhoanThu AND k.MaHocVien=@MaHocVien AND k.MaCoSo=@MaCoSo AND k.TrangThai='HIEU_LUC'
      WHERE k.MaKhoanThu IS NULL
   ) THROW 51005,N'Khoản phải thu không thuộc học viên hoặc cơ sở.',1;
   IF EXISTS (
      SELECT 1 FROM @PhanBo x JOIN dbo.v_CongNoKhoanThu v ON v.MaKhoanThu=x.MaKhoanThu
      WHERE x.SoTien>v.CongNoConLai
   ) THROW 51006,N'Số tiền phân bổ vượt công nợ còn lại.',1;
   INSERT dbo.PhieuThu(SoPhieuThu,MaHocVien,MaCoSo,TongTien,HinhThucThanhToan,MaTaiKhoan,
                      TrangThai,MaGiaoDichNghiepVu,GhiChu)
   VALUES(@SoPhieuThu,@MaHocVien,@MaCoSo,@Tong,@HinhThucThanhToan,@MaTaiKhoan,
          'XAC_NHAN',@MaGiaoDichNghiepVu,@GhiChu);
   SET @MaPhieuThu=CONVERT(INT,SCOPE_IDENTITY());
   INSERT dbo.ChiTietPhieuThu(MaPhieuThu,MaKhoanThu,MaHocVien,MaCoSo,SoTienPhanBo,
                             LoaiBienDong,MaGiaoDichNghiepVu)
   SELECT @MaPhieuThu,x.MaKhoanThu,@MaHocVien,@MaCoSo,x.SoTien,'PHAN_BO',
          CONVERT(VARCHAR(64),HASHBYTES('SHA2_256',CONCAT(@MaGiaoDichNghiepVu,':',x.MaKhoanThu)),2)
   FROM @PhanBo x;
   COMMIT;
   SELECT @MaPhieuThu AS MaPhieuThu,@Tong AS TongTien;
 END TRY
 BEGIN CATCH
    IF XACT_STATE()<>0 ROLLBACK;
    THROW;
 END CATCH
END;
GO
CREATE OR ALTER PROCEDURE dbo.usp_TaoTinDungTuThuDu
    @MaChiTietPhanBoGoc INT,@MaDieuChinh INT,@SoTien DECIMAL(18,2),
    @MaTaiKhoan INT,@MaGiaoDichNghiepVu VARCHAR(64),@LyDo NVARCHAR(500)
AS
BEGIN
 SET NOCOUNT ON; SET XACT_ABORT ON;
 DECLARE @MaKhoanThu INT,@MaPhieuThu INT,@MaHocVien INT,@MaCoSo INT,@TaiNguyen NVARCHAR(255),
         @LoaiKhoan VARCHAR(25),@LoaiTinDung VARCHAR(20),@MaKhoa INT,
         @SoTienGoc DECIMAL(18,2),@DaHoanTac DECIMAL(18,2),@NghiaVu DECIMAL(18,2),
         @DaPhanBo DECIMAL(18,2),@DaDungTinDung DECIMAL(18,2),@MaHoanTac INT;
 IF @SoTien<=0 THROW 51101,N'Số tiền tạo tín dụng phải lớn hơn 0.',1;
 BEGIN TRY
   SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
   BEGIN TRAN;
   SELECT @MaKhoanThu=c.MaKhoanThu,@MaPhieuThu=c.MaPhieuThu,
          @MaHocVien=c.MaHocVien,@MaCoSo=c.MaCoSo,@LoaiKhoan=k.LoaiKhoan,
          @SoTienGoc=c.SoTienPhanBo
   FROM dbo.ChiTietPhieuThu c WITH (UPDLOCK,HOLDLOCK)
   JOIN dbo.PhieuThu p ON p.MaPhieuThu=c.MaPhieuThu AND p.TrangThai='XAC_NHAN'
   JOIN dbo.KhoanPhaiThu k ON k.MaKhoanThu=c.MaKhoanThu AND k.TrangThai='HIEU_LUC'
   WHERE c.MaChiTietPhieuThu=@MaChiTietPhanBoGoc AND c.LoaiBienDong='PHAN_BO';
   IF @MaKhoanThu IS NULL THROW 51102,N'Không tìm thấy dòng phân bổ hợp lệ.',1;
   IF @LoaiKhoan='TIEN_AN' THROW 51103,N'Tín dụng học phí không dùng cho tiền ăn.',1;
   IF dbo.fn_CoQuyen(@MaTaiKhoan,@MaCoSo,'DIEU_CHINH_HOC_PHI')<>1
       THROW 51104,N'Không có quyền điều chỉnh học phí.',1;
   SET @TaiNguyen=CONCAT('VINABACUS:TAI_CHINH:',@MaCoSo,':',@MaHocVien);
   EXEC @MaKhoa=sys.sp_getapplock @Resource=@TaiNguyen,
        @LockMode='Exclusive',@LockOwner='Transaction',@LockTimeout=10000;
   IF @MaKhoa<0 THROW 51105,N'Không khóa được giao dịch tài chính.',1;
   IF NOT EXISTS (SELECT 1 FROM dbo.DieuChinhKhoanThu
                 WHERE MaDieuChinh=@MaDieuChinh AND MaKhoanThu=@MaKhoanThu AND TrangThai='XAC_NHAN')
       THROW 51106,N'Không có chứng từ điều chỉnh học phí đã xác nhận.',1;
   IF EXISTS (SELECT 1 FROM dbo.PhatSinhTinDungHocPhi WHERE MaGiaoDichNghiepVu=@MaGiaoDichNghiepVu)
       THROW 51107,N'Giao dịch tạo tín dụng đã tồn tại.',1;
   SELECT @DaHoanTac=COALESCE(SUM(c.SoTienPhanBo),0)
   FROM dbo.ChiTietPhieuThu c WITH (UPDLOCK,HOLDLOCK)
   WHERE c.MaChiTietNguon=@MaChiTietPhanBoGoc AND c.LoaiBienDong='HOAN_TAC_PHAN_BO';
   IF @SoTienGoc-@DaHoanTac<@SoTien THROW 51108,N'Hoàn tác vượt phần phân bổ chưa hoàn tác.',1;
   SELECT @NghiaVu=NghiaVu,@DaPhanBo=DaPhanBo,@DaDungTinDung=DaDungTinDung
   FROM dbo.v_CongNoKhoanThu WHERE MaKhoanThu=@MaKhoanThu;
   IF @DaPhanBo+@DaDungTinDung-@SoTien<@NghiaVu
       THROW 51109,N'Không đủ phần tiền thu vượt để tạo tín dụng.',1;
   SET @LoaiTinDung=CASE WHEN @LoaiKhoan='HOC_PHI_THANG' THEN 'MAM_NON' ELSE 'TRUNG_TAM' END;
   INSERT dbo.ChiTietPhieuThu(MaPhieuThu,MaKhoanThu,MaHocVien,MaCoSo,SoTienPhanBo,
       LoaiBienDong,MaChiTietNguon,MaDieuChinh,MaGiaoDichNghiepVu)
   VALUES(@MaPhieuThu,@MaKhoanThu,@MaHocVien,@MaCoSo,@SoTien,
       'HOAN_TAC_PHAN_BO',@MaChiTietPhanBoGoc,@MaDieuChinh,
       CONVERT(VARCHAR(64),HASHBYTES('SHA2_256',CONCAT(@MaGiaoDichNghiepVu,':HOAN_TAC')),2));
   SET @MaHoanTac=CONVERT(INT,SCOPE_IDENTITY());
   INSERT dbo.PhatSinhTinDungHocPhi(MaHocVien,MaCoSo,LoaiTinDung,LoaiBienDong,MaKhoanThuNguon,
           MaChiTietHoanTacNguon,SoTien,MaGiaoDichNghiepVu,MaTaiKhoan,LyDo)
   VALUES(@MaHocVien,@MaCoSo,@LoaiTinDung,'TAO',@MaKhoanThu,@MaHoanTac,
          @SoTien,@MaGiaoDichNghiepVu,@MaTaiKhoan,@LyDo);
   COMMIT;
   SELECT @MaHoanTac AS MaChiTietHoanTac,CONVERT(INT,SCOPE_IDENTITY()) AS MaPhatSinh;
 END TRY
 BEGIN CATCH
   IF XACT_STATE()<>0 ROLLBACK;
   THROW;
 END CATCH
END;
GO
CREATE OR ALTER PROCEDURE dbo.usp_SuDungTinDungHocPhi
    @MaHocVien INT,@MaCoSo INT,@MaKhoanThuDich INT,@SoTien DECIMAL(18,2),
    @MaTaiKhoan INT,@MaGiaoDichNghiepVu VARCHAR(64),@LyDo NVARCHAR(500)
AS
BEGIN
 SET NOCOUNT ON; SET XACT_ABORT ON;
 DECLARE @LoaiTinDung VARCHAR(20),@LoaiKhoan VARCHAR(25),@MaKhoa INT,@TaiNguyen NVARCHAR(255),
         @SoDu DECIMAL(18,2),@CongNo DECIMAL(18,2),@MaPhatSinh INT;
 IF @SoTien<=0 THROW 51201,N'Số tiền sử dụng tín dụng phải lớn hơn 0.',1;
 IF dbo.fn_CoQuyen(@MaTaiKhoan,@MaCoSo,'THU_HOC_PHI')<>1
       THROW 51202,N'Không có quyền xử lý thanh toán học phí.',1;
 BEGIN TRY
   SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
   BEGIN TRAN;
   SET @TaiNguyen=CONCAT('VINABACUS:TAI_CHINH:',@MaCoSo,':',@MaHocVien);
   EXEC @MaKhoa=sys.sp_getapplock @Resource=@TaiNguyen,
        @LockMode='Exclusive',@LockOwner='Transaction',@LockTimeout=10000;
   IF @MaKhoa<0 THROW 51203,N'Không khóa được tài chính học viên.',1;
   SELECT @LoaiKhoan=k.LoaiKhoan
   FROM dbo.KhoanPhaiThu k WITH (UPDLOCK,HOLDLOCK)
   WHERE k.MaKhoanThu=@MaKhoanThuDich AND k.MaHocVien=@MaHocVien
     AND k.MaCoSo=@MaCoSo AND k.TrangThai='HIEU_LUC';
   IF @LoaiKhoan NOT IN ('HOC_PHI_KHOA','HOC_PHI_THANG') OR @LoaiKhoan IS NULL
       THROW 51204,N'Tín dụng chỉ dùng cho đúng học phí của học viên.',1;
   SET @LoaiTinDung=CASE WHEN @LoaiKhoan='HOC_PHI_KHOA' THEN 'TRUNG_TAM' ELSE 'MAM_NON' END;
   SELECT @CongNo=CongNoConLai FROM dbo.v_CongNoKhoanThu WHERE MaKhoanThu=@MaKhoanThuDich;
   SELECT @SoDu=COALESCE(SUM(CASE WHEN LoaiBienDong IN ('TAO','HOAN_TAC_SU_DUNG')
                        THEN SoTien ELSE -SoTien END),0)
   FROM dbo.PhatSinhTinDungHocPhi WITH (UPDLOCK,HOLDLOCK)
   WHERE MaHocVien=@MaHocVien AND MaCoSo=@MaCoSo AND LoaiTinDung=@LoaiTinDung;
   IF @SoDu<@SoTien THROW 51205,N'Không đủ số dư tín dụng học phí.',1;
   IF @CongNo<@SoTien THROW 51206,N'Khấu trừ vượt công nợ khoản thu đích.',1;
   IF EXISTS (SELECT 1 FROM dbo.PhatSinhTinDungHocPhi WHERE MaGiaoDichNghiepVu=@MaGiaoDichNghiepVu)
       THROW 51207,N'Giao dịch sử dụng tín dụng đã tồn tại.',1;
   INSERT dbo.PhatSinhTinDungHocPhi(MaHocVien,MaCoSo,LoaiTinDung,LoaiBienDong,
       MaKhoanThuDich,SoTien,MaGiaoDichNghiepVu,MaTaiKhoan,LyDo)
   VALUES(@MaHocVien,@MaCoSo,@LoaiTinDung,'SU_DUNG',
       @MaKhoanThuDich,@SoTien,@MaGiaoDichNghiepVu,@MaTaiKhoan,@LyDo);
   SET @MaPhatSinh=CONVERT(INT,SCOPE_IDENTITY());
   COMMIT;
   SELECT @MaPhatSinh AS MaPhatSinh,@SoDu-@SoTien AS SoDuConLai;
 END TRY
 BEGIN CATCH
    IF XACT_STATE()<>0 ROLLBACK;
    THROW;
 END CATCH
END;
GO
CREATE OR ALTER PROCEDURE dbo.usp_XacNhanPhieuKho
    @MaCoSo INT,@MaKyKho INT,@MaTaiKhoan INT,
    @LoaiPhieu VARCHAR(25),@MaGiaoDichNghiepVu VARCHAR(64),
    @ChiTiet dbo.TVP_HangKho READONLY,@GhiChu NVARCHAR(500)=NULL
AS
BEGIN
 SET NOCOUNT ON; SET XACT_ABORT ON;
 DECLARE @MaKhoa INT,@MaHangHoa INT,@MaPhieuKho INT,@TaiNguyen NVARCHAR(255);
 IF @LoaiPhieu NOT IN ('NHAP','XUAT_HS','XUAT_CS','TON_DAU')
    THROW 51301,N'Phiếu điều chỉnh chỉ được tạo từ xác nhận kiểm kê.',1;
 IF dbo.fn_CoQuyen(@MaTaiKhoan,@MaCoSo,CASE WHEN @LoaiPhieu IN ('NHAP','TON_DAU') THEN 'KHO_NHAP' ELSE 'KHO_XUAT' END)<>1
    THROW 51302,N'Không có quyền thực hiện loại phiếu kho.',1;
 IF NOT EXISTS(SELECT 1 FROM @ChiTiet) THROW 51303,N'Phiếu kho không có mặt hàng.',1;
 BEGIN TRY
   SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
   BEGIN TRAN;
   IF NOT EXISTS(SELECT 1 FROM dbo.KyKho WITH (UPDLOCK,HOLDLOCK)
                 WHERE MaKyKho=@MaKyKho AND MaCoSo=@MaCoSo AND TrangThai='MO')
      THROW 51304,N'Kỳ kho không hợp lệ hoặc đã chốt.',1;
   IF EXISTS(SELECT 1 FROM dbo.PhieuKho WHERE MaGiaoDichNghiepVu=@MaGiaoDichNghiepVu)
      THROW 51305,N'Giao dịch phiếu kho đã tồn tại.',1;
   IF EXISTS(SELECT 1 FROM @ChiTiet d LEFT JOIN dbo.HangHoa h
             ON h.MaHangHoa=d.MaHangHoa AND h.MaCoSo=@MaCoSo AND h.TrangThai=1
             WHERE h.MaHangHoa IS NULL)
      THROW 51306,N'Mặt hàng không thuộc cơ sở hoặc đã khóa.',1;
   -- Khóa từng hàng theo mã tăng dần để giảm nguy cơ deadlock.
   DECLARE curHang CURSOR LOCAL FAST_FORWARD FOR SELECT MaHangHoa FROM @ChiTiet ORDER BY MaHangHoa;
   OPEN curHang; FETCH NEXT FROM curHang INTO @MaHangHoa;
   WHILE @@FETCH_STATUS=0
   BEGIN
      SET @TaiNguyen=CONCAT('VINABACUS:KHO:',@MaCoSo,':',@MaHangHoa);
      EXEC @MaKhoa=sys.sp_getapplock @Resource=@TaiNguyen,
          @LockMode='Exclusive',@LockOwner='Transaction',@LockTimeout=10000;
      IF @MaKhoa<0 THROW 51307,N'Không khóa được mặt hàng.',1;
      FETCH NEXT FROM curHang INTO @MaHangHoa;
   END;
   CLOSE curHang; DEALLOCATE curHang;
   IF @LoaiPhieu='TON_DAU' AND EXISTS (
       SELECT 1 FROM @ChiTiet d
       JOIN dbo.ChiTietPhieuKho c ON c.MaHangHoa=d.MaHangHoa AND c.MaCoSo=@MaCoSo
       JOIN dbo.PhieuKho p ON p.MaPhieuKho=c.MaPhieuKho AND p.TrangThai='XAC_NHAN'
   ) THROW 51309,N'Hàng hóa đã có lịch sử kho, không thể tạo tồn đầu thêm lần nữa.',1;
   IF @LoaiPhieu IN ('XUAT_HS','XUAT_CS') AND EXISTS (
       SELECT 1 FROM @ChiTiet d
       JOIN dbo.v_TonKhoHienTai t ON t.MaHangHoa=d.MaHangHoa AND t.MaCoSo=@MaCoSo
       WHERE d.SoLuong>t.TonHienTai
   ) THROW 51308,N'Số lượng xuất vượt tồn kho hiện tại.',1;
   INSERT dbo.PhieuKho(MaCoSo,MaKyKho,LoaiPhieu,MaTaiKhoan,TrangThai,MaGiaoDichNghiepVu,GhiChu)
   VALUES(@MaCoSo,@MaKyKho,@LoaiPhieu,@MaTaiKhoan,'XAC_NHAN',@MaGiaoDichNghiepVu,@GhiChu);
   SET @MaPhieuKho=CONVERT(INT,SCOPE_IDENTITY());
   INSERT dbo.ChiTietPhieuKho(MaPhieuKho,MaCoSo,MaHangHoa,SoLuong,
          DonGiaSiTaiThoiDiem,DonGiaXuatBanTaiThoiDiem)
   SELECT @MaPhieuKho,@MaCoSo,d.MaHangHoa,d.SoLuong,h.DonGiaSi,h.DonGiaXuatBan
   FROM @ChiTiet d JOIN dbo.HangHoa h ON h.MaHangHoa=d.MaHangHoa AND h.MaCoSo=@MaCoSo;
   COMMIT;
   SELECT @MaPhieuKho AS MaPhieuKho;
 END TRY
 BEGIN CATCH
   IF CURSOR_STATUS('local','curHang')>=0 CLOSE curHang;
   IF CURSOR_STATUS('local','curHang')>=-1 DEALLOCATE curHang;
   IF XACT_STATE()<>0 ROLLBACK;
   THROW;
 END CATCH
END;
GO
-- <<< 04_VIEW_GIAO_DICH_GOC.sql

-- >>> 05_KHO_KIEM_KE.sql
USE [VINABACUS];
GO
-- Kiểm kê nguyên khối: chụp tồn theo sổ, tồn thực tế và chỉ một phiếu điều chỉnh/kết quả.
CREATE OR ALTER PROCEDURE dbo.usp_XacNhanKiemKeKho
    @MaCoSo INT,@MaKyKho INT,@MaTaiKhoan INT,
    @MaGiaoDichNghiepVu VARCHAR(64),@ChiTiet dbo.TVP_KiemKeKho READONLY,
    @GhiChu NVARCHAR(500)=NULL
AS
BEGIN
 SET NOCOUNT ON; SET XACT_ABORT ON;
 DECLARE @MaKiemKe INT,@MaPhieuKho INT,@MaHangHoa INT,
         @MaKhoa INT,@TaiNguyen NVARCHAR(255);
 IF dbo.fn_CoQuyen(@MaTaiKhoan,@MaCoSo,'KHO_KIEM_KE')<>1
     THROW 51401,N'Không có quyền kiểm kê kho.',1;
 IF NOT EXISTS(SELECT 1 FROM @ChiTiet) THROW 51402,N'Phiếu kiểm kê không có hàng hóa.',1;
 BEGIN TRY
  SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
  BEGIN TRAN;
  IF NOT EXISTS(SELECT 1 FROM dbo.KyKho WITH (UPDLOCK,HOLDLOCK)
                WHERE MaKyKho=@MaKyKho AND MaCoSo=@MaCoSo AND TrangThai='MO')
       THROW 51403,N'Kỳ kho không thuộc cơ sở hoặc đã chốt.',1;
  IF EXISTS(SELECT 1 FROM dbo.KiemKeKho WHERE MaGiaoDichNghiepVu=@MaGiaoDichNghiepVu)
       THROW 51404,N'Giao dịch kiểm kê đã tồn tại.',1;
  IF EXISTS (SELECT 1 FROM @ChiTiet d LEFT JOIN dbo.HangHoa h
             ON h.MaHangHoa=d.MaHangHoa AND h.MaCoSo=@MaCoSo
             WHERE h.MaHangHoa IS NULL)
       THROW 51405,N'Hàng hóa kiểm kê không thuộc cơ sở.',1;
  DECLARE curKiemKe CURSOR LOCAL FAST_FORWARD FOR SELECT MaHangHoa FROM @ChiTiet ORDER BY MaHangHoa;
  OPEN curKiemKe; FETCH NEXT FROM curKiemKe INTO @MaHangHoa;
  WHILE @@FETCH_STATUS=0
  BEGIN
      SET @TaiNguyen=CONCAT('VINABACUS:KHO:',@MaCoSo,':',@MaHangHoa);
      EXEC @MaKhoa=sys.sp_getapplock @Resource=@TaiNguyen,
          @LockMode='Exclusive',@LockOwner='Transaction',@LockTimeout=10000;
      IF @MaKhoa<0 THROW 51406,N'Không khóa được hàng hóa kiểm kê.',1;
      FETCH NEXT FROM curKiemKe INTO @MaHangHoa;
  END;
  CLOSE curKiemKe; DEALLOCATE curKiemKe;
  INSERT dbo.KiemKeKho(MaKyKho,MaCoSo,MaTaiKhoan,TrangThai,MaGiaoDichNghiepVu,GhiChu)
  VALUES(@MaKyKho,@MaCoSo,@MaTaiKhoan,'XAC_NHAN',@MaGiaoDichNghiepVu,@GhiChu);
  SET @MaKiemKe=CONVERT(INT,SCOPE_IDENTITY());
  INSERT dbo.ChiTietKiemKe(MaKiemKe,MaCoSo,MaHangHoa,TonHeThong,TonThucTe)
  SELECT @MaKiemKe,@MaCoSo,d.MaHangHoa,t.TonHienTai,d.TonThucTe
  FROM @ChiTiet d JOIN dbo.v_TonKhoHienTai t
    ON t.MaHangHoa=d.MaHangHoa AND t.MaCoSo=@MaCoSo;
  IF EXISTS (SELECT 1 FROM dbo.ChiTietKiemKe
             WHERE MaKiemKe=@MaKiemKe AND TonThucTe<>TonHeThong)
  BEGIN
      INSERT dbo.PhieuKho(MaCoSo,MaKyKho,MaKiemKe,LoaiPhieu,MaTaiKhoan,
                         TrangThai,MaGiaoDichNghiepVu,GhiChu)
      VALUES(@MaCoSo,@MaKyKho,@MaKiemKe,'DIEU_CHINH',@MaTaiKhoan,'XAC_NHAN',
             CONVERT(VARCHAR(64),HASHBYTES('SHA2_256',CONCAT(@MaGiaoDichNghiepVu,':DIEU_CHINH')),2),
             N'Điều chỉnh tự động từ kiểm kê');
      SET @MaPhieuKho=CONVERT(INT,SCOPE_IDENTITY());
      INSERT dbo.ChiTietPhieuKho(MaPhieuKho,MaCoSo,MaHangHoa,SoLuong,
            DonGiaSiTaiThoiDiem,DonGiaXuatBanTaiThoiDiem,GhiChu)
      SELECT @MaPhieuKho,@MaCoSo,c.MaHangHoa,c.TonThucTe-c.TonHeThong,
             h.DonGiaSi,h.DonGiaXuatBan,N'Chênh lệch kiểm kê'
      FROM dbo.ChiTietKiemKe c JOIN dbo.HangHoa h
           ON h.MaHangHoa=c.MaHangHoa AND h.MaCoSo=c.MaCoSo
      WHERE c.MaKiemKe=@MaKiemKe AND c.TonThucTe<>c.TonHeThong;
  END;
  COMMIT;
  SELECT @MaKiemKe AS MaKiemKe,@MaPhieuKho AS MaPhieuDieuChinh;
 END TRY
 BEGIN CATCH
    IF CURSOR_STATUS('local','curKiemKe')>=0 CLOSE curKiemKe;
    IF CURSOR_STATUS('local','curKiemKe')>=-1 DEALLOCATE curKiemKe;
    IF XACT_STATE()<>0 ROLLBACK;
    THROW;
 END CATCH
END;
GO
-- Chốt kỳ kho: ảnh chụp 12 cột mẫu Excel + điều chỉnh kiểm kê nội bộ.
CREATE OR ALTER PROCEDURE dbo.usp_ChotKyKho
    @MaKyKho INT,@MaTaiKhoan INT
AS
BEGIN
 SET NOCOUNT ON; SET XACT_ABORT ON;
 DECLARE @MaCoSo INT,@Nam SMALLINT,@Thang TINYINT,@MaKhoa INT,@TaiNguyen NVARCHAR(255);
 BEGIN TRY
  SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
  BEGIN TRAN;
  SELECT @MaCoSo=MaCoSo,@Nam=Nam,@Thang=Thang
  FROM dbo.KyKho WITH (UPDLOCK,HOLDLOCK)
  WHERE MaKyKho=@MaKyKho AND TrangThai='MO';
  IF @MaCoSo IS NULL THROW 51501,N'Kỳ kho không tồn tại hoặc đã chốt.',1;
  IF dbo.fn_CoQuyen(@MaTaiKhoan,@MaCoSo,'KHO_KIEM_KE')<>1
      THROW 51502,N'Không có quyền chốt kho tại cơ sở.',1;
  SET @TaiNguyen=CONCAT('VINABACUS:CHOT_KHO:',@MaCoSo,':',@Nam,':',@Thang);
  EXEC @MaKhoa=sys.sp_getapplock @Resource=@TaiNguyen,
       @LockMode='Exclusive',@LockOwner='Transaction',@LockTimeout=10000;
  IF @MaKhoa<0 THROW 51503,N'Không khóa được kỳ kho.',1;
  IF EXISTS(SELECT 1 FROM dbo.KyKho k WHERE k.MaCoSo=@MaCoSo
       AND (k.Nam<@Nam OR (k.Nam=@Nam AND k.Thang<@Thang)) AND k.TrangThai='MO')
      THROW 51504,N'Còn kỳ kho trước chưa chốt.',1;
  IF EXISTS(SELECT 1 FROM dbo.KiemKeKho k WHERE k.MaKyKho=@MaKyKho AND k.TrangThai='NHAP')
      THROW 51505,N'Còn phiếu kiểm kê chưa xác nhận.',1;
  IF EXISTS(SELECT 1 FROM dbo.PhieuKho p WHERE p.MaKyKho=@MaKyKho AND p.TrangThai='NHAP')
      THROW 51506,N'Còn phiếu kho chưa xác nhận.',1;
  IF EXISTS(SELECT 1 FROM dbo.SoLieuChotKho WHERE MaKyKho=@MaKyKho)
      THROW 51507,N'Số liệu chốt kỳ đã tồn tại.',1;
  ;WITH DuLieu AS (
    SELECT h.MaHangHoa,h.MaSanPham,h.TenHangHoa,h.DonViTinh,h.DonGiaSi,h.DonGiaXuatBan,
      CAST(COALESCE(prev.TonCuoi,0)+CASE WHEN prev.TonCuoi IS NULL
           THEN COALESCE(m.TonDauMoi,0) ELSE 0 END AS DECIMAL(18,3)) AS TonDau,
      CAST(COALESCE(m.Nhap,0) AS DECIMAL(18,3)) AS Nhap,
      CAST(COALESCE(m.XuatHS,0) AS DECIMAL(18,3)) AS XuatHS,
      CAST(COALESCE(m.XuatCS,0) AS DECIMAL(18,3)) AS XuatCS,
      CAST(COALESCE(m.DieuChinh,0) AS DECIMAL(18,3)) AS DieuChinh
    FROM dbo.HangHoa h
    OUTER APPLY (
       SELECT TOP (1) s.TonCuoi
       FROM dbo.SoLieuChotKho s
       JOIN dbo.KyKho k ON k.MaKyKho=s.MaKyKho
       WHERE s.MaHangHoa=h.MaHangHoa AND k.MaCoSo=@MaCoSo
         AND (k.Nam<@Nam OR (k.Nam=@Nam AND k.Thang<@Thang))
         AND k.TrangThai='CHOT'
       ORDER BY k.Nam DESC,k.Thang DESC
    ) prev
    OUTER APPLY (
       SELECT SUM(CASE WHEN p.LoaiPhieu='TON_DAU' THEN d.SoLuong ELSE 0 END) AS TonDauMoi,
              SUM(CASE WHEN p.LoaiPhieu='NHAP' THEN d.SoLuong ELSE 0 END) AS Nhap,
              SUM(CASE WHEN p.LoaiPhieu='XUAT_HS' THEN d.SoLuong ELSE 0 END) AS XuatHS,
              SUM(CASE WHEN p.LoaiPhieu='XUAT_CS' THEN d.SoLuong ELSE 0 END) AS XuatCS,
              SUM(CASE WHEN p.LoaiPhieu='DIEU_CHINH' THEN d.SoLuong ELSE 0 END) AS DieuChinh
       FROM dbo.ChiTietPhieuKho d JOIN dbo.PhieuKho p ON p.MaPhieuKho=d.MaPhieuKho
       WHERE d.MaHangHoa=h.MaHangHoa AND p.MaKyKho=@MaKyKho AND p.TrangThai='XAC_NHAN'
    ) m
    WHERE h.MaCoSo=@MaCoSo
  )
  INSERT dbo.SoLieuChotKho(MaKyKho,MaCoSo,MaHangHoa,MaSanPham,TenHangHoa,DonViTinh,
      DonGiaSi,DonGiaXuatBan,TonDau,Nhap,XuatHS,XuatCS,DieuChinhKiemKe,TonCuoi,GhiChu)
  SELECT @MaKyKho,@MaCoSo,MaHangHoa,MaSanPham,TenHangHoa,DonViTinh,
     DonGiaSi,DonGiaXuatBan,TonDau,Nhap,XuatHS,XuatCS,DieuChinh,
     TonDau+Nhap-XuatHS-XuatCS+DieuChinh,
     CASE WHEN DieuChinh<>0 THEN N'Chênh lệch kiểm kê: '+CONVERT(NVARCHAR(40),DieuChinh) ELSE NULL END
  FROM DuLieu;
  IF EXISTS (SELECT 1 FROM dbo.SoLieuChotKho WHERE MaKyKho=@MaKyKho AND TonCuoi<0)
      THROW 51508,N'Tồn cuối âm sau chốt; giao dịch đã hoàn tác.',1;
  UPDATE dbo.KyKho SET TrangThai='CHOT',NgayChot=SYSUTCDATETIME(),MaTaiKhoanChot=@MaTaiKhoan
  WHERE MaKyKho=@MaKyKho;
  COMMIT;
  SELECT @MaKyKho AS MaKyKho,COUNT(*) AS SoDongChot
  FROM dbo.SoLieuChotKho WHERE MaKyKho=@MaKyKho;
 END TRY
 BEGIN CATCH
   IF XACT_STATE()<>0 ROLLBACK;
   THROW;
 END CATCH
END;
GO
-- <<< 05_KHO_KIEM_KE.sql

-- >>> 06_TUYEN_SINH_TRUNG_TAM.sql
USE [VINABACUS];
GO
-- Xếp lớp chính thức cho học viên TRUNG TÂM từ một yêu cầu tuyển sinh đã xác nhận.
-- Thực hiện nguyên khối: khóa yêu cầu -> tính ưu đãi tốt nhất -> tạo đăng ký,
-- xếp lớp, khoản phải thu, cập nhật trạng thái yêu cầu.
-- Chưa giải quyết chính sách hủy đăng ký / nghỉ hẳn khác khóa; không tự hoàn tiền.
CREATE OR ALTER PROCEDURE dbo.usp_XepLopChinhThucTrungTam
 @MaYeuCau INT,@MaCoSo INT,@MaKhoaHoc INT,@MaLop INT,@MaTaiKhoan INT,
 @NgayDangKy DATE,@NgayBatDau DATE,@HocPhiThoaThuan DECIMAL(18,2),
 @MaGiaoDichNghiepVu VARCHAR(64)
AS
BEGIN
 SET NOCOUNT ON; SET XACT_ABORT ON;
 DECLARE @MaDangKy INT,@MaHocVien INT,@TongSoBuoi INT,@MaChuongTrinh INT,
         @SoTienGiam DECIMAL(18,2)=0,@NgayKetThucDuKien DATE=NULL;
 IF @HocPhiThoaThuan<0 THROW 51701,N'Học phí thỏa thuận không hợp lệ.',1;
 IF dbo.fn_CoQuyen(@MaTaiKhoan,@MaCoSo,'XEP_LOP')<>1
     THROW 51703,N'Tài khoản không được xếp lớp tại cơ sở.',1;
 BEGIN TRY
  SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
  BEGIN TRAN;
  SELECT @MaHocVien=y.MaHocVien
  FROM dbo.YeuCauXepLop y WITH (UPDLOCK,HOLDLOCK)
  JOIN dbo.CoSo c ON c.MaCoSo=y.MaCoSo AND c.LoaiCoSo='TRUNG_TAM'
  WHERE y.MaYeuCau=@MaYeuCau AND y.MaCoSo=@MaCoSo
    AND y.TrangThai='CHO_XEP_LOP' AND (y.MaKhoaHoc IS NULL OR y.MaKhoaHoc=@MaKhoaHoc);
  IF @MaHocVien IS NULL THROW 51704,N'Yêu cầu chưa sẵn sàng hoặc đã được xếp lớp.',1;
  IF EXISTS (SELECT 1 FROM dbo.DangKyKhoaHoc WHERE MaYeuCau=@MaYeuCau)
     THROW 51705,N'Yêu cầu này đã có đăng ký.',1;
  SELECT @TongSoBuoi=k.TongSoBuoi
  FROM dbo.KhoaHoc k JOIN dbo.LopTrungTam l
      ON l.MaKhoaHoc=k.MaKhoaHoc AND l.MaCoSo=k.MaCoSo
  WHERE k.MaKhoaHoc=@MaKhoaHoc AND k.MaCoSo=@MaCoSo
    AND l.MaLop=@MaLop AND k.TrangThai=1 AND l.TrangThai='DANG_MO';
  IF @TongSoBuoi IS NULL THROW 51706,N'Lớp không thuộc đúng khóa học và cơ sở.',1;
  SELECT TOP(1) @MaChuongTrinh=ct.MaChuongTrinh,
      @SoTienGiam=CASE WHEN ct.LoaiGiam='PHAN_TRAM' THEN ROUND(@HocPhiThoaThuan*ct.GiaTriGiam/100.00,2)
                  WHEN ct.GiaTriGiam>@HocPhiThoaThuan THEN @HocPhiThoaThuan
                  ELSE ct.GiaTriGiam END
  FROM dbo.GiamGiaKhoaHoc g
  JOIN dbo.ChuongTrinhGiamGia ct ON ct.MaChuongTrinh=g.MaChuongTrinh AND ct.MaCoSo=g.MaCoSo
  WHERE g.MaKhoaHoc=@MaKhoaHoc AND g.MaCoSo=@MaCoSo AND ct.TrangThai=1
    AND @NgayDangKy BETWEEN ct.TuNgay AND ct.DenNgay AND ct.PhamViApDung='KHOA_HOC'
  ORDER BY CASE WHEN ct.LoaiGiam='PHAN_TRAM' THEN ROUND(@HocPhiThoaThuan*ct.GiaTriGiam/100.00,2)
                WHEN ct.GiaTriGiam>@HocPhiThoaThuan THEN @HocPhiThoaThuan
                ELSE ct.GiaTriGiam END DESC,ct.MaChuongTrinh ASC;
  -- Dự kiến theo mẫu lịch học của lớp, bắt đầu từ ngày nhập học thực tế,
  -- không dựa vào số buổi lớp đã học trước đó. Không tính ngày nghỉ lễ đột xuất.
  ;WITH SoThuTu AS (
      SELECT TOP(2000) ROW_NUMBER() OVER(ORDER BY a.object_id,b.object_id)-1 AS N
      FROM sys.all_objects a CROSS JOIN sys.all_objects b
  ), NgayLich AS (
      SELECT DATEADD(DAY,CONVERT(INT,N),@NgayBatDau) AS NgayHoc FROM SoThuTu
  ), NgayHopLe AS (
      SELECT d.NgayHoc,ROW_NUMBER() OVER(ORDER BY d.NgayHoc) AS STT
      FROM NgayLich d
      WHERE EXISTS (
         SELECT 1 FROM dbo.LichHocLop l WHERE l.MaLop=@MaLop AND l.TrangThai='HIEU_LUC'
           AND l.ThuTrongTuan=(DATEDIFF(DAY,CONVERT(DATE,'19000101'),d.NgayHoc)%7)+2
           AND l.TuNgay<=d.NgayHoc AND (l.DenNgay IS NULL OR l.DenNgay>=d.NgayHoc)
      )
  )
  SELECT @NgayKetThucDuKien=NgayHoc FROM NgayHopLe WHERE STT=@TongSoBuoi;
  IF @NgayKetThucDuKien IS NULL THROW 51707,N'Lớp chưa có lịch hiệu lực đủ để dự kiến kết thúc khóa. Hãy cấu hình lịch trước.',1;
  INSERT dbo.DangKyKhoaHoc(MaHocVien,MaCoSo,MaKhoaHoc,MaYeuCau,NgayDangKy,NgayBatDau,
        NgayKetThucDuKien,TongSoBuoiDangKy,HocPhiThoaThuan,MaChuongTrinh,
        SoTienGiam,HocPhiSauGiam,TrangThai)
  VALUES(@MaHocVien,@MaCoSo,@MaKhoaHoc,@MaYeuCau,@NgayDangKy,@NgayBatDau,
         @NgayKetThucDuKien,@TongSoBuoi,@HocPhiThoaThuan,@MaChuongTrinh,
         @SoTienGiam,@HocPhiThoaThuan-@SoTienGiam,'DANG_HOC');
  SET @MaDangKy=CONVERT(INT,SCOPE_IDENTITY());
  INSERT dbo.XepLopTrungTam(MaDangKy,MaKhoaHoc,MaCoSo,MaLop,TuNgay,TrangThai)
  VALUES(@MaDangKy,@MaKhoaHoc,@MaCoSo,@MaLop,@NgayBatDau,'HIEU_LUC');
  INSERT dbo.KhoanPhaiThu(MaHocVien,MaCoSo,LoaiKhoan,MaDangKy,SoTienGoc,TrangThai)
  VALUES(@MaHocVien,@MaCoSo,'HOC_PHI_KHOA',@MaDangKy,@HocPhiThoaThuan-@SoTienGiam,'HIEU_LUC');
  UPDATE dbo.YeuCauXepLop SET TrangThai='HOAN_TAT',NgayHoanTat=SYSUTCDATETIME(),
      MaGiaoDichHoanTat=@MaGiaoDichNghiepVu WHERE MaYeuCau=@MaYeuCau;
  UPDATE dbo.KhachTiemNang SET TrangThai='DA_CHUYEN_DOI'
   WHERE MaKhach=(SELECT MaKhach FROM dbo.YeuCauXepLop WHERE MaYeuCau=@MaYeuCau);
  COMMIT;
  SELECT @MaDangKy AS MaDangKy,@TongSoBuoi AS TongSoBuoiDangKy,
         @NgayKetThucDuKien AS NgayKetThucDuKien,@SoTienGiam AS SoTienGiam;
 END TRY
 BEGIN CATCH
  IF XACT_STATE()<>0 ROLLBACK;
  THROW;
 END CATCH
END;
GO
-- <<< 06_TUYEN_SINH_TRUNG_TAM.sql

-- >>> 07_NGHIEP_VU_BO_SUNG.sql
USE [VINABACUS];
GO
-- One-screen preschool meal credit ledger. Does not create new tables.
CREATE OR ALTER VIEW dbo.v_SoDuTienAn
AS
SELECT MaHocVien,MaCoSo,
 CAST(SUM(CASE WHEN LoaiBienDong IN ('TAO','HOAN_TAC_SU_DUNG') THEN SoTien ELSE -SoTien END) AS DECIMAL(18,2)) AS SoDu
FROM dbo.PhatSinhTienAn GROUP BY MaHocVien,MaCoSo;
GO
-- Class schedule: UI supplies only course registration and actual lesson, no manual course/facility entry.
CREATE OR ALTER PROCEDURE dbo.usp_SapXepBuoiHoc
 @MaDangKy INT,@MaBuoiHoc INT,@LoaiSapXep VARCHAR(20),@MaTaiKhoan INT,
 @MaSapXepNguon INT=NULL
AS
BEGIN
 SET NOCOUNT ON; SET XACT_ABORT ON;
 DECLARE @MaHocVien INT,@MaCoSo INT,@NgayHoc DATE,@MaKhoaHoc INT,@MaNguon INT,
         @TrangThaiNguon VARCHAR(25),@MaKhoa INT,@NguonKhoa NVARCHAR(255),@MaSapXep INT;
 IF @LoaiSapXep NOT IN ('CHINH_THUC','DOI_BUOI','HOC_BO_SUNG','HOC_BU')
     THROW 54001,N'Loại xếp buổi không hợp lệ.',1;
 BEGIN TRY
  BEGIN TRAN;
  SELECT @MaHocVien=d.MaHocVien,@MaCoSo=d.MaCoSo,@MaKhoaHoc=d.MaKhoaHoc,@NgayHoc=b.NgayHoc
  FROM dbo.DangKyKhoaHoc d WITH (UPDLOCK,HOLDLOCK)
  JOIN dbo.BuoiHoc b ON b.MaBuoiHoc=@MaBuoiHoc AND b.TrangThai<>'HUY'
  JOIN dbo.LopTrungTam l ON l.MaLop=b.MaLop AND l.MaCoSo=d.MaCoSo AND l.MaKhoaHoc=d.MaKhoaHoc
  WHERE d.MaDangKy=@MaDangKy AND d.TrangThai='DANG_HOC' AND b.NgayHoc>=d.NgayBatDau;
  IF @MaHocVien IS NULL THROW 54002,N'Buổi học không đúng khóa/cơ sở của học viên hoặc trước ngày nhập học.',1;
  IF dbo.fn_CoQuyen(@MaTaiKhoan,@MaCoSo,'XEP_LOP')<>1
      THROW 54003,N'Không có quyền xếp lịch học.',1;
  SET @NguonKhoa=CONCAT('VINABACUS:LICH:',@MaCoSo,':',@MaHocVien);
  EXEC @MaKhoa=sys.sp_getapplock @Resource=@NguonKhoa,@LockMode='Exclusive',@LockOwner='Transaction',@LockTimeout=10000;
  IF @MaKhoa<0 THROW 54004,N'Không khóa được lịch học viên.',1;
  IF @LoaiSapXep IN ('DOI_BUOI','HOC_BU') AND @MaSapXepNguon IS NULL
      THROW 54005,N'Đổi buổi/học bù phải có buổi nguồn.',1;
  IF @MaSapXepNguon IS NOT NULL
  BEGIN
   SELECT @MaNguon=s.MaSapXep,@TrangThaiNguon=dd.TrangThai
   FROM dbo.SapXepBuoiHoc s WITH (UPDLOCK,HOLDLOCK)
   LEFT JOIN dbo.DiemDanhTrungTam dd ON dd.MaSapXep=s.MaSapXep
   WHERE s.MaSapXep=@MaSapXepNguon AND s.MaDangKy=@MaDangKy
     AND s.LoaiSapXep IN ('CHINH_THUC','DOI_BUOI','HOC_BO_SUNG');
   IF @MaNguon IS NULL THROW 54006,N'Buổi nguồn không thuộc đăng ký hoặc đã có nguồn sai loại.',1;
   IF @LoaiSapXep='HOC_BU' AND @TrangThaiNguon<>'VANG_CO_PHEP'
       THROW 54007,N'Chỉ vắng có phép mới có thể học bù.',1;
   IF @LoaiSapXep='DOI_BUOI' AND (@TrangThaiNguon IS NOT NULL AND @TrangThaiNguon<>'CHUA_DIEM_DANH')
       THROW 54008,N'Không đổi buổi đã điểm danh.',1;
  END;
  -- Chính sách chỉ giới hạn 3 buổi học bù HOÀN THÀNH, không tính lần đặt lịch chưa học.
  IF EXISTS (SELECT 1 FROM dbo.SapXepBuoiHoc WITH (UPDLOCK,HOLDLOCK)
             WHERE MaHocVien=@MaHocVien AND MaBuoiHoc=@MaBuoiHoc AND TrangThai IN ('DA_XEP','HOAN_THANH'))
       THROW 54010,N'Học viên đã có tên trong buổi này.',1;
  IF @LoaiSapXep='DOI_BUOI'
      UPDATE dbo.SapXepBuoiHoc SET TrangThai='HUY' WHERE MaSapXep=@MaSapXepNguon AND TrangThai='DA_XEP';
  INSERT dbo.SapXepBuoiHoc(MaDangKy,MaHocVien,MaBuoiHoc,LoaiSapXep,MaSapXepNguon,TrangThai)
  VALUES(@MaDangKy,@MaHocVien,@MaBuoiHoc,@LoaiSapXep,@MaSapXepNguon,'DA_XEP');
  SET @MaSapXep=CONVERT(INT,SCOPE_IDENTITY());
  COMMIT;
  SELECT @MaSapXep AS MaSapXep;
 END TRY
 BEGIN CATCH
  IF XACT_STATE()<>0 ROLLBACK;
  THROW;
 END CATCH
END;
GO
-- Teacher can only mark a lesson they are assigned to; admins/leaders may mark within branch.
CREATE OR ALTER PROCEDURE dbo.usp_GhiDiemDanhTrungTam
 @MaSapXep INT,@MaTaiKhoan INT,@TrangThai VARCHAR(25),@GhiChu NVARCHAR(500)=NULL
AS
BEGIN
 SET NOCOUNT ON; SET XACT_ABORT ON;
 DECLARE @MaCoSo INT,@MaDangKy INT,@MaGiaoVien INT,@MaTaiKhoanNhanVien INT,
 @LoaiSapXep VARCHAR(20),@MaKhoa INT,@TaiNguyen NVARCHAR(255);
 IF @TrangThai NOT IN ('CO_MAT','VANG_CO_PHEP','VANG_KHONG_PHEP','CHUA_DIEM_DANH')
     THROW 54101,N'Trạng thái điểm danh không hợp lệ.',1;
 BEGIN TRY
  BEGIN TRAN;
  SELECT @MaCoSo=d.MaCoSo,@MaDangKy=d.MaDangKy,@MaGiaoVien=l.MaGiaoVien,
         @LoaiSapXep=s.LoaiSapXep
  FROM dbo.SapXepBuoiHoc s WITH (UPDLOCK,HOLDLOCK)
  JOIN dbo.DangKyKhoaHoc d ON d.MaDangKy=s.MaDangKy
  JOIN dbo.BuoiHoc b ON b.MaBuoiHoc=s.MaBuoiHoc AND b.TrangThai<>'HUY'
  JOIN dbo.LopTrungTam l ON l.MaLop=b.MaLop AND l.MaCoSo=d.MaCoSo AND l.MaKhoaHoc=d.MaKhoaHoc
  WHERE s.MaSapXep=@MaSapXep AND s.TrangThai<>'HUY';
  IF @MaCoSo IS NULL THROW 54102,N'Buổi xếp không hợp lệ.',1;
  IF dbo.fn_CoQuyen(@MaTaiKhoan,@MaCoSo,'DIEM_DANH')<>1
       THROW 54103,N'Không có quyền điểm danh.',1;
  SELECT @MaTaiKhoanNhanVien=MaNhanVien FROM dbo.TaiKhoan WHERE MaTaiKhoan=@MaTaiKhoan;
  IF EXISTS (SELECT 1 FROM dbo.TaiKhoanCoSo ac
            JOIN dbo.VaiTro v ON v.MaVaiTro=ac.MaVaiTro
            WHERE ac.MaTaiKhoan=@MaTaiKhoan AND ac.MaCoSo=@MaCoSo
              AND v.MaVaiTroChu='GIAO_VIEN')
      AND (@MaGiaoVien IS NULL OR @MaGiaoVien<>@MaTaiKhoanNhanVien)
      THROW 54104,N'Giáo viên không phụ trách lớp này.',1;
  SET @TaiNguyen=CONCAT('VINABACUS:DIEM_DANH:',@MaDangKy);
  EXEC @MaKhoa=sys.sp_getapplock @Resource=@TaiNguyen,@LockMode='Exclusive',@LockOwner='Transaction',@LockTimeout=10000;
  IF @MaKhoa<0 THROW 54105,N'Không khóa được buổi điểm danh.',1;
  IF @LoaiSapXep='HOC_BU' AND @TrangThai='CO_MAT'
     AND (SELECT COUNT(*) FROM dbo.SapXepBuoiHoc s
           JOIN dbo.DiemDanhTrungTam dd ON dd.MaSapXep=s.MaSapXep
           WHERE s.MaDangKy=@MaDangKy AND s.MaSapXep<>@MaSapXep
             AND s.LoaiSapXep='HOC_BU' AND s.TrangThai<>'HUY' AND dd.TrangThai='CO_MAT')>=3
      THROW 54106,N'Đã hoàn thành ba buổi học bù của đăng ký.',1;
  IF EXISTS(SELECT 1 FROM dbo.DiemDanhTrungTam WHERE MaSapXep=@MaSapXep)
     UPDATE dbo.DiemDanhTrungTam SET TrangThai=@TrangThai,GhiChu=@GhiChu,
           MaTaiKhoan=@MaTaiKhoan,NgayCapNhat=SYSUTCDATETIME() WHERE MaSapXep=@MaSapXep;
  ELSE
     INSERT dbo.DiemDanhTrungTam(MaSapXep,TrangThai,GhiChu,MaTaiKhoan)
     VALUES(@MaSapXep,@TrangThai,@GhiChu,@MaTaiKhoan);
  UPDATE dbo.SapXepBuoiHoc SET TrangThai=CASE WHEN @TrangThai='CHUA_DIEM_DANH' THEN 'DA_XEP' ELSE 'HOAN_THANH' END
     WHERE MaSapXep=@MaSapXep;
  COMMIT;
 END TRY
 BEGIN CATCH
  IF XACT_STATE()<>0 ROLLBACK;
  THROW;
 END CATCH
END;
GO
-- Monthly preschool attendance. Only Admin may correct an already closed month.
CREATE OR ALTER PROCEDURE dbo.usp_GhiDiemDanhMamNon
 @MaHocVien INT,@MaXepLop INT,@NgayHoc DATE,@TrangThai VARCHAR(25),
 @MaTaiKhoan INT,@GhiChu NVARCHAR(500)=NULL,@LyDoSuaSauChot NVARCHAR(500)=NULL
AS
BEGIN
 SET NOCOUNT ON; SET XACT_ABORT ON;
 DECLARE @MaCoSo INT,@MaLop INT,@MaThang INT,@TrangThaiKy VARCHAR(15),
 @MaDiemDanh INT,@TrangThaiCu VARCHAR(25),@GhiChuCu NVARCHAR(500),
 @MaNhanVien INT,@MaKhoa INT,@TaiNguyen NVARCHAR(255);
 IF @TrangThai NOT IN ('CHUA_DIEM_DANH','CO_MAT','VANG_CO_PHEP','VANG_KHONG_PHEP')
    THROW 54201,N'Trạng thái điểm danh không hợp lệ.',1;
 BEGIN TRY
  BEGIN TRAN;
  SELECT @MaCoSo=x.MaCoSo,@MaLop=x.MaLop
  FROM dbo.XepLopMamNon x WITH (UPDLOCK,HOLDLOCK)
  WHERE x.MaXepLop=@MaXepLop AND x.MaHocVien=@MaHocVien AND x.TrangThai<>'HUY'
    AND @NgayHoc>=x.TuNgay AND (@NgayHoc<=x.DenNgay OR x.DenNgay IS NULL);
  IF @MaCoSo IS NULL THROW 54202,N'Không có xếp lớp hợp lệ trong ngày.',1;
  IF (DATEDIFF(DAY,CONVERT(DATE,'19000101'),@NgayHoc)%7)=6
      OR EXISTS(SELECT 1 FROM dbo.NgayNghiChung WHERE MaCoSo=@MaCoSo AND NgayNghi=@NgayHoc)
      THROW 54210,N'Không điểm danh ngày Chủ nhật hoặc ngày nghỉ chung.',1;
  SELECT @MaThang=MaThang,@TrangThaiKy=TrangThai FROM dbo.ThangMamNon WITH (UPDLOCK,HOLDLOCK)
  WHERE MaCoSo=@MaCoSo AND Nam=YEAR(@NgayHoc) AND Thang=MONTH(@NgayHoc);
  IF @MaThang IS NULL THROW 54203,N'Chưa thiết lập kỳ Mầm non cho ngày này.',1;
  IF dbo.fn_CoQuyen(@MaTaiKhoan,@MaCoSo,'DIEM_DANH')<>1
      THROW 54204,N'Không có quyền điểm danh.',1;
  SELECT @MaNhanVien=MaNhanVien FROM dbo.TaiKhoan WHERE MaTaiKhoan=@MaTaiKhoan;
  IF EXISTS (SELECT 1 FROM dbo.TaiKhoanCoSo ac JOIN dbo.VaiTro v ON v.MaVaiTro=ac.MaVaiTro
             WHERE ac.MaTaiKhoan=@MaTaiKhoan AND ac.MaCoSo=@MaCoSo AND v.MaVaiTroChu='GIAO_VIEN')
     AND NOT EXISTS(SELECT 1 FROM dbo.PhanCongGiaoVienMamNon p WHERE p.MaLop=@MaLop
                    AND p.MaCoSo=@MaCoSo AND p.MaNhanVien=@MaNhanVien
                    AND p.TuNgay<=@NgayHoc AND (p.DenNgay IS NULL OR p.DenNgay>=@NgayHoc))
      THROW 54205,N'Giáo viên không phụ trách lớp trong ngày này.',1;
  SET @TaiNguyen=CONCAT('VINABACUS:MN_DIEM_DANH:',@MaCoSo,':',@MaHocVien,':',CONVERT(CHAR(8),@NgayHoc,112));
  EXEC @MaKhoa=sys.sp_getapplock @Resource=@TaiNguyen,@LockMode='Exclusive',@LockOwner='Transaction',@LockTimeout=10000;
  IF @MaKhoa<0 THROW 54206,N'Không khóa được ngày điểm danh.',1;
  SELECT @MaDiemDanh=MaDiemDanh,@TrangThaiCu=TrangThai,@GhiChuCu=GhiChu
  FROM dbo.DiemDanhMamNon WITH (UPDLOCK,HOLDLOCK)
  WHERE MaHocVien=@MaHocVien AND MaCoSo=@MaCoSo AND NgayHoc=@NgayHoc;
  IF @TrangThaiKy='CHOT'
  BEGIN
    IF dbo.fn_CoQuyen(@MaTaiKhoan,@MaCoSo,'SUA_DIEM_DANH_DA_CHOT')<>1
       THROW 54207,N'Chỉ Admin được sửa sau chốt.',1;
    IF @LyDoSuaSauChot IS NULL OR LEN(LTRIM(RTRIM(@LyDoSuaSauChot)))=0
       THROW 54208,N'Phải nhập lý do sửa điểm danh sau chốt.',1;
    IF @MaDiemDanh IS NULL THROW 54209,N'Không thể thêm bản ghi mới vào tháng đã chốt.',1;
    IF @TrangThaiCu<>@TrangThai OR ISNULL(@GhiChuCu,N'')<>ISNULL(@GhiChu,N'')
    BEGIN
      INSERT dbo.LichSuSuaDiemDanhMamNon(MaDiemDanh,TrangThaiTruoc,TrangThaiSau,GhiChuTruoc,GhiChuSau,LyDo,MaTaiKhoan)
      VALUES(@MaDiemDanh,@TrangThaiCu,@TrangThai,@GhiChuCu,@GhiChu,@LyDoSuaSauChot,@MaTaiKhoan);
    END;
  END;
  IF @MaDiemDanh IS NULL
      INSERT dbo.DiemDanhMamNon(MaHocVien,MaCoSo,MaXepLop,NgayHoc,TrangThai,GhiChu,MaTaiKhoanDiemDanh)
      VALUES(@MaHocVien,@MaCoSo,@MaXepLop,@NgayHoc,@TrangThai,@GhiChu,@MaTaiKhoan);
  ELSE
      UPDATE dbo.DiemDanhMamNon SET TrangThai=@TrangThai,GhiChu=@GhiChu,
          MaTaiKhoanDiemDanh=@MaTaiKhoan,ThoiDiemCapNhat=SYSUTCDATETIME()
      WHERE MaDiemDanh=@MaDiemDanh;
  COMMIT;
 END TRY
 BEGIN CATCH
  IF XACT_STATE()<>0 ROLLBACK;
  THROW;
 END CATCH
END;
GO
-- Excused-absence meal credit created only once, value frozen from charged food line.
CREATE OR ALTER PROCEDURE dbo.usp_TaoTinDungTienAnVangCoPhep
 @MaDiemDanh INT,@MaTaiKhoan INT,@MaGiaoDichNghiepVu VARCHAR(64)
AS
BEGIN
 SET NOCOUNT ON; SET XACT_ABORT ON;
 DECLARE @MaHocVien INT,@MaCoSo INT,@MaBangTinh INT,@SoTien DECIMAL(18,2),
 @MaKhoa INT,@TaiNguyen NVARCHAR(255),@SoDong INT;
 BEGIN TRY
  BEGIN TRAN;
  SELECT @MaHocVien=d.MaHocVien,@MaCoSo=d.MaCoSo
  FROM dbo.DiemDanhMamNon d WITH (UPDLOCK,HOLDLOCK)
  WHERE d.MaDiemDanh=@MaDiemDanh AND d.TrangThai='VANG_CO_PHEP';
  IF @MaHocVien IS NULL THROW 54301,N'Chỉ vắng có phép được khấu trừ tiền ăn.',1;
  IF dbo.fn_CoQuyen(@MaTaiKhoan,@MaCoSo,'DIEU_CHINH_HOC_PHI')<>1
      THROW 54302,N'Không có quyền ghi nhận tiền ăn.',1;
  SET @TaiNguyen=CONCAT('VINABACUS:TAI_CHINH:',@MaCoSo,':',@MaHocVien);
  EXEC @MaKhoa=sys.sp_getapplock @Resource=@TaiNguyen,@LockMode='Exclusive',@LockOwner='Transaction',@LockTimeout=10000;
  IF @MaKhoa<0 THROW 54303,N'Không khóa được số dư tiền ăn.',1;
  SELECT @SoDong=COUNT(*),@MaBangTinh=MAX(ct.MaBangTinh),@SoTien=MAX(ct.DonGiaApDung)
  FROM dbo.ChiTietTienAnMamNon ct
  JOIN dbo.DiemDanhMamNon d ON d.MaDiemDanh=@MaDiemDanh AND d.MaHocVien=ct.MaHocVien
       AND d.MaCoSo=ct.MaCoSo AND d.MaXepLop=ct.MaXepLop
       AND d.NgayHoc BETWEEN ct.TuNgay AND ct.DenNgay;
  IF @SoDong<>1 OR @SoTien IS NULL OR @SoTien<=0
      THROW 54304,N'Không tìm thấy đúng một khoản tiền ăn đã tính cho ngày vắng.',1;
  IF EXISTS(SELECT 1 FROM dbo.PhatSinhTienAn WHERE MaDiemDanh=@MaDiemDanh AND LoaiBienDong='TAO')
      THROW 54305,N'Ngày vắng đã được tạo tín dụng.',1;
  INSERT dbo.PhatSinhTienAn(MaHocVien,MaCoSo,LoaiBienDong,SoTien,MaDiemDanh,MaBangTinh,
                            MaGiaoDichNghiepVu,MaTaiKhoan,LyDo)
  VALUES(@MaHocVien,@MaCoSo,'TAO',@SoTien,@MaDiemDanh,@MaBangTinh,
         @MaGiaoDichNghiepVu,@MaTaiKhoan,N'Vắng có phép được trừ tiền ăn');
  COMMIT;
  SELECT @SoTien AS TienAnBaoLuu;
 END TRY
 BEGIN CATCH
  IF XACT_STATE()<>0 ROLLBACK;
  THROW;
 END CATCH
END;
GO
CREATE OR ALTER PROCEDURE dbo.usp_SuDungTinDungTienAn
 @MaHocVien INT,@MaCoSo INT,@MaKhoanThuDich INT,@SoTien DECIMAL(18,2),
 @MaTaiKhoan INT,@MaGiaoDichNghiepVu VARCHAR(64)
AS
BEGIN
 SET NOCOUNT ON; SET XACT_ABORT ON;
 DECLARE @SoDu DECIMAL(18,2),@CongNo DECIMAL(18,2),@MaKhoa INT,@TaiNguyen NVARCHAR(255);
 IF @SoTien<=0 THROW 54401,N'Số tiền phải lớn hơn 0.',1;
 IF dbo.fn_CoQuyen(@MaTaiKhoan,@MaCoSo,'THU_HOC_PHI')<>1 THROW 54402,N'Không có quyền thu tiền.',1;
 BEGIN TRY
  BEGIN TRAN;
  SET @TaiNguyen=CONCAT('VINABACUS:TAI_CHINH:',@MaCoSo,':',@MaHocVien);
  EXEC @MaKhoa=sys.sp_getapplock @Resource=@TaiNguyen,@LockMode='Exclusive',@LockOwner='Transaction',@LockTimeout=10000;
  IF @MaKhoa<0 THROW 54403,N'Không khóa được tài chính học viên.',1;
  IF NOT EXISTS (SELECT 1 FROM dbo.KhoanPhaiThu WITH (UPDLOCK,HOLDLOCK)
    WHERE MaKhoanThu=@MaKhoanThuDich AND MaHocVien=@MaHocVien AND MaCoSo=@MaCoSo
      AND LoaiKhoan='TIEN_AN' AND TrangThai='HIEU_LUC')
     THROW 54404,N'Khoản đích không phải tiền ăn của học viên.',1;
  SELECT @SoDu=COALESCE(SUM(CASE WHEN LoaiBienDong IN ('TAO','HOAN_TAC_SU_DUNG') THEN SoTien ELSE -SoTien END),0)
  FROM dbo.PhatSinhTienAn WITH (UPDLOCK,HOLDLOCK)
  WHERE MaHocVien=@MaHocVien AND MaCoSo=@MaCoSo;
  SELECT @CongNo=CongNoConLai FROM dbo.v_CongNoKhoanThu WHERE MaKhoanThu=@MaKhoanThuDich;
  IF @SoDu<@SoTien THROW 54405,N'Không đủ tín dụng tiền ăn.',1;
  IF @CongNo<@SoTien THROW 54406,N'Không thể trừ vượt tiền ăn cần đóng.',1;
  IF EXISTS(SELECT 1 FROM dbo.PhatSinhTienAn WHERE MaGiaoDichNghiepVu=@MaGiaoDichNghiepVu)
     THROW 54407,N'Giao dịch tiền ăn đã tồn tại.',1;
  INSERT dbo.PhatSinhTienAn(MaHocVien,MaCoSo,LoaiBienDong,SoTien,MaKhoanThuDich,
              MaGiaoDichNghiepVu,MaTaiKhoan,LyDo)
  VALUES(@MaHocVien,@MaCoSo,'SU_DUNG',@SoTien,@MaKhoanThuDich,
         @MaGiaoDichNghiepVu,@MaTaiKhoan,N'Khấu trừ tiền ăn bảo lưu');
  COMMIT;
 END TRY
 BEGIN CATCH
  IF XACT_STATE()<>0 ROLLBACK;
  THROW;
 END CATCH
END;
GO
CREATE OR ALTER PROCEDURE dbo.usp_HoanTienAn
 @MaHocVien INT,@MaCoSo INT,@SoTien DECIMAL(18,2),
 @MaTaiKhoan INT,@MaGiaoDichNghiepVu VARCHAR(64),@LyDo NVARCHAR(500)
AS
BEGIN
 SET NOCOUNT ON; SET XACT_ABORT ON;
 DECLARE @SoDu DECIMAL(18,2),@MaPhieuHoan INT,@MaKhoa INT,@TaiNguyen NVARCHAR(255);
 IF @SoTien<=0 THROW 54501,N'Số tiền hoàn phải lớn hơn 0.',1;
 IF @LyDo IS NULL OR LEN(LTRIM(RTRIM(@LyDo)))=0 THROW 54502,N'Phải có lý do hoàn tiền.',1;
 IF dbo.fn_CoQuyen(@MaTaiKhoan,@MaCoSo,'HOAN_TIEN_AN')<>1
    THROW 54503,N'Chỉ Admin được hoàn tiền ăn.',1;
 BEGIN TRY
  BEGIN TRAN;
  SET @TaiNguyen=CONCAT('VINABACUS:TAI_CHINH:',@MaCoSo,':',@MaHocVien);
  EXEC @MaKhoa=sys.sp_getapplock @Resource=@TaiNguyen,@LockMode='Exclusive',@LockOwner='Transaction',@LockTimeout=10000;
  IF @MaKhoa<0 THROW 54504,N'Không khóa được tiền ăn.',1;
  SELECT @SoDu=COALESCE(SUM(CASE WHEN LoaiBienDong IN ('TAO','HOAN_TAC_SU_DUNG') THEN SoTien ELSE -SoTien END),0)
  FROM dbo.PhatSinhTienAn WITH (UPDLOCK,HOLDLOCK)
  WHERE MaHocVien=@MaHocVien AND MaCoSo=@MaCoSo;
  IF @SoDu<@SoTien THROW 54505,N'Hoàn vượt tiền ăn dư.',1;
  IF EXISTS(SELECT 1 FROM dbo.PhieuHoanTienAn WHERE MaGiaoDichNghiepVu=@MaGiaoDichNghiepVu)
       THROW 54506,N'Phiếu hoàn đã tồn tại.',1;
  INSERT dbo.PhieuHoanTienAn(MaHocVien,MaCoSo,SoTienHoan,LyDo,MaTaiKhoan,TrangThai,MaGiaoDichNghiepVu)
  VALUES(@MaHocVien,@MaCoSo,@SoTien,@LyDo,@MaTaiKhoan,'XAC_NHAN',@MaGiaoDichNghiepVu);
  SET @MaPhieuHoan=CONVERT(INT,SCOPE_IDENTITY());
  INSERT dbo.PhatSinhTienAn(MaHocVien,MaCoSo,LoaiBienDong,SoTien,MaPhieuHoan,
                 MaGiaoDichNghiepVu,MaTaiKhoan,LyDo)
  VALUES(@MaHocVien,@MaCoSo,'HOAN',@SoTien,@MaPhieuHoan,
         CONVERT(VARCHAR(64),HASHBYTES('SHA2_256',CONCAT(@MaGiaoDichNghiepVu,':HOAN')),2),@MaTaiKhoan,@LyDo);
  COMMIT;
  SELECT @MaPhieuHoan AS MaPhieuHoan;
 END TRY
 BEGIN CATCH
  IF XACT_STATE()<>0 ROLLBACK;
  THROW;
 END CATCH
END;
GO
-- Chot only after billed students have both canonical fee sources; calculation is done in Java business layer.
CREATE OR ALTER PROCEDURE dbo.usp_ChotThangMamNon
 @MaThang INT,@MaTaiKhoan INT
AS
BEGIN
 SET NOCOUNT ON; SET XACT_ABORT ON;
 DECLARE @MaCoSo INT,@MaKhoa INT,@TaiNguyen NVARCHAR(255);
 BEGIN TRY
  BEGIN TRAN;
  SELECT @MaCoSo=MaCoSo FROM dbo.ThangMamNon WITH (UPDLOCK,HOLDLOCK)
  WHERE MaThang=@MaThang AND TrangThai='MO';
  IF @MaCoSo IS NULL THROW 54601,N'Kỳ không tồn tại hoặc đã chốt.',1;
  IF dbo.fn_CoQuyen(@MaTaiKhoan,@MaCoSo,'CHOT_THANG_MAM_NON')<>1
     THROW 54602,N'Không có quyền chốt tháng.',1;
  SET @TaiNguyen=CONCAT('VINABACUS:CHOT_MN:',@MaThang);
  EXEC @MaKhoa=sys.sp_getapplock @Resource=@TaiNguyen,@LockMode='Exclusive',@LockOwner='Transaction',@LockTimeout=10000;
  IF @MaKhoa<0 THROW 54603,N'Không khóa được tháng Mầm non.',1;
  IF EXISTS (SELECT 1 FROM dbo.BangTinhPhiMamNon b
            OUTER APPLY (SELECT SUM(ThanhTien) AS T FROM dbo.ChiTietHocPhiMamNon WHERE MaBangTinh=b.MaBangTinh) h
            OUTER APPLY (SELECT SUM(ThanhTien) AS T FROM dbo.ChiTietTienAnMamNon WHERE MaBangTinh=b.MaBangTinh) a
            WHERE b.MaThang=@MaThang AND
                  (b.TrangThai<>'CHOT' OR b.TongHocPhi<>ISNULL(h.T,0)
                   OR b.TongTienAn<>ISNULL(a.T,0)
                   OR (SELECT COUNT(*) FROM dbo.KhoanPhaiThu k
                       WHERE k.MaBangTinh=b.MaBangTinh AND k.TrangThai='HIEU_LUC'
                         AND k.LoaiKhoan IN ('HOC_PHI_THANG','TIEN_AN'))<>2))
       THROW 54604,N'Có bảng tính chưa chốt hoặc số liệu/chứng từ chưa khớp.',1;
  IF EXISTS (SELECT 1 FROM dbo.BangTinhPhiMamNon b
             JOIN dbo.KhoanPhaiThu k ON k.MaBangTinh=b.MaBangTinh AND k.TrangThai='HIEU_LUC'
             WHERE b.MaThang=@MaThang AND
                ((k.LoaiKhoan='HOC_PHI_THANG' AND k.SoTienGoc<>b.TongHocPhi) OR
                 (k.LoaiKhoan='TIEN_AN' AND k.SoTienGoc<>b.TongTienAn)))
      THROW 54606,N'Số tiền gốc trên khoản thu không khớp bảng phí tháng.',1;
  IF NOT EXISTS(SELECT 1 FROM dbo.BangTinhPhiMamNon WHERE MaThang=@MaThang)
       THROW 54605,N'Chưa có bảng phí nào trong tháng.',1;
  UPDATE dbo.ThangMamNon SET TrangThai='CHOT',NgayChot=SYSUTCDATETIME(),MaNguoiChot=@MaTaiKhoan
  WHERE MaThang=@MaThang;
  COMMIT;
 END TRY
 BEGIN CATCH
  IF XACT_STATE()<>0 ROLLBACK;
  THROW;
 END CATCH
END;
GO
-- <<< 07_NGHIEP_VU_BO_SUNG.sql

-- >>> 08_KIEM_TRA_CAU_TRUC.sql
USE [VINABACUS];
GO
SET NOCOUNT ON;
IF (SELECT COUNT(*) FROM sys.tables WHERE schema_id=SCHEMA_ID(N'dbo'))<>57
 THROW 55001,N'Số bảng không bằng 57. Đây chỉ là bộ cài mới.',1;
IF EXISTS(SELECT 1 FROM sys.foreign_keys WHERE is_not_trusted=1 OR is_disabled=1)
 THROW 55002,N'Có khóa ngoại bị tắt hoặc chưa xác thực.',1;
IF (SELECT COUNT(*) FROM sys.foreign_keys WHERE parent_object_id IN (SELECT object_id FROM sys.tables WHERE schema_id=SCHEMA_ID(N'dbo')))<137
 THROW 55003,N'Thiếu khóa ngoại tăng cường.',1;
IF EXISTS(SELECT 1 FROM dbo.v_CongNoKhoanThu WHERE NghiaVu<0 OR ThuVuotChuaDoiChieu>0)
 THROW 55004,N'Có nghĩa vụ âm hoặc thu vượt chưa đối chiếu.',1;
IF EXISTS(SELECT 1 FROM dbo.v_SoDuTinDungHocPhi WHERE SoDu<0)
 THROW 55005,N'Có tín dụng học phí âm.',1;
IF EXISTS(SELECT 1 FROM dbo.v_SoDuTienAn WHERE SoDu<0)
 THROW 55006,N'Có tín dụng tiền ăn âm.',1;
IF EXISTS(SELECT 1 FROM dbo.v_TonKhoHienTai WHERE TonHienTai<0)
 THROW 55007,N'Có tồn kho âm.',1;
IF EXISTS (
 SELECT 1 FROM dbo.SapXepBuoiHoc s JOIN dbo.DangKyKhoaHoc d ON d.MaDangKy=s.MaDangKy
 JOIN dbo.BuoiHoc b ON b.MaBuoiHoc=s.MaBuoiHoc JOIN dbo.LopTrungTam l ON l.MaLop=b.MaLop
 WHERE l.MaKhoaHoc<>d.MaKhoaHoc OR l.MaCoSo<>d.MaCoSo
) THROW 55008,N'Có buổi xếp sai khóa/cơ sở.',1;
IF EXISTS(
 SELECT 1 FROM dbo.ChiTietPhieuThu r JOIN dbo.ChiTietPhieuThu p ON p.MaChiTietPhieuThu=r.MaChiTietNguon
 WHERE r.LoaiBienDong='HOAN_TAC_PHAN_BO' AND (p.LoaiBienDong<>'PHAN_BO'
 OR p.MaKhoanThu<>r.MaKhoanThu OR p.MaPhieuThu<>r.MaPhieuThu)
) THROW 55009,N'Hoàn tác sai dòng nguồn.',1;
IF EXISTS(
 SELECT 1 FROM dbo.ChiTietPhieuThu p OUTER APPLY (
 SELECT COALESCE(SUM(r.SoTienPhanBo),0) AS DaHoanTac FROM dbo.ChiTietPhieuThu r
 WHERE r.MaChiTietNguon=p.MaChiTietPhieuThu AND r.LoaiBienDong='HOAN_TAC_PHAN_BO') x
 WHERE p.LoaiBienDong='PHAN_BO' AND x.DaHoanTac>p.SoTienPhanBo
) THROW 55010,N'Có hoàn tác vượt tiền phân bổ gốc.',1;
IF EXISTS (
 SELECT 1 FROM dbo.DiemDanhTrungTam dd JOIN dbo.SapXepBuoiHoc s ON s.MaSapXep=dd.MaSapXep
 WHERE s.LoaiSapXep='HOC_BU' AND dd.TrangThai='CO_MAT' AND s.TrangThai='HUY'
) THROW 55011,N'Có điểm danh học bù đã hủy.',1;
PRINT N'Các kiểm tra cấu trúc và bất biến cơ bản: PASS (không thay thế kiểm thử kịch bản).';
SELECT DB_NAME() AS CoSoDuLieu,(SELECT COUNT(*) FROM sys.tables WHERE schema_id=SCHEMA_ID(N'dbo')) AS SoBang,
 (SELECT COUNT(*) FROM sys.foreign_keys) AS SoKhoaNgoai,
 (SELECT COUNT(*) FROM sys.procedures WHERE schema_id=SCHEMA_ID(N'dbo')) AS SoThuTuc;
GO
-- <<< 08_KIEM_TRA_CAU_TRUC.sql

-- >>> 09_TAI_KHOAN_PHAN_QUYEN.sql
USE [VINABACUS];
GO
-- Tai khoan dang nhap bang TenDangNhap + mat khau duoc bam tai ung dung (Argon2id/bcrypt).
-- Khong tao tai khoan cong khai; khong luu mat khau ro.
-- MaTaiKhoanThucHien la tham so NGHIEP VU, khong phai danh tinh SQL Server da chung thuc.
-- CHI cap EXECUTE cho ket noi/role tin cay, va xac thuc nguoi goi qua tang may chu hoac
-- SQL principal rieng theo nguoi su dung; dung shared JDBC login va tin @MaTaiKhoanThucHien la KHONG AN TOAN.

CREATE OR ALTER PROCEDURE dbo.usp_TaoTaiKhoan
    @MaTaiKhoanThucHien INT,
    @MaCoSo INT,
    @MaNhanVien INT,
    @TenDangNhap VARCHAR(100),
    @MatKhauBam VARCHAR(255),
    @MaVaiTroChu VARCHAR(30) = 'GIAO_VIEN'
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;
    IF @TenDangNhap IS NULL OR LEN(LTRIM(RTRIM(@TenDangNhap))) < 3
        THROW 56101, N'Ten dang nhap phai co it nhat 3 ky tu.',1;
    IF @MatKhauBam IS NULL OR NOT
       (@MatKhauBam LIKE '$argon2id$%' OR @MatKhauBam LIKE '$2a$%' OR
        @MatKhauBam LIKE '$2b$%' OR @MatKhauBam LIKE '$2y$%')
        THROW 56102,N'Mat khau phai bam tai ung dung bang Argon2id hoac bcrypt.',1;
    IF @MaVaiTroChu NOT IN ('ADMIN','TO_TRUONG','GIAO_VIEN')
        THROW 56103,N'Vai tro khong hop le.',1;
    BEGIN TRY
        BEGIN TRAN;
        -- Buoc kiem tra nay phai duoc ket hop voi dinh danh ket noi da chung thuc.
        DECLARE @VaiTroNguoiTao VARCHAR(30);
        SELECT @VaiTroNguoiTao=v.MaVaiTroChu
        FROM dbo.TaiKhoan t WITH (UPDLOCK,HOLDLOCK)
        JOIN dbo.TaiKhoanCoSo tc WITH (UPDLOCK,HOLDLOCK) ON tc.MaTaiKhoan=t.MaTaiKhoan
        JOIN dbo.VaiTro v ON v.MaVaiTro=tc.MaVaiTro
        WHERE t.MaTaiKhoan=@MaTaiKhoanThucHien AND tc.MaCoSo=@MaCoSo
          AND t.TrangThai=1 AND tc.TrangThai=1;
        IF @VaiTroNguoiTao NOT IN ('ADMIN','TO_TRUONG')
            THROW 56104,N'Chi Admin va To truong duoc uy quyen duoc tao tai khoan.',1;
        IF @VaiTroNguoiTao='TO_TRUONG' AND @MaVaiTroChu<>'GIAO_VIEN'
            THROW 56109,N'To truong chi duoc tao tai khoan Giao vien.',1;
        IF dbo.fn_CoQuyen(@MaTaiKhoanThucHien,@MaCoSo,'TAO_TAI_KHOAN')<>1
            THROW 56110,N'Tai khoan khong co quyen tao tai khoan.',1;
        IF NOT EXISTS (
            SELECT 1 FROM dbo.NhanVien nv
            JOIN dbo.NhanVienCoSo nvc ON nvc.MaNhanVien=nv.MaNhanVien
            WHERE nv.MaNhanVien=@MaNhanVien AND nvc.MaCoSo=@MaCoSo
              AND nv.TrangThai=1 AND nvc.TrangThai=1
        ) THROW 56105,N'Nhan vien chua duoc gan vao co so nay.',1;
        IF EXISTS (SELECT 1 FROM dbo.TaiKhoan WHERE MaNhanVien=@MaNhanVien)
            THROW 56106,N'Nhan vien da co tai khoan. Gan vao co so bang chuc nang rieng.',1;
        IF EXISTS (SELECT 1 FROM dbo.TaiKhoan WHERE TenDangNhap=LTRIM(RTRIM(@TenDangNhap)))
            THROW 56107,N'Ten dang nhap da duoc su dung.',1;
        DECLARE @MaVaiTro INT=(SELECT MaVaiTro FROM dbo.VaiTro WHERE MaVaiTroChu=@MaVaiTroChu);
        IF @MaVaiTro IS NULL THROW 56108,N'Vai tro chua duoc khoi tao.',1;
        INSERT dbo.TaiKhoan(MaNhanVien,TenDangNhap,MatKhauBam,PhaiDoiMatKhau)
        VALUES (@MaNhanVien,LTRIM(RTRIM(@TenDangNhap)),@MatKhauBam,1);
        DECLARE @MaTaiKhoanMoi INT=CONVERT(INT,SCOPE_IDENTITY());
        INSERT dbo.TaiKhoanCoSo(MaTaiKhoan,MaNhanVien,MaCoSo,MaVaiTro,LaCoSoMacDinh)
        VALUES (@MaTaiKhoanMoi,@MaNhanVien,@MaCoSo,@MaVaiTro,1);
        COMMIT;
        SELECT @MaTaiKhoanMoi AS MaTaiKhoan;
    END TRY
    BEGIN CATCH
        IF XACT_STATE()<>0 ROLLBACK;
        THROW;
    END CATCH
END;
GO
CREATE OR ALTER PROCEDURE dbo.usp_AdminDoiVaiTro
    @MaTaiKhoanThucHien INT,
    @MaCoSo INT,
    @MaTaiKhoanDuocSua INT,
    @MaVaiTroMoi VARCHAR(30)
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;
    IF @MaVaiTroMoi NOT IN ('ADMIN','TO_TRUONG','GIAO_VIEN')
        THROW 56111,N'Vai tro khong hop le.',1;
    BEGIN TRY
        SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
        BEGIN TRAN;
        IF NOT EXISTS (
            SELECT 1 FROM dbo.TaiKhoan a WITH (UPDLOCK,HOLDLOCK)
            JOIN dbo.TaiKhoanCoSo tc WITH (UPDLOCK,HOLDLOCK) ON tc.MaTaiKhoan=a.MaTaiKhoan
            JOIN dbo.VaiTro vt ON vt.MaVaiTro=tc.MaVaiTro
            WHERE a.MaTaiKhoan=@MaTaiKhoanThucHien AND tc.MaCoSo=@MaCoSo
              AND a.TrangThai=1 AND tc.TrangThai=1 AND vt.MaVaiTroChu='ADMIN'
        ) THROW 56112,N'Chi Admin duoc nang/ha vai tro.',1;
        DECLARE @MaVaiTro INT=(SELECT MaVaiTro FROM dbo.VaiTro WHERE MaVaiTroChu=@MaVaiTroMoi);
        DECLARE @MaVaiTroCu VARCHAR(30);
        SELECT @MaVaiTroCu=v.MaVaiTroChu
        FROM dbo.TaiKhoanCoSo tc WITH (UPDLOCK,HOLDLOCK)
        JOIN dbo.VaiTro v ON v.MaVaiTro=tc.MaVaiTro
        WHERE tc.MaTaiKhoan=@MaTaiKhoanDuocSua AND tc.MaCoSo=@MaCoSo AND tc.TrangThai=1;
        IF @MaVaiTroCu IS NULL THROW 56113,N'Tai khoan khong thuoc co so hoac da khoa.',1;
        IF @MaTaiKhoanThucHien=@MaTaiKhoanDuocSua AND @MaVaiTroMoi<>'ADMIN'
            THROW 56114,N'Khong duoc tu ha quyen Admin cua minh.',1;
        IF @MaVaiTroCu='ADMIN' AND @MaVaiTroMoi<>'ADMIN'
        BEGIN
            IF (SELECT COUNT_BIG(*) FROM dbo.TaiKhoanCoSo tc WITH (UPDLOCK,HOLDLOCK)
                JOIN dbo.VaiTro v ON v.MaVaiTro=tc.MaVaiTro
                JOIN dbo.TaiKhoan a ON a.MaTaiKhoan=tc.MaTaiKhoan
                WHERE tc.MaCoSo=@MaCoSo AND tc.TrangThai=1 AND a.TrangThai=1
                  AND v.MaVaiTroChu='ADMIN')<=1
                THROW 56115,N'Khong duoc xoa Admin cuoi cung cua co so.',1;
        END;
        UPDATE dbo.TaiKhoanCoSo SET MaVaiTro=@MaVaiTro
        WHERE MaTaiKhoan=@MaTaiKhoanDuocSua AND MaCoSo=@MaCoSo;
        -- Khong giu override quan tri khi ha quyen.
        IF @MaVaiTroMoi<>'ADMIN'
            DELETE p FROM dbo.PhanQuyenTaiKhoan p
            JOIN dbo.Quyen q ON q.MaQuyen=p.MaQuyen
            WHERE p.MaTaiKhoan=@MaTaiKhoanDuocSua AND p.MaCoSo=@MaCoSo
            AND q.MaQuyenChu IN ('THAY_DOI_VAI_TRO','HOAN_TIEN_AN',
                               'SUA_DIEM_DANH_DA_CHOT','CAI_DAT_HE_THONG');
        COMMIT;
    END TRY
    BEGIN CATCH
        IF XACT_STATE()<>0 ROLLBACK;
        THROW;
    END CATCH
END;
GO
CREATE OR ALTER PROCEDURE dbo.usp_CapQuyenCaNhan
    @MaTaiKhoanThucHien INT,
    @MaCoSo INT,
    @MaTaiKhoanDuocSua INT,
    @MaQuyenChu VARCHAR(70),
    @ChoPhep BIT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;
    BEGIN TRY
        BEGIN TRAN;
        DECLARE @VaiTroNguoiCap VARCHAR(30), @VaiTroDich VARCHAR(30), @MaQuyen INT;
        SELECT @VaiTroNguoiCap=v.MaVaiTroChu
        FROM dbo.TaiKhoan t JOIN dbo.TaiKhoanCoSo tc ON tc.MaTaiKhoan=t.MaTaiKhoan
        JOIN dbo.VaiTro v ON v.MaVaiTro=tc.MaVaiTro
        WHERE t.MaTaiKhoan=@MaTaiKhoanThucHien AND t.TrangThai=1
        AND tc.MaCoSo=@MaCoSo AND tc.TrangThai=1;
        IF @VaiTroNguoiCap NOT IN ('ADMIN','TO_TRUONG')
            THROW 56121,N'Khong co quyen cap quyen ca nhan.',1;
        SELECT @VaiTroDich=v.MaVaiTroChu
        FROM dbo.TaiKhoan t JOIN dbo.TaiKhoanCoSo tc WITH(UPDLOCK,HOLDLOCK)
          ON tc.MaTaiKhoan=t.MaTaiKhoan
        JOIN dbo.VaiTro v ON v.MaVaiTro=tc.MaVaiTro
        WHERE t.MaTaiKhoan=@MaTaiKhoanDuocSua AND t.TrangThai=1
        AND tc.MaCoSo=@MaCoSo AND tc.TrangThai=1;
        IF @VaiTroDich IS NULL THROW 56122,N'Tai khoan duoc sua khong hop le.',1;
        IF @VaiTroNguoiCap='TO_TRUONG' AND @VaiTroDich<>'GIAO_VIEN'
            THROW 56123,N'To truong chi duoc phan quyen gioi han cho Giao vien.',1;
        IF @MaQuyenChu IN ('THAY_DOI_VAI_TRO','HOAN_TIEN_AN',
             'SUA_DIEM_DANH_DA_CHOT','CAI_DAT_HE_THONG') AND @VaiTroDich<>'ADMIN'
            THROW 56124,N'Quyen nay chi danh cho vai tro Admin.',1;
        IF @VaiTroNguoiCap='TO_TRUONG' AND @MaQuyenChu NOT IN
           ('XEM_HOC_VIEN','XEM_BAO_CAO','DIEM_DANH','KHO_XEM','KHO_XUAT','TUYEN_SINH')
            THROW 56125,N'To truong chi duoc cap quyen gioi han cho Giao vien.',1;
        SELECT @MaQuyen=MaQuyen FROM dbo.Quyen WHERE MaQuyenChu=@MaQuyenChu;
        IF @MaQuyen IS NULL THROW 56126,N'Ma quyen khong ton tai.',1;
        IF EXISTS(SELECT 1 FROM dbo.PhanQuyenTaiKhoan WITH (UPDLOCK,HOLDLOCK)
                  WHERE MaTaiKhoan=@MaTaiKhoanDuocSua AND MaCoSo=@MaCoSo AND MaQuyen=@MaQuyen)
            UPDATE dbo.PhanQuyenTaiKhoan SET ChoPhep=@ChoPhep
            WHERE MaTaiKhoan=@MaTaiKhoanDuocSua AND MaCoSo=@MaCoSo AND MaQuyen=@MaQuyen;
        ELSE
            INSERT dbo.PhanQuyenTaiKhoan(MaTaiKhoan,MaCoSo,MaQuyen,ChoPhep)
            VALUES(@MaTaiKhoanDuocSua,@MaCoSo,@MaQuyen,@ChoPhep);
        COMMIT;
    END TRY
    BEGIN CATCH
        IF XACT_STATE()<>0 ROLLBACK;
        THROW;
    END CATCH
END;
GO
CREATE OR ALTER PROCEDURE dbo.usp_AdminDatLaiMatKhau
    @MaTaiKhoanThucHien INT,
    @MaCoSo INT,
    @MaTaiKhoanDuocSua INT,
    @MatKhauBamMoi VARCHAR(255)
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;
    IF @MatKhauBamMoi IS NULL OR NOT
       (@MatKhauBamMoi LIKE '$argon2id$%' OR @MatKhauBamMoi LIKE '$2a$%'
        OR @MatKhauBamMoi LIKE '$2b$%' OR @MatKhauBamMoi LIKE '$2y$%')
        THROW 56131,N'Chi chap nhan hash mat khau Argon2id/bcrypt.',1;
    BEGIN TRY
        BEGIN TRAN;
        IF NOT EXISTS (SELECT 1 FROM dbo.TaiKhoan a
           JOIN dbo.TaiKhoanCoSo tc ON tc.MaTaiKhoan=a.MaTaiKhoan
           JOIN dbo.VaiTro v ON v.MaVaiTro=tc.MaVaiTro
           WHERE a.MaTaiKhoan=@MaTaiKhoanThucHien AND tc.MaCoSo=@MaCoSo
             AND a.TrangThai=1 AND tc.TrangThai=1 AND v.MaVaiTroChu='ADMIN')
            THROW 56132,N'Chi Admin duoc dat lai mat khau cho tai khoan.',1;
        IF NOT EXISTS (SELECT 1 FROM dbo.TaiKhoanCoSo tc
           WHERE tc.MaTaiKhoan=@MaTaiKhoanDuocSua AND tc.MaCoSo=@MaCoSo AND tc.TrangThai=1)
            THROW 56133,N'Tai khoan khong thuoc co so.',1;
        UPDATE dbo.TaiKhoan SET MatKhauBam=@MatKhauBamMoi,PhaiDoiMatKhau=1
        WHERE MaTaiKhoan=@MaTaiKhoanDuocSua;
        COMMIT;
    END TRY
    BEGIN CATCH
        IF XACT_STATE()<>0 ROLLBACK;
        THROW;
    END CATCH
END;
GO
CREATE OR ALTER PROCEDURE dbo.usp_AdminGanTaiKhoanVaoCoSo
    @MaTaiKhoanThucHien INT,
    @MaCoSo INT,
    @MaTaiKhoanDuocGan INT,
    @MaVaiTroChu VARCHAR(30)='GIAO_VIEN'
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;
    IF @MaVaiTroChu NOT IN ('ADMIN','TO_TRUONG','GIAO_VIEN')
        THROW 56141,N'Vai tro khong hop le.',1;
    BEGIN TRY
        BEGIN TRAN;
        IF NOT EXISTS(
            SELECT 1 FROM dbo.TaiKhoan a JOIN dbo.TaiKhoanCoSo tc ON tc.MaTaiKhoan=a.MaTaiKhoan
            JOIN dbo.VaiTro v ON v.MaVaiTro=tc.MaVaiTro
            WHERE a.MaTaiKhoan=@MaTaiKhoanThucHien AND a.TrangThai=1
              AND tc.MaCoSo=@MaCoSo AND tc.TrangThai=1 AND v.MaVaiTroChu='ADMIN'
        ) THROW 56142,N'Chi Admin cua co so dich duoc cap them co so.',1;
        DECLARE @MaNhanVien INT=(SELECT MaNhanVien FROM dbo.TaiKhoan WHERE MaTaiKhoan=@MaTaiKhoanDuocGan AND TrangThai=1);
        IF @MaNhanVien IS NULL THROW 56143,N'Tai khoan can gan khong ton tai.',1;
        IF NOT EXISTS(SELECT 1 FROM dbo.NhanVienCoSo WHERE MaNhanVien=@MaNhanVien AND MaCoSo=@MaCoSo AND TrangThai=1)
            THROW 56144,N'Nhan vien chua duoc gan vao co so.',1;
        IF EXISTS(SELECT 1 FROM dbo.TaiKhoanCoSo WITH(UPDLOCK,HOLDLOCK)
                  WHERE MaTaiKhoan=@MaTaiKhoanDuocGan AND MaCoSo=@MaCoSo)
            THROW 56145,N'Tai khoan da duoc gan tai co so nay.',1;
        DECLARE @MaVaiTro INT=(SELECT MaVaiTro FROM dbo.VaiTro WHERE MaVaiTroChu=@MaVaiTroChu);
        IF @MaVaiTro IS NULL THROW 56146,N'Vai tro chua duoc khoi tao.',1;
        INSERT dbo.TaiKhoanCoSo(MaTaiKhoan,MaNhanVien,MaCoSo,MaVaiTro,LaCoSoMacDinh)
        VALUES (@MaTaiKhoanDuocGan,@MaNhanVien,@MaCoSo,@MaVaiTro,0);
        COMMIT;
    END TRY
    BEGIN CATCH
        IF XACT_STATE()<>0 ROLLBACK;
        THROW;
    END CATCH
END;
GO
-- <<< 09_TAI_KHOAN_PHAN_QUYEN.sql

-- >>> 10_KIEM_TRA_DANG_NHAP.sql
USE [VINABACUS];
GO
IF COL_LENGTH(N'dbo.TaiKhoan',N'PhaiDoiMatKhau') IS NULL
    THROW 56201,N'Thieu co PhaiDoiMatKhau.',1;
IF NOT EXISTS(SELECT 1 FROM dbo.Quyen WHERE MaQuyenChu='THAY_DOI_VAI_TRO')
    THROW 56202,N'Thieu quyen THAY_DOI_VAI_TRO.',1;
IF EXISTS(SELECT 1 FROM dbo.VaiTroQuyen vq JOIN dbo.VaiTro v ON v.MaVaiTro=vq.MaVaiTro
          JOIN dbo.Quyen q ON q.MaQuyen=vq.MaQuyen
          WHERE q.MaQuyenChu='THAY_DOI_VAI_TRO' AND v.MaVaiTroChu<>'ADMIN')
    THROW 56203,N'Quyen nang vai tro khong duoc cap mac dinh cho vai tro khac.',1;
IF (SELECT COUNT(*) FROM dbo.VaiTro)<>3
    THROW 56204,N'He thong phai chi co 3 vai tro.',1;
IF (SELECT COUNT(*) FROM sys.procedures WHERE schema_id=SCHEMA_ID(N'dbo')) < 19
    THROW 56206,N'Thieu thu tuc quan ly tai khoan.',1;
IF EXISTS(SELECT 1 FROM dbo.TaiKhoan WHERE MatKhauBam NOT LIKE '$argon2id$%'
          AND MatKhauBam NOT LIKE '$2a$%' AND MatKhauBam NOT LIKE '$2b$%'
          AND MatKhauBam NOT LIKE '$2y$%')
    THROW 56205,N'Co mat khau bam khong dung dinh dang cho phep.',1;
SELECT v.MaVaiTroChu,q.MaQuyenChu FROM dbo.VaiTroQuyen vq
JOIN dbo.VaiTro v ON v.MaVaiTro=vq.MaVaiTro
JOIN dbo.Quyen q ON q.MaQuyen=vq.MaQuyen
WHERE q.MaQuyenChu IN ('THAY_DOI_VAI_TRO','PHAN_QUYEN','TAO_TAI_KHOAN')
ORDER BY v.MaVaiTroChu,q.MaQuyenChu;
PRINT N'Kiem tra chinh sach tai khoan: PASS (chi kiem tra cau truc/du lieu, chua kiem thu bao mat JDBC).';
GO
-- <<< 10_KIEM_TRA_DANG_NHAP.sql


-- VINABACUS - PHAN BO SUNG DANG KY GOI HOC MOT NAM
-- Thu tuc/danh muc chi co tac dung khi chay SAU 01_CO_SO_DU_LIEU_DAY_DU.sql
USE [VINABACUS];
GO
IF TYPE_ID(N'dbo.TVP_KhoaTrongGoiNam') IS NULL
    EXEC(N'CREATE TYPE dbo.TVP_KhoaTrongGoiNam AS TABLE (
        SoThuTu INT NOT NULL PRIMARY KEY CHECK (SoThuTu>0),
        MaKhoaHoc INT NOT NULL,
        MaLop INT NULL,
        NgayBatDauDuKien DATE NOT NULL,
        HocPhiThoaThuan DECIMAL(18,2) NOT NULL CHECK (HocPhiThoaThuan>=0)
    )');
GO

-- Tra cuu mon, khoa, lop hien tai cua hoc vien; dung cho tab "Mon hoc & Lop dang hoc".
CREATE OR ALTER VIEW dbo.v_MonKhoaLopHocVien
AS
SELECT d.MaHocVien,d.MaCoSo,d.MaDangKy,d.MaDangKyGoiNam,d.SoThuTuTrongGoi,
       m.MaMonHoc,m.TenMonHoc,k.MaKhoaHoc,k.TenKhoaHoc,
       x.MaXepLop,l.MaLop,l.TenLop,l.MaGiaoVien,
       d.NgayBatDau,d.NgayKetThucDuKien,d.TongSoBuoiDangKy,
       d.HocPhiSauGiam,d.TrangThai AS TrangThaiDangKy,
       x.TrangThai AS TrangThaiXepLop,x.TuNgay AS NgayVaoLop
FROM dbo.DangKyKhoaHoc d
JOIN dbo.KhoaHoc k ON k.MaKhoaHoc=d.MaKhoaHoc AND k.MaCoSo=d.MaCoSo
JOIN dbo.MonHoc m ON m.MaMonHoc=k.MaMonHoc AND m.MaCoSo=d.MaCoSo
LEFT JOIN dbo.XepLopTrungTam x ON x.MaDangKy=d.MaDangKy AND x.TrangThai='HIEU_LUC'
LEFT JOIN dbo.LopTrungTam l ON l.MaLop=x.MaLop AND l.MaCoSo=d.MaCoSo;
GO

-- Gom ket tien do, khoan phai thu va cong no cua GOI NAM. Cac khoan thu gan voi KHOA,
-- khong tao chung tu thu rieng cho header goi de tranh thu tien hai lan.
CREATE OR ALTER VIEW dbo.v_TongQuanGoiNam
AS
SELECT g.MaDangKyGoiNam,g.MaHocVien,g.MaCoSo,g.TenGoi,
       g.NgayBatDau,g.NgayKetThuc,g.MaChuongTrinh,
       g.TongHocPhiGoc,g.SoTienGiam,g.HocPhiSauGiam,g.TrangThai,
       COUNT(d.MaDangKy) AS SoKhoaTrongGoi,
       COALESCE(SUM(CASE WHEN d.TrangThai='HOAN_THANH' THEN 1 ELSE 0 END),0) AS SoKhoaDaHoanThanh,
       CAST(COALESCE(SUM(v.CongNoConLai),0) AS DECIMAL(18,2)) AS CongNoConLai
FROM dbo.DangKyGoiNam g
LEFT JOIN dbo.DangKyKhoaHoc d ON d.MaDangKyGoiNam=g.MaDangKyGoiNam
LEFT JOIN dbo.KhoanPhaiThu p ON p.MaDangKy=d.MaDangKy AND p.LoaiKhoan='HOC_PHI_KHOA' AND p.TrangThai='HIEU_LUC'
LEFT JOIN dbo.v_CongNoKhoanThu v ON v.MaKhoanThu=p.MaKhoanThu
GROUP BY g.MaDangKyGoiNam,g.MaHocVien,g.MaCoSo,g.TenGoi,
         g.NgayBatDau,g.NgayKetThuc,g.MaChuongTrinh,
         g.TongHocPhiGoc,g.SoTienGiam,g.HocPhiSauGiam,g.TrangThai;
GO

-- Dang ky goi mot nam: 1 uu dai CAO NHAT tren toan giao dich, khong cong don.
-- Uu dai GOI_NAM tinh tren TONG GIA THOA THUAN; uu dai KHOA_HOC chi tinh tren
-- dung khoa duoc gan tai GiamGiaKhoaHoc. Nguoi dung khong tu tinh phan bo.
-- Phan bo uu dai nam theo ty le hoc phi tung khoa, lam tron 2 chu so va phan
-- chenhlech xuong khoa cuoi. MaDangKyGoiNam chi lam nhiem vu lien ket cac khoa.
CREATE OR ALTER PROCEDURE dbo.usp_DangKyGoiNam
    @MaHocVien INT,
    @MaCoSo INT,
    @TenGoi NVARCHAR(200),
    @NgayDangKy DATE,
    @NgayBatDau DATE,
    @MaTaiKhoan INT,
    @MaGiaoDichNghiepVu VARCHAR(64),
    @DanhSach dbo.TVP_KhoaTrongGoiNam READONLY,
    @MaYeuCau INT=NULL,
    @GhiChu NVARCHAR(500)=NULL
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    DECLARE @NgayKetThuc DATE=DATEADD(DAY,-1,DATEADD(YEAR,1,@NgayBatDau));
    DECLARE @TongHocPhi DECIMAL(18,2),@SoTienGiam DECIMAL(18,2)=0,
            @MaChuongTrinh INT=NULL,@PhamVi VARCHAR(12)=NULL,@SoThuTuUuDai INT=NULL,
            @MaDangKyGoiNam INT,@MaKhoa INT,@TaiNguyen NVARCHAR(255),
            @SoThuTuCuoi INT;

    IF @TenGoi IS NULL OR LEN(LTRIM(RTRIM(@TenGoi)))=0
        THROW 57001,N'Vui lòng nhập tên gói học một năm.',1;
    IF @MaGiaoDichNghiepVu IS NULL OR LEN(@MaGiaoDichNghiepVu)=0
        THROW 57002,N'Thiếu mã giao dịch chống lặp.',1;
    IF NOT EXISTS (SELECT 1 FROM @DanhSach)
        THROW 57003,N'Gói năm phải có ít nhất một khóa.',1;
    IF NOT EXISTS (SELECT 1 FROM @DanhSach WHERE MaLop IS NOT NULL)
        THROW 57004,N'Gói năm phải có ít nhất một khóa đã xếp lớp chính thức.',1;
    IF EXISTS (SELECT 1 FROM @DanhSach WHERE NgayBatDauDuKien<@NgayBatDau OR NgayBatDauDuKien>@NgayKetThuc)
        THROW 57005,N'Ngày dự kiến bắt đầu khóa phải trong thời hạn của gói năm.',1;
    IF dbo.fn_CoQuyen(@MaTaiKhoan,@MaCoSo,'XEP_LOP')<>1
        THROW 57006,N'Tài khoản không có quyền đăng ký và xếp lớp.',1;

    SELECT @TongHocPhi=SUM(HocPhiThoaThuan),@SoThuTuCuoi=MAX(SoThuTu) FROM @DanhSach;
    IF @TongHocPhi IS NULL OR @TongHocPhi<0
        THROW 57007,N'Tổng học phí thỏa thuận không hợp lệ.',1;

    BEGIN TRY
        SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
        BEGIN TRAN;
        SET @TaiNguyen=CONCAT('VINABACUS:DKGOI:',@MaCoSo,':',@MaHocVien);
        EXEC @MaKhoa=sys.sp_getapplock @Resource=@TaiNguyen,
             @LockMode='Exclusive',@LockOwner='Transaction',@LockTimeout=10000;
        IF @MaKhoa<0 THROW 57008,N'Không thể khóa giao dịch đăng ký gói.',1;
        IF EXISTS (SELECT 1 FROM dbo.DangKyGoiNam WHERE MaGiaoDichNghiepVu=@MaGiaoDichNghiepVu)
            THROW 57009,N'Giao dịch đăng ký gói năm đã được xử lý.',1;
        IF NOT EXISTS (SELECT 1 FROM dbo.HocVien WHERE MaHocVien=@MaHocVien AND TrangThaiHoSo='HOAT_DONG')
            THROW 57010,N'Không tìm thấy hồ sơ học viên hợp lệ.',1;
        IF NOT EXISTS (SELECT 1 FROM dbo.CoSo WHERE MaCoSo=@MaCoSo AND LoaiCoSo='TRUNG_TAM' AND TrangThai=1)
            THROW 57011,N'Gói năm chỉ áp dụng cho Trung tâm đang hoạt động.',1;
        IF EXISTS (
            SELECT 1 FROM @DanhSach ds
            LEFT JOIN dbo.KhoaHoc k ON k.MaKhoaHoc=ds.MaKhoaHoc AND k.MaCoSo=@MaCoSo AND k.TrangThai=1
            WHERE k.MaKhoaHoc IS NULL
        ) THROW 57012,N'Có khóa học không thuộc Trung tâm hiện tại hoặc đã ngừng.',1;
        IF EXISTS (
            SELECT 1 FROM @DanhSach ds
            LEFT JOIN dbo.LopTrungTam l ON l.MaLop=ds.MaLop AND l.MaKhoaHoc=ds.MaKhoaHoc
               AND l.MaCoSo=@MaCoSo AND l.TrangThai='DANG_MO'
            WHERE ds.MaLop IS NOT NULL AND l.MaLop IS NULL
        ) THROW 57013,N'Lớp chọn không thuộc khóa học hoặc đang đóng.',1;

        -- Yeu cau tuyen sinh, neu co, chi duoc hoan tat mot lan trong cung giao dich.
        IF @MaYeuCau IS NOT NULL
        BEGIN
            IF NOT EXISTS (
                SELECT 1 FROM dbo.YeuCauXepLop y WITH (UPDLOCK,HOLDLOCK)
                WHERE y.MaYeuCau=@MaYeuCau AND y.MaCoSo=@MaCoSo
                  AND y.MaHocVien=@MaHocVien AND y.TrangThai='CHO_XEP_LOP'
                  AND (y.MaKhoaHoc IS NULL OR EXISTS (
                      SELECT 1 FROM @DanhSach ds WHERE ds.MaKhoaHoc=y.MaKhoaHoc))
            ) THROW 57014,N'Yêu cầu tuyển sinh chưa hợp lệ hoặc đã hoàn tất.',1;
            IF EXISTS(SELECT 1 FROM dbo.DangKyKhoaHoc WHERE MaYeuCau=@MaYeuCau)
                OR EXISTS(SELECT 1 FROM dbo.XepLopMamNon WHERE MaYeuCau=@MaYeuCau)
                OR EXISTS(SELECT 1 FROM dbo.DangKyGoiNam WHERE MaYeuCau=@MaYeuCau)
                THROW 57015,N'Yêu cầu đã được dùng để xếp lớp.',1;
        END;

        -- Chi mot chuong trinh co giam cao nhat duoc chon.
        DECLARE @UngVien TABLE (
            MaChuongTrinh INT NOT NULL,PhamVi VARCHAR(12) NOT NULL,
            SoThuTu INT NULL,Giam DECIMAL(18,2) NOT NULL
        );
        INSERT @UngVien(MaChuongTrinh,PhamVi,SoThuTu,Giam)
        SELECT ct.MaChuongTrinh,'GOI_NAM',NULL,
            CAST(CASE WHEN ct.LoaiGiam='PHAN_TRAM' THEN ROUND(@TongHocPhi*ct.GiaTriGiam/100,2)
                 WHEN ct.GiaTriGiam>@TongHocPhi THEN @TongHocPhi ELSE ct.GiaTriGiam END AS DECIMAL(18,2))
        FROM dbo.ChuongTrinhGiamGia ct
        WHERE ct.MaCoSo=@MaCoSo AND ct.PhamViApDung='GOI_NAM' AND ct.TrangThai=1
          AND @NgayDangKy BETWEEN ct.TuNgay AND ct.DenNgay;

        INSERT @UngVien(MaChuongTrinh,PhamVi,SoThuTu,Giam)
        SELECT ct.MaChuongTrinh,'KHOA_HOC',ds.SoThuTu,
            CAST(CASE WHEN ct.LoaiGiam='PHAN_TRAM' THEN ROUND(ds.HocPhiThoaThuan*ct.GiaTriGiam/100,2)
                 WHEN ct.GiaTriGiam>ds.HocPhiThoaThuan THEN ds.HocPhiThoaThuan ELSE ct.GiaTriGiam END AS DECIMAL(18,2))
        FROM @DanhSach ds
        JOIN dbo.GiamGiaKhoaHoc gg ON gg.MaKhoaHoc=ds.MaKhoaHoc AND gg.MaCoSo=@MaCoSo
        JOIN dbo.ChuongTrinhGiamGia ct ON ct.MaChuongTrinh=gg.MaChuongTrinh AND ct.MaCoSo=@MaCoSo
        WHERE ct.PhamViApDung='KHOA_HOC' AND ct.TrangThai=1
          AND @NgayDangKy BETWEEN ct.TuNgay AND ct.DenNgay;

        SELECT TOP(1) @MaChuongTrinh=MaChuongTrinh,@PhamVi=PhamVi,
              @SoThuTuUuDai=SoThuTu,@SoTienGiam=Giam
        FROM @UngVien WHERE Giam>0
        ORDER BY Giam DESC,MaChuongTrinh,SoThuTu;

        DECLARE @Khoa TABLE (
            SoThuTu INT NOT NULL PRIMARY KEY,MaKhoaHoc INT NOT NULL,MaLop INT NULL,
            NgayBatDau DATE NOT NULL,HocPhi DECIMAL(18,2) NOT NULL,
            Giam DECIMAL(18,2) NOT NULL DEFAULT 0
        );
        INSERT @Khoa(SoThuTu,MaKhoaHoc,MaLop,NgayBatDau,HocPhi)
        SELECT SoThuTu,MaKhoaHoc,MaLop,NgayBatDauDuKien,HocPhiThoaThuan FROM @DanhSach;
        IF @MaChuongTrinh IS NOT NULL AND @PhamVi='KHOA_HOC'
            UPDATE @Khoa SET Giam=@SoTienGiam WHERE SoThuTu=@SoThuTuUuDai;
        IF @MaChuongTrinh IS NOT NULL AND @PhamVi='GOI_NAM' AND @TongHocPhi>0
        BEGIN
            UPDATE @Khoa SET Giam=ROUND(HocPhi*@SoTienGiam/@TongHocPhi,2)
                WHERE SoThuTu<>@SoThuTuCuoi;
            DECLARE @GiamDaPhanBo DECIMAL(18,2)=(SELECT COALESCE(SUM(Giam),0) FROM @Khoa);
            UPDATE @Khoa SET Giam=@SoTienGiam-@GiamDaPhanBo WHERE SoThuTu=@SoThuTuCuoi;
        END;
        IF EXISTS(SELECT 1 FROM @Khoa WHERE Giam<0 OR Giam>HocPhi)
            THROW 57016,N'Không thể phân bổ giảm giá hợp lệ theo khóa.',1;
        IF (SELECT COALESCE(SUM(Giam),0) FROM @Khoa)<>@SoTienGiam
            THROW 57017,N'Tổng giảm giá phân bổ không khớp ưu đãi gói.',1;

        INSERT dbo.DangKyGoiNam (MaHocVien,MaCoSo,MaYeuCau,TenGoi,NgayDangKy,
            NgayBatDau,NgayKetThuc,TongHocPhiGoc,MaChuongTrinh,SoTienGiam,HocPhiSauGiam,
            MaTaiKhoanTao,MaGiaoDichNghiepVu,GhiChu)
        VALUES(@MaHocVien,@MaCoSo,@MaYeuCau,@TenGoi,@NgayDangKy,
            @NgayBatDau,@NgayKetThuc,@TongHocPhi,@MaChuongTrinh,@SoTienGiam,
            @TongHocPhi-@SoTienGiam,@MaTaiKhoan,@MaGiaoDichNghiepVu,@GhiChu);
        SET @MaDangKyGoiNam=CONVERT(INT,SCOPE_IDENTITY());

        DECLARE @SoThuTu INT,@MaKhoaHoc INT,@MaLop INT,@NgayBD DATE,
                @HocPhi DECIMAL(18,2),@Giam DECIMAL(18,2),@TongBuoi INT,
                @MaDangKy INT,@NgayKetThucDuKien DATE,@MaChuongTrinhKhoa INT;
        DECLARE curGoi CURSOR LOCAL FAST_FORWARD FOR
            SELECT SoThuTu,MaKhoaHoc,MaLop,NgayBatDau,HocPhi,Giam
            FROM @Khoa ORDER BY SoThuTu;
        OPEN curGoi;
        FETCH NEXT FROM curGoi INTO @SoThuTu,@MaKhoaHoc,@MaLop,@NgayBD,@HocPhi,@Giam;
        WHILE @@FETCH_STATUS=0
        BEGIN
            SET @NgayKetThucDuKien=NULL;
            SELECT @TongBuoi=TongSoBuoi FROM dbo.KhoaHoc
            WHERE MaKhoaHoc=@MaKhoaHoc AND MaCoSo=@MaCoSo;
            IF @MaLop IS NOT NULL
            BEGIN
                -- Chi tinh ngay ket thuc neu da co lop va lich; khoa tuong lai chua
                -- xep lop duoc de ngay du kien ket thuc NULL.
                ;WITH DemNgay AS (
                    SELECT TOP (2000) ROW_NUMBER() OVER(ORDER BY a.object_id,b.object_id)-1 AS N
                    FROM sys.all_objects a CROSS JOIN sys.all_objects b
                ), NgayPhuHop AS (
                    SELECT DATEADD(DAY,N,@NgayBD) AS NgayHoc FROM DemNgay
                ), Lich AS (
                    SELECT NgayHoc,ROW_NUMBER() OVER(ORDER BY NgayHoc) AS STT
                    FROM NgayPhuHop d WHERE EXISTS(
                        SELECT 1 FROM dbo.LichHocLop lh
                        WHERE lh.MaLop=@MaLop AND lh.TrangThai='HIEU_LUC'
                          AND lh.ThuTrongTuan=(DATEDIFF(DAY,CONVERT(DATE,'19000101'),d.NgayHoc)%7)+2
                          AND lh.TuNgay<=d.NgayHoc AND (lh.DenNgay IS NULL OR lh.DenNgay>=d.NgayHoc))
                )
                SELECT @NgayKetThucDuKien=NgayHoc FROM Lich WHERE STT=@TongBuoi;
                IF @NgayKetThucDuKien IS NULL
                    THROW 57018,N'Lớp chưa có lịch học đủ để tính ngày dự kiến kết thúc.',1;
            END;
            SET @MaChuongTrinhKhoa=CASE WHEN @Giam>0 THEN @MaChuongTrinh ELSE NULL END;
            INSERT dbo.DangKyKhoaHoc(MaHocVien,MaCoSo,MaKhoaHoc,
                MaDangKyGoiNam,SoThuTuTrongGoi,NgayDangKy,NgayBatDau,NgayKetThucDuKien,
                TongSoBuoiDangKy,HocPhiThoaThuan,MaChuongTrinh,SoTienGiam,HocPhiSauGiam,TrangThai)
            VALUES(@MaHocVien,@MaCoSo,@MaKhoaHoc,@MaDangKyGoiNam,@SoThuTu,@NgayDangKy,
                @NgayBD,@NgayKetThucDuKien,@TongBuoi,@HocPhi,@MaChuongTrinhKhoa,@Giam,
                @HocPhi-@Giam,CASE WHEN @MaLop IS NULL THEN 'CHO_XEP_LOP' ELSE 'DANG_HOC' END);
            SET @MaDangKy=CONVERT(INT,SCOPE_IDENTITY());
            IF @MaLop IS NOT NULL
                INSERT dbo.XepLopTrungTam(MaDangKy,MaKhoaHoc,MaCoSo,MaLop,TuNgay,TrangThai)
                VALUES(@MaDangKy,@MaKhoaHoc,@MaCoSo,@MaLop,@NgayBD,'HIEU_LUC');
            INSERT dbo.KhoanPhaiThu(MaHocVien,MaCoSo,LoaiKhoan,MaDangKy,SoTienGoc)
            VALUES(@MaHocVien,@MaCoSo,'HOC_PHI_KHOA',@MaDangKy,@HocPhi-@Giam);
            FETCH NEXT FROM curGoi INTO @SoThuTu,@MaKhoaHoc,@MaLop,@NgayBD,@HocPhi,@Giam;
        END;
        CLOSE curGoi;
        DEALLOCATE curGoi;

        IF @MaYeuCau IS NOT NULL
        BEGIN
            UPDATE dbo.YeuCauXepLop SET TrangThai='HOAN_TAT',NgayHoanTat=SYSUTCDATETIME(),
                MaGiaoDichHoanTat=@MaGiaoDichNghiepVu WHERE MaYeuCau=@MaYeuCau;
            UPDATE dbo.KhachTiemNang SET TrangThai='DA_CHUYEN_DOI'
                WHERE MaKhach=(SELECT MaKhach FROM dbo.YeuCauXepLop WHERE MaYeuCau=@MaYeuCau);
        END;
        COMMIT;
        SELECT @MaDangKyGoiNam AS MaDangKyGoiNam,@TongHocPhi AS TongHocPhiGoc,
               @SoTienGiam AS SoTienGiam,@TongHocPhi-@SoTienGiam AS HocPhiSauGiam,
               @MaChuongTrinh AS MaChuongTrinh;
    END TRY
    BEGIN CATCH
        IF CURSOR_STATUS('local','curGoi')>=0 CLOSE curGoi;
        IF CURSOR_STATUS('local','curGoi')>-3 DEALLOCATE curGoi;
        IF XACT_STATE()<>0 ROLLBACK;
        THROW;
    END CATCH
END;
GO


-- KIEM TRA SAU KHI CAI MOI / CO DU LIEU
USE [VINABACUS];
GO
SET NOCOUNT ON;
DECLARE @SoBang INT=(SELECT COUNT(*) FROM sys.tables WHERE schema_id=SCHEMA_ID(N'dbo'));
DECLARE @SoFK INT=(SELECT COUNT(*) FROM sys.foreign_keys WHERE schema_id=SCHEMA_ID(N'dbo'));
IF @SoBang<>57 THROW 57401,N'Khong du 57 bang du kien.',1;
IF @SoFK<137 THROW 57402,N'Thieu khoa ngoai du kien.',1;
IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE is_disabled=1 OR is_not_trusted=1)
   THROW 57403,N'Co khoa ngoai dang bi tat hoac khong duoc tin cay.',1;
IF NOT EXISTS(SELECT 1 FROM sys.columns WHERE object_id=OBJECT_ID('dbo.DangKyKhoaHoc') AND name='MaDangKyGoiNam')
   THROW 57404,N'DangKyKhoaHoc chua co khoa ngoai goi nam.',1;
IF NOT EXISTS(SELECT 1 FROM sys.columns WHERE object_id=OBJECT_ID('dbo.ChuongTrinhGiamGia') AND name='PhamViApDung')
   THROW 57405,N'ChuongTrinhGiamGia chua co pham vi uu dai.',1;
IF NOT EXISTS(SELECT 1 FROM sys.columns WHERE object_id=OBJECT_ID('dbo.NhanVien') AND name='EmailDaXacMinh')
   THROW 57406,N'Thieu co danh dau Email da xac minh.',1;
IF OBJECT_ID('dbo.usp_DangKyGoiNam','P') IS NULL
   THROW 57407,N'Thieu thu tuc dang ky goi nam.',1;
IF OBJECT_ID('dbo.v_MonKhoaLopHocVien','V') IS NULL
   THROW 57408,N'Thieu view tra cuu mon khoa lop hoc vien.',1;
IF EXISTS (SELECT 1 FROM dbo.DangKyKhoaHoc d JOIN dbo.DangKyGoiNam g
       ON g.MaDangKyGoiNam=d.MaDangKyGoiNam
       WHERE d.MaHocVien<>g.MaHocVien OR d.MaCoSo<>g.MaCoSo)
   THROW 57409,N'Dang ky khoa bi noi sai hoc vien/co so voi goi nam.',1;
IF EXISTS(
  SELECT 1 FROM dbo.DangKyGoiNam g
  OUTER APPLY (
      SELECT COUNT(*) AS SoKhoa,COALESCE(SUM(d.HocPhiThoaThuan),0) AS TongGoc,
        COALESCE(SUM(d.SoTienGiam),0) AS TongGiam,
        COALESCE(SUM(d.HocPhiSauGiam),0) AS TongSau
      FROM dbo.DangKyKhoaHoc d WHERE d.MaDangKyGoiNam=g.MaDangKyGoiNam
  ) t
  WHERE t.SoKhoa=0 OR t.TongGoc<>g.TongHocPhiGoc
    OR t.TongGiam<>g.SoTienGiam OR t.TongSau<>g.HocPhiSauGiam
) THROW 57410,N'Tong hoc phi goi khong khop chi tiet khoa.',1;
IF EXISTS (SELECT 1 FROM dbo.DangKyKhoaHoc d
   JOIN dbo.KhoanPhaiThu p ON p.MaDangKy=d.MaDangKy AND p.LoaiKhoan='HOC_PHI_KHOA'
   WHERE d.MaDangKyGoiNam IS NOT NULL AND p.SoTienGoc<>d.HocPhiSauGiam)
   THROW 57411,N'Cong no goc cua khoa trong goi khong khop hoc phi sau giam.',1;
IF EXISTS (SELECT 1 FROM dbo.DangKyKhoaHoc d WHERE d.MaDangKyGoiNam IS NOT NULL
    AND NOT EXISTS(SELECT 1 FROM dbo.KhoanPhaiThu p WHERE p.MaDangKy=d.MaDangKy AND p.LoaiKhoan='HOC_PHI_KHOA'))
    THROW 57412,N'Co khoa trong goi chua tao khoan phai thu.',1;
IF EXISTS(SELECT 1 FROM dbo.ChuongTrinhGiamGia WHERE PhamViApDung NOT IN ('KHOA_HOC','GOI_NAM'))
    THROW 57413,N'Pham vi uu dai khong hop le.',1;
SELECT DB_NAME() AS TenCSDL,@SoBang AS SoBang,@SoFK AS SoKhoaNgoai,
       (SELECT COUNT(*) FROM sys.procedures WHERE schema_id=SCHEMA_ID(N'dbo')) AS SoStoredProcedure,
       (SELECT COUNT(*) FROM sys.views WHERE schema_id=SCHEMA_ID(N'dbo')) AS SoView,
       (SELECT COUNT(*) FROM dbo.DangKyGoiNam) AS SoGoiNam,
       (SELECT COUNT(*) FROM dbo.DangKyKhoaHoc WHERE MaDangKyGoiNam IS NOT NULL) AS SoKhoaTrongGoi;
PRINT N'PASS: Kiem tra cau truc co ban va doi chieu goi nam (KHONG phai kiem thu runtime).';
GO



-- DU LIEU MINH HOA -- CHI DUNG TRONG MOI TRUONG THU NGHIEM

-- DU LIEU MINH HOA VINABACUS - CHI DUNG TREN DATABASE THU NGHIEM
-- KHONG chay tren VINABACUS dang van hanh; cac ten co tien to [MAU].
-- Du lieu: 2 hoc vien, 2 mon/3 khoa Trung tam, 2 lop, 2 uu dai, 1 nam hoc
-- Mam non, 3 lop Mam non, 2 mat hang kho. Khong sinh phieu thu/tin dung gia.
-- Khong tao account/mat khau mac dinh de tranh lo thong tin bao mat.
USE [VINABACUS];
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
BEGIN TRY
  BEGIN TRAN;
  DECLARE @TT INT=(SELECT MaCoSo FROM dbo.CoSo WHERE LoaiCoSo='TRUNG_TAM');
  DECLARE @MN INT=(SELECT MaCoSo FROM dbo.CoSo WHERE LoaiCoSo='MAM_NON');

  IF NOT EXISTS(SELECT 1 FROM dbo.HocVien WHERE HoTen=N'[MẪU] Nguyễn Minh Anh')
    INSERT dbo.HocVien(HoTen,NgaySinh,TenNguoiLienHe,SoDienThoaiLienHe,GhiChu)
    VALUES(N'[MẪU] Nguyễn Minh Anh','20170618',N'[MẪU] Phụ huynh Anh', '0900000001',N'Dữ liệu minh họa; không dùng học viên thật');
  IF NOT EXISTS(SELECT 1 FROM dbo.HocVien WHERE HoTen=N'[MẪU] Trần Hải Nam')
    INSERT dbo.HocVien(HoTen,NgaySinh,TenNguoiLienHe,SoDienThoaiLienHe,GhiChu)
    VALUES(N'[MẪU] Trần Hải Nam','20190826',N'[MẪU] Phụ huynh Nam', '0900000002',N'Dữ liệu minh họa; không dùng học viên thật');

  IF NOT EXISTS(SELECT 1 FROM dbo.MonHoc WHERE MaCoSo=@TT AND TenMonHoc=N'[MẪU] Toán tư duy')
    INSERT dbo.MonHoc(MaCoSo,TenMonHoc) VALUES(@TT,N'[MẪU] Toán tư duy');
  IF NOT EXISTS(SELECT 1 FROM dbo.MonHoc WHERE MaCoSo=@TT AND TenMonHoc=N'[MẪU] Tiếng Anh')
    INSERT dbo.MonHoc(MaCoSo,TenMonHoc) VALUES(@TT,N'[MẪU] Tiếng Anh');

  DECLARE @MonToan INT=(SELECT MaMonHoc FROM dbo.MonHoc WHERE MaCoSo=@TT AND TenMonHoc=N'[MẪU] Toán tư duy');
  DECLARE @MonAnh INT=(SELECT MaMonHoc FROM dbo.MonHoc WHERE MaCoSo=@TT AND TenMonHoc=N'[MẪU] Tiếng Anh');
  IF NOT EXISTS(SELECT 1 FROM dbo.KhoaHoc WHERE MaCoSo=@TT AND TenKhoaHoc=N'[MẪU] Kid 1')
    INSERT dbo.KhoaHoc(MaCoSo,MaMonHoc,TenKhoaHoc,TongSoBuoi,HocPhiMacDinh)
    VALUES(@TT,@MonToan,N'[MẪU] Kid 1',32,2000000);
  IF NOT EXISTS(SELECT 1 FROM dbo.KhoaHoc WHERE MaCoSo=@TT AND TenKhoaHoc=N'[MẪU] Kid 2')
    INSERT dbo.KhoaHoc(MaCoSo,MaMonHoc,TenKhoaHoc,TongSoBuoi,HocPhiMacDinh)
    VALUES(@TT,@MonToan,N'[MẪU] Kid 2',32,3000000);
  IF NOT EXISTS(SELECT 1 FROM dbo.KhoaHoc WHERE MaCoSo=@TT AND TenKhoaHoc=N'[MẪU] Cambridge 1A')
    INSERT dbo.KhoaHoc(MaCoSo,MaMonHoc,TenKhoaHoc,TongSoBuoi,HocPhiMacDinh)
    VALUES(@TT,@MonAnh,N'[MẪU] Cambridge 1A',36,3600000);

  DECLARE @Kid1 INT=(SELECT MaKhoaHoc FROM dbo.KhoaHoc WHERE MaCoSo=@TT AND TenKhoaHoc=N'[MẪU] Kid 1');
  DECLARE @Kid2 INT=(SELECT MaKhoaHoc FROM dbo.KhoaHoc WHERE MaCoSo=@TT AND TenKhoaHoc=N'[MẪU] Kid 2');
  IF NOT EXISTS (SELECT 1 FROM dbo.LopTrungTam WHERE MaCoSo=@TT AND TenLop=N'[MẪU] Lớp Toán A')
    INSERT dbo.LopTrungTam(MaCoSo,MaKhoaHoc,TenLop,NgayKhaiGiang)
    VALUES(@TT,@Kid1,N'[MẪU] Lớp Toán A','20261110');
  DECLARE @LopKid1 INT=(SELECT TOP(1) MaLop FROM dbo.LopTrungTam WHERE MaCoSo=@TT AND TenLop=N'[MẪU] Lớp Toán A');
  IF NOT EXISTS (SELECT 1 FROM dbo.LichHocLop WHERE MaLop=@LopKid1)
    INSERT dbo.LichHocLop(MaLop,ThuTrongTuan,GioBatDau,GioKetThuc,TuNgay)
    VALUES(@LopKid1,3,'17:30','19:00','20261110'),
          (@LopKid1,5,'17:30','19:00','20261110'),
          (@LopKid1,7,'08:00','09:30','20261110');

  IF NOT EXISTS(SELECT 1 FROM dbo.ChuongTrinhGiamGia
                WHERE MaCoSo=@TT AND TenChuongTrinh=N'[MẪU] Ưu đãi học 1 năm giảm 15%')
    INSERT dbo.ChuongTrinhGiamGia(MaCoSo,TenChuongTrinh,PhamViApDung,LoaiGiam,GiaTriGiam,TuNgay,DenNgay)
    VALUES(@TT,N'[MẪU] Ưu đãi học 1 năm giảm 15%','GOI_NAM','PHAN_TRAM',15,'20260101','20281231');
  IF NOT EXISTS(SELECT 1 FROM dbo.ChuongTrinhGiamGia
                WHERE MaCoSo=@TT AND TenChuongTrinh=N'[MẪU] Giảm 200.000đ khóa Kid 1')
    INSERT dbo.ChuongTrinhGiamGia(MaCoSo,TenChuongTrinh,PhamViApDung,LoaiGiam,GiaTriGiam,TuNgay,DenNgay)
    VALUES(@TT,N'[MẪU] Giảm 200.000đ khóa Kid 1','KHOA_HOC','SO_TIEN',200000,'20260101','20281231');
  DECLARE @MaUuDaiKhoa INT=(SELECT MaChuongTrinh FROM dbo.ChuongTrinhGiamGia
    WHERE MaCoSo=@TT AND TenChuongTrinh=N'[MẪU] Giảm 200.000đ khóa Kid 1');
  IF NOT EXISTS(SELECT 1 FROM dbo.GiamGiaKhoaHoc WHERE MaChuongTrinh=@MaUuDaiKhoa AND MaKhoaHoc=@Kid1)
    INSERT dbo.GiamGiaKhoaHoc(MaChuongTrinh,MaKhoaHoc,MaCoSo)
    VALUES(@MaUuDaiKhoa,@Kid1,@TT);

  IF NOT EXISTS(SELECT 1 FROM dbo.NamHoc WHERE MaCoSo=@MN AND TenNamHoc=N'[MẪU] 2026–2027')
    INSERT dbo.NamHoc(MaCoSo,TenNamHoc,TuNgay,DenNgay)
    VALUES(@MN,N'[MẪU] 2026–2027','20260801','20270731');
  DECLARE @NamHoc INT=(SELECT MaNamHoc FROM dbo.NamHoc WHERE MaCoSo=@MN AND TenNamHoc=N'[MẪU] 2026–2027');
  IF NOT EXISTS(SELECT 1 FROM dbo.LopMamNon WHERE MaNamHoc=@NamHoc AND TenLop=N'[MẪU] Lớp Mầm')
    INSERT dbo.LopMamNon(MaNamHoc,MaCoSo,TenLop) VALUES(@NamHoc,@MN,N'[MẪU] Lớp Mầm');
  IF NOT EXISTS(SELECT 1 FROM dbo.LopMamNon WHERE MaNamHoc=@NamHoc AND TenLop=N'[MẪU] Lớp Chồi')
    INSERT dbo.LopMamNon(MaNamHoc,MaCoSo,TenLop) VALUES(@NamHoc,@MN,N'[MẪU] Lớp Chồi');
  IF NOT EXISTS(SELECT 1 FROM dbo.LopMamNon WHERE MaNamHoc=@NamHoc AND TenLop=N'[MẪU] Lớp Lá')
    INSERT dbo.LopMamNon(MaNamHoc,MaCoSo,TenLop) VALUES(@NamHoc,@MN,N'[MẪU] Lớp Lá');

  IF NOT EXISTS(SELECT 1 FROM dbo.HangHoa WHERE MaCoSo=@MN AND MaSanPham='DEMO-MN-01')
    INSERT dbo.HangHoa(MaCoSo,MaSanPham,TenHangHoa,DonViTinh,DonGiaSi,DonGiaXuatBan)
    VALUES(@MN,'DEMO-MN-01',N'[MẪU] Vở tập tô',N'Quyển',9000,12000);
  IF NOT EXISTS(SELECT 1 FROM dbo.HangHoa WHERE MaCoSo=@TT AND MaSanPham='DEMO-TT-01')
    INSERT dbo.HangHoa(MaCoSo,MaSanPham,TenHangHoa,DonViTinh,DonGiaSi,DonGiaXuatBan)
    VALUES(@TT,'DEMO-TT-01',N'[MẪU] Bút chì',N'Cây',3000,5000);
  COMMIT;
  PRINT N'DA TAO danh muc/hoc vien du lieu mau. CHUA tao tai khoan hay chung tu thu tien.';
END TRY
BEGIN CATCH
  IF XACT_STATE()<>0 ROLLBACK;
  THROW;
END CATCH;
GO

-- Mau tao goi nam chi duoc thuc hien SAU KHI co tai khoan Admin hop le tai Trung tam.
-- Vi khong tao mat khau mau, lan cai dau quy trinh nay se BO QUA an toan.
DECLARE @MaAdmin INT;
SELECT TOP(1) @MaAdmin=tc.MaTaiKhoan
FROM dbo.TaiKhoanCoSo tc
JOIN dbo.VaiTro vt ON vt.MaVaiTro=tc.MaVaiTro
JOIN dbo.CoSo cs ON cs.MaCoSo=tc.MaCoSo
JOIN dbo.TaiKhoan t ON t.MaTaiKhoan=tc.MaTaiKhoan
WHERE cs.LoaiCoSo='TRUNG_TAM' AND vt.MaVaiTroChu='ADMIN'
  AND tc.TrangThai=1 AND t.TrangThai=1;
IF @MaAdmin IS NULL
    PRINT N'Khong co Admin Trung tam; bo qua dang ky goi nam mau. Tao Admin an toan bang ung dung.';
ELSE IF NOT EXISTS(SELECT 1 FROM dbo.DangKyGoiNam WHERE MaGiaoDichNghiepVu='VINABACUS-DEMO-GOI-2026-001')
BEGIN
    DECLARE @DS dbo.TVP_KhoaTrongGoiNam;
    DECLARE @MaHocVien INT=(SELECT TOP(1) MaHocVien FROM dbo.HocVien WHERE HoTen=N'[MẪU] Nguyễn Minh Anh');
    DECLARE @CoSoTT INT=(SELECT MaCoSo FROM dbo.CoSo WHERE LoaiCoSo='TRUNG_TAM');
    DECLARE @K1 INT=(SELECT MaKhoaHoc FROM dbo.KhoaHoc WHERE MaCoSo=@CoSoTT AND TenKhoaHoc=N'[MẪU] Kid 1');
    DECLARE @K2 INT=(SELECT MaKhoaHoc FROM dbo.KhoaHoc WHERE MaCoSo=@CoSoTT AND TenKhoaHoc=N'[MẪU] Kid 2');
    DECLARE @Lop INT=(SELECT TOP(1) MaLop FROM dbo.LopTrungTam WHERE MaCoSo=@CoSoTT AND TenLop=N'[MẪU] Lớp Toán A');
    INSERT @DS(SoThuTu,MaKhoaHoc,MaLop,NgayBatDauDuKien,HocPhiThoaThuan)
    VALUES(1,@K1,@Lop,'20261110',2000000),(2,@K2,NULL,'20270301',3000000);
    EXEC dbo.usp_DangKyGoiNam
      @MaHocVien=@MaHocVien,@MaCoSo=@CoSoTT,@TenGoi=N'[MẪU] Gói Toán tư duy 1 năm',
      @NgayDangKy='20261110',@NgayBatDau='20261110',@MaTaiKhoan=@MaAdmin,
      @MaGiaoDichNghiepVu='VINABACUS-DEMO-GOI-2026-001',@DanhSach=@DS;
    PRINT N'Da tao goi nam MAU (5.000.000d, uu dai 15% = 750.000d, thu 4.250.000d)';
END;
GO
