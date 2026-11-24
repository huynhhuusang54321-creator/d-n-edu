/* VINABACUS V2 | SQL Server 2017+ | 12 bang, du lieu thu nghiem
   Chay toan bo file trong SSMS. KHONG dung database VINABACUS cu.
   Neu database TEST da ton tai, dung de tranh ghi de. Khong co DROP.
   Mat khau mau NULL: tai khoan mau khong dang nhap duoc cho den khi app cap hash.
   Quy tac tinh phi mam non, hoc bu, chuyen khoa, chot ky: chua chot nghiep vu.
*/
USE master;
GO
IF DB_ID(N'VINABACUS_V2_TEST') IS NULL
    EXEC(N'CREATE DATABASE [VINABACUS_V2_TEST]');
GO
USE [VINABACUS_V2_TEST];
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
IF DB_NAME() <> N'VINABACUS_V2_TEST'
    THROW 50000, N'Sai database đích. Dừng cài đặt.', 1;
IF EXISTS (SELECT 1 FROM sys.tables WHERE is_ms_shipped=0)
    THROW 50001, N'Database TEST đã có bảng. Không ghi đè dữ liệu. Hãy dùng database thử nghiệm trống.', 1;
BEGIN TRY
BEGIN TRANSACTION;

-- 01. CO SO: nhieu co so cung loai duoc phep.
CREATE TABLE dbo.CoSo (
    MaCoSo INT IDENTITY PRIMARY KEY,
    MaCoSoHienThi VARCHAR(20) NOT NULL UNIQUE,
    TenCoSo NVARCHAR(200) NOT NULL,
    LoaiCoSo VARCHAR(15) NOT NULL CHECK (LoaiCoSo IN ('TRUNG_TAM','MAM_NON')),
    DiaChi NVARCHAR(300) NULL,
    SoDienThoai VARCHAR(20) NULL,
    Logo NVARCHAR(500) NULL,
    TrangThai BIT NOT NULL DEFAULT 1
);
-- 02. NHAN SU: mot nhan su/mot co so trong V2.
CREATE TABLE dbo.NhanSu (
    MaNhanSu INT IDENTITY PRIMARY KEY,
    MaCoSo INT NOT NULL REFERENCES dbo.CoSo(MaCoSo),
    HoTen NVARCHAR(150) NOT NULL,
    NgaySinh DATE NULL,
    GioiTinh VARCHAR(10) NULL CHECK (GioiTinh IN ('NAM','NU','KHAC')),
    SoDienThoai VARCHAR(20) NULL,
    Email NVARCHAR(254) NULL,
    DiaChi NVARCHAR(300) NULL,
    AnhDaiDien NVARCHAR(500) NULL,
    TenDangNhap NVARCHAR(100) NOT NULL UNIQUE,
    MatKhauBam NVARCHAR(500) NULL,
    VaiTro VARCHAR(20) NOT NULL CHECK (VaiTro IN ('ADMIN','TO_TRUONG','GIAO_VIEN','NHAN_VIEN')),
    ChoPhepDangNhap BIT NOT NULL DEFAULT 0,
    TrangThai BIT NOT NULL DEFAULT 1,
    NgayTao DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    UNIQUE (MaNhanSu,MaCoSo),
    CHECK (ChoPhepDangNhap=0 OR MatKhauBam IS NOT NULL)
);
-- 03. DANH MUC: mot bang, cac muc duoc phan theo nhom va co so.
-- MaCoSo bat buoc: muc dung chung duoc seed cho tung co so de FK luon an toan.
-- CauHinh chi chua mac dinh, khong chua giao dich/lich su.
CREATE TABLE dbo.DanhMuc (
    MaDanhMuc INT IDENTITY PRIMARY KEY,
    MaCoSo INT NOT NULL REFERENCES dbo.CoSo(MaCoSo),
    NhomDanhMuc VARCHAR(30) NOT NULL,
    MaKyHieu VARCHAR(40) NOT NULL,
    TenDanhMuc NVARCHAR(200) NOT NULL,
    MaCha INT NULL,
    ThuTu INT NOT NULL DEFAULT 0,
    CauHinh NVARCHAR(MAX) NULL CHECK (CauHinh IS NULL OR ISJSON(CauHinh)=1),
    TrangThai BIT NOT NULL DEFAULT 1,
    UNIQUE (MaCoSo,NhomDanhMuc,MaKyHieu),
    UNIQUE (MaDanhMuc,MaCoSo),
    FOREIGN KEY (MaCha,MaCoSo) REFERENCES dbo.DanhMuc(MaDanhMuc,MaCoSo),
    CHECK (MaCha IS NULL OR MaCha<>MaDanhMuc)
);
-- 04. HOC VIEN: ho so tu luc tiep nhan, khong copy khi chuyen trang thai.
CREATE TABLE dbo.HocVien (
    MaHocVien INT IDENTITY PRIMARY KEY,
    MaCoSo INT NOT NULL REFERENCES dbo.CoSo(MaCoSo),
    MaHoSo VARCHAR(30) NOT NULL,
    HoTen NVARCHAR(150) NOT NULL,
    NgaySinh DATE NULL,
    GioiTinh VARCHAR(10) NULL CHECK (GioiTinh IN ('NAM','NU','KHAC')),
    AnhDaiDien NVARCHAR(500) NULL,
    TenPhuHuynh NVARCHAR(150) NULL,
    SoDienThoai VARCHAR(20) NULL,
    Email NVARCHAR(254) NULL,
    DiaChi NVARCHAR(300) NULL,
    MaNguonTiepNhan INT NULL,
    MaNguoiTuVan INT NULL,
    MaNguoiTao INT NOT NULL,
    NgayTao DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    NgayChinhThuc DATE NULL,
    TrangThai VARCHAR(25) NOT NULL DEFAULT 'TIEP_NHAN' CHECK (TrangThai IN ('TIEP_NHAN','HOC_THU','CHO_XEP_LOP','CHINH_THUC','TAM_NGHI','NGHI_HOC','KHONG_DANG_KY')),
    GhiChu NVARCHAR(1000) NULL,
    UNIQUE (MaCoSo,MaHoSo), UNIQUE (MaHocVien,MaCoSo),
    FOREIGN KEY (MaNguonTiepNhan,MaCoSo) REFERENCES dbo.DanhMuc(MaDanhMuc,MaCoSo),
    FOREIGN KEY (MaNguoiTuVan,MaCoSo) REFERENCES dbo.NhanSu(MaNhanSu,MaCoSo),
    FOREIGN KEY (MaNguoiTao,MaCoSo) REFERENCES dbo.NhanSu(MaNhanSu,MaCoSo),
    CHECK (TrangThai<>'CHINH_THUC' OR NgayChinhThuc IS NOT NULL)
);
-- 05. TUONG TAC: nhieu lan tu van, nguyen vong va lich su trang thai.
CREATE TABLE dbo.TuongTacHocVien (
    MaTuongTac INT IDENTITY PRIMARY KEY,
    MaCoSo INT NOT NULL,
    MaHocVien INT NOT NULL,
    MaNhanSu INT NOT NULL,
    LoaiTuongTac VARCHAR(25) NOT NULL CHECK (LoaiTuongTac IN ('TIEP_NHAN','TU_VAN','NGUYEN_VONG','DOI_TRANG_THAI','GHI_CHU')),
    MaKhoaHocMongMuon INT NULL,
    ThoiGianMongMuon NVARCHAR(300) NULL,
    NoiDung NVARCHAR(1000) NOT NULL,
    KetQua NVARCHAR(500) NULL,
    TrangThaiTruoc VARCHAR(25) NULL,
    TrangThaiSau VARCHAR(25) NULL,
    NgayHenTiep DATETIME2(0) NULL,
    NgayTao DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    FOREIGN KEY (MaHocVien,MaCoSo) REFERENCES dbo.HocVien(MaHocVien,MaCoSo),
    FOREIGN KEY (MaNhanSu,MaCoSo) REFERENCES dbo.NhanSu(MaNhanSu,MaCoSo),
    FOREIGN KEY (MaKhoaHocMongMuon,MaCoSo) REFERENCES dbo.DanhMuc(MaDanhMuc,MaCoSo),
    CHECK (LoaiTuongTac<>'DOI_TRANG_THAI' OR (TrangThaiTruoc IS NOT NULL AND TrangThaiSau IS NOT NULL AND TrangThaiTruoc<>TrangThaiSau))
);
-- 06. LOP HOC: ca hai loai co so, mot giao vien phu trach chinh.
CREATE TABLE dbo.LopHoc (
    MaLop INT IDENTITY PRIMARY KEY,
    MaCoSo INT NOT NULL REFERENCES dbo.CoSo(MaCoSo),
    MaLopHienThi VARCHAR(30) NOT NULL,
    TenLop NVARCHAR(150) NOT NULL,
    MaKhoaHoc INT NULL,
    MaNamHoc INT NULL,
    MaGiaoVien INT NULL,
    SiSoToiDa SMALLINT NULL CHECK (SiSoToiDa>0),
    NgayBatDau DATE NOT NULL,
    NgayKetThuc DATE NULL,
    HocPhiMacDinh DECIMAL(18,2) NOT NULL DEFAULT 0 CHECK (HocPhiMacDinh>=0),
    TienAnMacDinh DECIMAL(18,2) NOT NULL DEFAULT 0 CHECK (TienAnMacDinh>=0),
    TrangThai VARCHAR(15) NOT NULL DEFAULT 'DANG_MO' CHECK (TrangThai IN ('DANG_MO','TAM_DUNG','DA_DONG')),
    GhiChu NVARCHAR(500) NULL,
    UNIQUE (MaCoSo,MaLopHienThi), UNIQUE (MaLop,MaCoSo),
    FOREIGN KEY (MaKhoaHoc,MaCoSo) REFERENCES dbo.DanhMuc(MaDanhMuc,MaCoSo),
    FOREIGN KEY (MaNamHoc,MaCoSo) REFERENCES dbo.DanhMuc(MaDanhMuc,MaCoSo),
    FOREIGN KEY (MaGiaoVien,MaCoSo) REFERENCES dbo.NhanSu(MaNhanSu,MaCoSo),
    CHECK (NgayKetThuc IS NULL OR NgayKetThuc>=NgayBatDau)
);
-- 07. DANG KY: hop dong khoa hoc + lich su xep lop.
-- Dong goc giu hoc phi/so buoi. Dong noi tiep cung khoa tro ve goc, khong tao phi lan 2.
-- Chuyen khac khoa: tao dang ky goc moi; MaDangKyTruoc co the tro ve dang ky cu.
CREATE TABLE dbo.DangKyHoc (
    MaDangKy INT IDENTITY PRIMARY KEY,
    MaCoSo INT NOT NULL,
    MaHocVien INT NOT NULL,
    MaKhoaHoc INT NULL,
    MaLop INT NULL,
    MaDangKyGoc INT NULL,
    MaDangKyTruoc INT NULL,
    NgayDangKy DATE NOT NULL,
    NgayBatDau DATE NULL,
    NgayKetThucDuKien DATE NULL,
    NgayKetThucThucTe DATE NULL,
    TongSoBuoi INT NULL CHECK (TongSoBuoi>0),
    HocPhiThoaThuan DECIMAL(18,2) NULL CHECK (HocPhiThoaThuan>=0),
    SoTienGiam DECIMAL(18,2) NOT NULL DEFAULT 0 CHECK (SoTienGiam>=0),
    MaNguoiDangKy INT NOT NULL,
    TrangThai VARCHAR(25) NOT NULL DEFAULT 'CHO_XEP_LOP' CHECK (TrangThai IN ('CHO_XEP_LOP','DANG_HOC','HOAN_THANH','DA_CHUYEN','TAM_NGHI','NGHI_HOC','HUY')),
    GhiChu NVARCHAR(500) NULL,
    UNIQUE (MaDangKy,MaHocVien,MaCoSo),
    FOREIGN KEY (MaHocVien,MaCoSo) REFERENCES dbo.HocVien(MaHocVien,MaCoSo),
    FOREIGN KEY (MaLop,MaCoSo) REFERENCES dbo.LopHoc(MaLop,MaCoSo),
    FOREIGN KEY (MaKhoaHoc,MaCoSo) REFERENCES dbo.DanhMuc(MaDanhMuc,MaCoSo),
    FOREIGN KEY (MaNguoiDangKy,MaCoSo) REFERENCES dbo.NhanSu(MaNhanSu,MaCoSo),
    FOREIGN KEY (MaDangKyGoc,MaHocVien,MaCoSo) REFERENCES dbo.DangKyHoc(MaDangKy,MaHocVien,MaCoSo),
    FOREIGN KEY (MaDangKyTruoc,MaHocVien,MaCoSo) REFERENCES dbo.DangKyHoc(MaDangKy,MaHocVien,MaCoSo),
    CHECK (MaDangKyGoc IS NULL OR (MaDangKyGoc<>MaDangKy AND MaDangKyTruoc IS NOT NULL AND TongSoBuoi IS NULL AND HocPhiThoaThuan IS NULL AND SoTienGiam=0)),
    CHECK (MaDangKyTruoc IS NULL OR MaDangKyTruoc<>MaDangKy),
    CHECK (SoTienGiam<=ISNULL(HocPhiThoaThuan,0)),
    CHECK (NgayKetThucDuKien IS NULL OR NgayBatDau IS NULL OR NgayKetThucDuKien>=NgayBatDau),
    CHECK (NgayKetThucThucTe IS NULL OR NgayBatDau IS NULL OR NgayKetThucThucTe>=NgayBatDau),
    CHECK (TrangThai<>'DANG_HOC' OR (MaLop IS NOT NULL AND NgayBatDau IS NOT NULL))
);
-- 08. THOI KHOA BIEU: thu ISO, 1=thu hai, 7=chu nhat.
CREATE TABLE dbo.ThoiKhoaBieu (
    MaThoiKhoaBieu INT IDENTITY PRIMARY KEY,
    MaCoSo INT NOT NULL,
    MaLop INT NOT NULL,
    ThuTrongTuan TINYINT NOT NULL CHECK (ThuTrongTuan BETWEEN 1 AND 7),
    MaCaHoc INT NOT NULL,
    TuNgay DATE NOT NULL,
    DenNgay DATE NULL,
    TrangThai BIT NOT NULL DEFAULT 1,
    UNIQUE (MaLop,ThuTrongTuan,MaCaHoc,TuNgay),
    UNIQUE (MaThoiKhoaBieu,MaLop,MaCoSo),
    FOREIGN KEY (MaLop,MaCoSo) REFERENCES dbo.LopHoc(MaLop,MaCoSo),
    FOREIGN KEY (MaCaHoc,MaCoSo) REFERENCES dbo.DanhMuc(MaDanhMuc,MaCoSo),
    CHECK (DenNgay IS NULL OR DenNgay>=TuNgay)
);
-- 09. BUOI HOC: khong sinh hang loat; DU_KIEN chi khi can giu cho (hoc thu...).
CREATE TABLE dbo.BuoiHoc (
    MaBuoiHoc INT IDENTITY PRIMARY KEY,
    MaCoSo INT NOT NULL,
    MaLop INT NOT NULL,
    MaThoiKhoaBieu INT NULL,
    NgayHoc DATE NOT NULL,
    MaCaHoc INT NOT NULL,
    GioBatDau TIME(0) NOT NULL,
    GioKetThuc TIME(0) NOT NULL,
    MaGiaoVien INT NULL,
    MaNguoiKichHoat INT NULL,
    TrangThai VARCHAR(15) NOT NULL DEFAULT 'DU_KIEN' CHECK (TrangThai IN ('DU_KIEN','DA_KICH_HOAT','HOAN_THANH','HUY')),
    NgayKichHoat DATETIME2(0) NULL,
    GhiChu NVARCHAR(500) NULL,
    UNIQUE (MaLop,NgayHoc,MaCaHoc), UNIQUE (MaBuoiHoc,MaCoSo),
    FOREIGN KEY (MaLop,MaCoSo) REFERENCES dbo.LopHoc(MaLop,MaCoSo),
    FOREIGN KEY (MaThoiKhoaBieu,MaLop,MaCoSo) REFERENCES dbo.ThoiKhoaBieu(MaThoiKhoaBieu,MaLop,MaCoSo),
    FOREIGN KEY (MaCaHoc,MaCoSo) REFERENCES dbo.DanhMuc(MaDanhMuc,MaCoSo),
    FOREIGN KEY (MaGiaoVien,MaCoSo) REFERENCES dbo.NhanSu(MaNhanSu,MaCoSo),
    FOREIGN KEY (MaNguoiKichHoat,MaCoSo) REFERENCES dbo.NhanSu(MaNhanSu,MaCoSo),
    CHECK (GioKetThuc>GioBatDau),
    CHECK (TrangThai NOT IN ('DA_KICH_HOAT','HOAN_THANH') OR (MaNguoiKichHoat IS NOT NULL AND NgayKichHoat IS NOT NULL))
);
-- 10. DIEM DANH: ca lich tham gia hoc thu va ket qua diem danh.
-- TinhBuoi = quyet dinh nghiep vu; khong mac dinh vang la mat buoi.
CREATE TABLE dbo.DiemDanh (
    MaDiemDanh INT IDENTITY PRIMARY KEY,
    MaCoSo INT NOT NULL,
    MaHocVien INT NOT NULL,
    MaBuoiHoc INT NOT NULL,
    MaDangKy INT NULL,
    LoaiThamGia VARCHAR(20) NOT NULL CHECK (LoaiThamGia IN ('CHINH_THUC','HOC_THU','HOC_BU','DOI_BUOI','BO_SUNG')),
    TrangThai VARCHAR(25) NOT NULL DEFAULT 'CHUA_DIEM_DANH' CHECK (TrangThai IN ('CHUA_DIEM_DANH','CO_MAT','VANG_CO_PHEP','VANG_KHONG_PHEP','HUY')),
    TinhBuoi BIT NOT NULL DEFAULT 0,
    MaDiemDanhNguon INT NULL,
    MaNguoiCapNhat INT NOT NULL,
    NgayCapNhat DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    GhiChu NVARCHAR(500) NULL,
    UNIQUE (MaHocVien,MaBuoiHoc), UNIQUE (MaDiemDanh,MaHocVien,MaCoSo),
    FOREIGN KEY (MaHocVien,MaCoSo) REFERENCES dbo.HocVien(MaHocVien,MaCoSo),
    FOREIGN KEY (MaBuoiHoc,MaCoSo) REFERENCES dbo.BuoiHoc(MaBuoiHoc,MaCoSo),
    FOREIGN KEY (MaDangKy,MaHocVien,MaCoSo) REFERENCES dbo.DangKyHoc(MaDangKy,MaHocVien,MaCoSo),
    FOREIGN KEY (MaDiemDanhNguon,MaHocVien,MaCoSo) REFERENCES dbo.DiemDanh(MaDiemDanh,MaHocVien,MaCoSo),
    FOREIGN KEY (MaNguoiCapNhat,MaCoSo) REFERENCES dbo.NhanSu(MaNhanSu,MaCoSo),
    CHECK ((LoaiThamGia='HOC_THU' AND MaDangKy IS NULL) OR (LoaiThamGia<>'HOC_THU' AND MaDangKy IS NOT NULL)),
    CHECK (TinhBuoi=0 OR (LoaiThamGia<>'HOC_THU' AND TrangThai NOT IN ('CHUA_DIEM_DANH','HUY'))),
    CHECK (MaDiemDanhNguon IS NULL OR MaDiemDanhNguon<>MaDiemDanh),
    CHECK (LoaiThamGia NOT IN ('HOC_BU','DOI_BUOI') OR MaDiemDanhNguon IS NOT NULL)
);
-- 11. TAI CHINH: mot dong = mot khoan phat sinh / mot phan bo tien.
-- PHAI_THU khong phai tien mat; THU/CHI/HOAN moi la dong tien.
-- THU hoc phi tro den PHAI_THU; HOAN tro den THU; DIEU_CHINH tro den PHAI_THU.
-- MaNhomChungTu gom nhieu dong thu (vi du 1 phieu thu 2 khoan).
CREATE TABLE dbo.GiaoDichTaiChinh (
    MaGiaoDich INT IDENTITY PRIMARY KEY,
    MaCoSo INT NOT NULL REFERENCES dbo.CoSo(MaCoSo),
    MaHocVien INT NULL,
    MaDangKy INT NULL,
    MaGiaoDichNguon INT NULL,
    MaNhomChungTu VARCHAR(50) NOT NULL,
    LoaiGiaoDich VARCHAR(15) NOT NULL CHECK (LoaiGiaoDich IN ('PHAI_THU','THU','CHI','HOAN','DIEU_CHINH')),
    MaLoaiPhi INT NOT NULL,
    MaHinhThuc INT NULL,
    KyPhi DATE NULL,
    SoLuong DECIMAL(12,3) NULL CHECK (SoLuong>=0),
    DonGia DECIMAL(18,2) NULL CHECK (DonGia>=0),
    SoTien DECIMAL(18,2) NOT NULL,
    NgayGiaoDich DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    HanThanhToan DATE NULL,
    MaNhanSu INT NOT NULL,
    TrangThai VARCHAR(15) NOT NULL DEFAULT 'XAC_NHAN' CHECK (TrangThai IN ('NHAP','XAC_NHAN','HUY')),
    DienGiai NVARCHAR(500) NOT NULL,
    UNIQUE (MaGiaoDich,MaCoSo),
    FOREIGN KEY (MaHocVien,MaCoSo) REFERENCES dbo.HocVien(MaHocVien,MaCoSo),
    FOREIGN KEY (MaDangKy,MaHocVien,MaCoSo) REFERENCES dbo.DangKyHoc(MaDangKy,MaHocVien,MaCoSo),
    FOREIGN KEY (MaGiaoDichNguon,MaCoSo) REFERENCES dbo.GiaoDichTaiChinh(MaGiaoDich,MaCoSo),
    FOREIGN KEY (MaLoaiPhi,MaCoSo) REFERENCES dbo.DanhMuc(MaDanhMuc,MaCoSo),
    FOREIGN KEY (MaHinhThuc,MaCoSo) REFERENCES dbo.DanhMuc(MaDanhMuc,MaCoSo),
    FOREIGN KEY (MaNhanSu,MaCoSo) REFERENCES dbo.NhanSu(MaNhanSu,MaCoSo),
    CHECK (MaDangKy IS NULL OR MaHocVien IS NOT NULL),
    CHECK (LoaiGiaoDich NOT IN ('PHAI_THU','DIEU_CHINH','HOAN') OR MaHocVien IS NOT NULL),
    CHECK (LoaiGiaoDich NOT IN ('HOAN','DIEU_CHINH') OR MaGiaoDichNguon IS NOT NULL),
    CHECK (LoaiGiaoDich NOT IN ('PHAI_THU','CHI') OR MaGiaoDichNguon IS NULL),
    CHECK (MaGiaoDichNguon IS NULL OR MaGiaoDichNguon<>MaGiaoDich),
    CHECK ((LoaiGiaoDich='DIEU_CHINH' AND SoTien<>0) OR (LoaiGiaoDich<>'DIEU_CHINH' AND SoTien>0)),
    CHECK (KyPhi IS NULL OR DAY(KyPhi)=1),
    CHECK (LoaiGiaoDich NOT IN ('THU','CHI','HOAN') OR MaHinhThuc IS NOT NULL)
);
-- 12. KHO: mot dong = mot mat hang trong chung tu, san pham nam trong DanhMuc.
-- Kiem ke luu chenh lech co dau; ton kho = tong bien dong da xac nhan.
CREATE TABLE dbo.GiaoDichKho (
    MaGiaoDichKho INT IDENTITY PRIMARY KEY,
    MaCoSo INT NOT NULL REFERENCES dbo.CoSo(MaCoSo),
    MaNhomChungTu VARCHAR(50) NOT NULL,
    MaHangHoa INT NOT NULL,
    MaHocVien INT NULL,
    MaGiaoDichTaiChinh INT NULL,
    LoaiGiaoDich VARCHAR(15) NOT NULL CHECK (LoaiGiaoDich IN ('NHAP','XUAT','TON_DAU','KIEM_KE','DIEU_CHINH')),
    SoLuong DECIMAL(18,3) NOT NULL,
    TonThucTe DECIMAL(18,3) NULL CHECK (TonThucTe>=0),
    DonGia DECIMAL(18,2) NOT NULL DEFAULT 0 CHECK (DonGia>=0),
    NgayGiaoDich DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    MaNhanSu INT NOT NULL,
    TrangThai VARCHAR(15) NOT NULL DEFAULT 'XAC_NHAN' CHECK (TrangThai IN ('NHAP','XAC_NHAN','HUY')),
    GhiChu NVARCHAR(500) NULL,
    FOREIGN KEY (MaHangHoa,MaCoSo) REFERENCES dbo.DanhMuc(MaDanhMuc,MaCoSo),
    FOREIGN KEY (MaHocVien,MaCoSo) REFERENCES dbo.HocVien(MaHocVien,MaCoSo),
    FOREIGN KEY (MaGiaoDichTaiChinh,MaCoSo) REFERENCES dbo.GiaoDichTaiChinh(MaGiaoDich,MaCoSo),
    FOREIGN KEY (MaNhanSu,MaCoSo) REFERENCES dbo.NhanSu(MaNhanSu,MaCoSo),
    CHECK ((LoaiGiaoDich IN ('NHAP','TON_DAU') AND SoLuong>0) OR (LoaiGiaoDich='XUAT' AND SoLuong<0) OR (LoaiGiaoDich='DIEU_CHINH' AND SoLuong<>0) OR (LoaiGiaoDich='KIEM_KE' AND TonThucTe IS NOT NULL)),
    CHECK (LoaiGiaoDich='KIEM_KE' OR TonThucTe IS NULL)
);

-- Chi muc cho tra cuu theo ho so/co so. Khong phai bang moi.
CREATE INDEX IX_HocVien_CoSoTrangThai ON dbo.HocVien(MaCoSo,TrangThai);
CREATE INDEX IX_TuongTac_HocVien ON dbo.TuongTacHocVien(MaHocVien,NgayTao);
CREATE INDEX IX_DangKy_HocVien ON dbo.DangKyHoc(MaHocVien,TrangThai);
CREATE INDEX IX_DangKy_Lop ON dbo.DangKyHoc(MaLop,TrangThai);
CREATE INDEX IX_BuoiHoc_CoSoNgay ON dbo.BuoiHoc(MaCoSo,NgayHoc);
CREATE INDEX IX_DiemDanh_Buoi ON dbo.DiemDanh(MaBuoiHoc,TrangThai);
CREATE INDEX IX_TaiChinh_HocVien ON dbo.GiaoDichTaiChinh(MaHocVien,NgayGiaoDich);
CREATE INDEX IX_TaiChinh_Nguon ON dbo.GiaoDichTaiChinh(MaGiaoDichNguon,TrangThai);
CREATE INDEX IX_TaiChinh_CoSoNgay ON dbo.GiaoDichTaiChinh(MaCoSo,NgayGiaoDich);
CREATE INDEX IX_Kho_Hang ON dbo.GiaoDichKho(MaCoSo,MaHangHoa,NgayGiaoDich);

-- Mot so rang buoc nghiep vu khong the dien dat bang FK thong thuong.
-- Trigger kiem tra loai giao dich nguon va cung hoc vien, khong tu dong tinh phi.
EXEC(N'CREATE TRIGGER dbo.TR_TaiChinh_Nguon ON dbo.GiaoDichTaiChinh AFTER INSERT, UPDATE AS
BEGIN
 SET NOCOUNT ON;
 IF EXISTS (SELECT 1 FROM dbo.GiaoDichTaiChinh g JOIN dbo.GiaoDichTaiChinh n ON n.MaGiaoDich=g.MaGiaoDichNguon
 WHERE (g.MaGiaoDich IN (SELECT MaGiaoDich FROM inserted) OR n.MaGiaoDich IN (SELECT MaGiaoDich FROM inserted)) AND
 (ISNULL(g.MaHocVien,-1)<>ISNULL(n.MaHocVien,-1) OR g.MaLoaiPhi<>n.MaLoaiPhi OR
 (g.LoaiGiaoDich IN (''THU'',''DIEU_CHINH'') AND n.LoaiGiaoDich<>''PHAI_THU'') OR
 (g.LoaiGiaoDich=''HOAN'' AND n.LoaiGiaoDich<>''THU'') OR
 (g.TrangThai=''XAC_NHAN'' AND n.TrangThai<>''XAC_NHAN'')))
 THROW 50002, N''Giao dịch nguồn không hợp lệ hoặc khác học viên/loại phí.'',1;
END');
EXEC(N'CREATE TRIGGER dbo.TR_DangKy_LienKet ON dbo.DangKyHoc AFTER INSERT, UPDATE AS
BEGIN
 SET NOCOUNT ON;
 IF EXISTS (SELECT 1 FROM dbo.DangKyHoc d JOIN dbo.DangKyHoc g ON d.MaDangKyGoc=g.MaDangKy
 JOIN dbo.DangKyHoc t ON d.MaDangKyTruoc=t.MaDangKy
 WHERE (d.MaDangKy IN (SELECT MaDangKy FROM inserted) OR g.MaDangKy IN (SELECT MaDangKy FROM inserted) OR t.MaDangKy IN (SELECT MaDangKy FROM inserted))
 AND (g.MaDangKyGoc IS NOT NULL OR ISNULL(d.MaKhoaHoc,-1)<>ISNULL(g.MaKhoaHoc,-1) OR ISNULL(t.MaDangKyGoc,t.MaDangKy)<>g.MaDangKy OR t.MaDangKy>=d.MaDangKy))
 THROW 50003, N''Chuỗi đăng ký chuyển lớp không hợp lệ.'',1;
 IF EXISTS (SELECT 1 FROM inserted d JOIN dbo.LopHoc l ON l.MaLop=d.MaLop WHERE ISNULL(d.MaKhoaHoc,-1)<>ISNULL(l.MaKhoaHoc,-1))
 THROW 50004, N''Khóa học đăng ký khác khóa học của lớp.'',1;
END');

-- ====================== DU LIEU MAU ======================
-- ID co dinh de doc va kiem thu de dang; du lieu hoan toan gia lap.
SET IDENTITY_INSERT dbo.CoSo ON;
INSERT dbo.CoSo(MaCoSo,MaCoSoHienThi,TenCoSo,LoaiCoSo,DiaChi) VALUES
(1,'CS01',N'VINABACUS Trung tâm','TRUNG_TAM',N'Cần Thơ (mẫu)'),
(2,'CS02',N'VINABACUS Mầm non','MAM_NON',N'Cần Thơ (mẫu)');
SET IDENTITY_INSERT dbo.CoSo OFF;
SET IDENTITY_INSERT dbo.NhanSu ON;
INSERT dbo.NhanSu(MaNhanSu,MaCoSo,HoTen,TenDangNhap,VaiTro) VALUES
(1,1,N'Nhân viên tư vấn 01','tuvan01','NHAN_VIEN'),(2,1,N'Giáo viên trung tâm 01','gv01','GIAO_VIEN'),(3,1,N'Quản trị trung tâm','admin01','ADMIN'),
(4,2,N'Nhân viên mầm non 01','tuvan02','NHAN_VIEN'),(5,2,N'Giáo viên mầm non 01','gv02','GIAO_VIEN'),(6,2,N'Quản trị mầm non','admin02','ADMIN');
SET IDENTITY_INSERT dbo.NhanSu OFF;
SET IDENTITY_INSERT dbo.DanhMuc ON;
INSERT dbo.DanhMuc(MaDanhMuc,MaCoSo,NhomDanhMuc,MaKyHieu,TenDanhMuc,CauHinh) VALUES
(1,1,'MON_HOC','ABACUS',N'Toán tư duy',NULL),
(2,1,'KHOA_HOC','LV1',N'Abacus Level 1',N'{"TongSoBuoi":24,"HocPhiMacDinh":3500000}'),
(3,1,'KHOA_HOC','LV2',N'Abacus Level 2',N'{"TongSoBuoi":24,"HocPhiMacDinh":4000000}'),
(4,1,'CA_HOC','S1',N'Sáng 1',N'{"GioBatDau":"08:00","GioKetThuc":"09:30"}'),
(5,1,'CA_HOC','C1',N'Chiều 1',N'{"GioBatDau":"14:00","GioKetThuc":"15:30"}'),
(6,1,'NGUON_TIEP_NHAN','GT',N'Phụ huynh giới thiệu',NULL),
(7,1,'THANH_TOAN','TM',N'Tiền mặt',NULL),(8,1,'THANH_TOAN','CK',N'Chuyển khoản',NULL),
(9,1,'LOAI_PHI','HP',N'Học phí khóa',NULL),(10,1,'LOAI_CHI','VPP',N'Văn phòng phẩm',NULL),
(11,1,'HANG_HOA','BT',N'Bàn tính',N'{"DonViTinh":"Cái","GiaNhapMacDinh":80000,"GiaBanMacDinh":120000}'),
(12,1,'HANG_HOA','GT1',N'Giáo trình Level 1',N'{"DonViTinh":"Quyển","GiaNhapMacDinh":40000}'),
(21,2,'NAM_HOC','2026_2027',N'2026–2027',N'{"TuNgay":"2026-09-01","DenNgay":"2027-05-31"}'),
(22,2,'CA_HOC','BT',N'Bán trú',N'{"GioBatDau":"07:00","GioKetThuc":"16:30"}'),
(23,2,'NGUON_TIEP_NHAN','TT',N'Trực tiếp',NULL),
(24,2,'THANH_TOAN','TM',N'Tiền mặt',NULL),(25,2,'THANH_TOAN','CK',N'Chuyển khoản',NULL),
(26,2,'LOAI_PHI','HP',N'Học phí tháng',NULL),(27,2,'LOAI_PHI','TA',N'Tiền ăn',NULL),
(28,2,'LOAI_CHI','TP',N'Thực phẩm',NULL),
(29,2,'HANG_HOA','BUT',N'Bút màu',N'{"DonViTinh":"Hộp","GiaNhapMacDinh":25000}'),
(30,2,'HANG_HOA','VO',N'Vở tập tô',N'{"DonViTinh":"Quyển","GiaNhapMacDinh":12000}');
UPDATE dbo.DanhMuc SET MaCha=1 WHERE MaDanhMuc IN (2,3);
SET IDENTITY_INSERT dbo.DanhMuc OFF;
SET IDENTITY_INSERT dbo.HocVien ON;
INSERT dbo.HocVien(MaHocVien,MaCoSo,MaHoSo,HoTen,NgaySinh,TenPhuHuynh,MaNguonTiepNhan,MaNguoiTuVan,MaNguoiTao,NgayTao,NgayChinhThuc,TrangThai,GhiChu) VALUES
(1,1,'HV001',N'Nguyễn Minh Anh','2018-03-15',N'Phụ huynh A',6,1,1,'2026-11-01T08:00:00','2026-11-09','CHINH_THUC',N'Đã qua tuyển sinh và học thử.'),
(2,1,'HV002',N'Trần Gia Huy','2019-07-10',N'Phụ huynh B',6,1,1,'2026-11-10T08:00:00',NULL,'HOC_THU',N'Đã vắng một buổi học thử, hẹn lại.'),
(3,1,'HV003',N'Lê Bảo Ngọc','2018-09-20',N'Phụ huynh C',6,1,1,'2026-11-20T08:00:00',NULL,'CHO_XEP_LOP',N'Đã đồng ý học phí, đang chờ lớp.'),
(4,2,'HV001',N'Phạm An Nhiên','2022-06-12',N'Phụ huynh D',NULL,NULL,4,'2026-11-01T08:00:00','2026-11-01','CHINH_THUC',N'Tạo hồ sơ trực tiếp, không qua tư vấn.'),
(5,2,'HV002',N'Võ Khánh Linh','2023-02-04',N'Phụ huynh E',23,4,4,'2026-11-02T08:00:00','2026-11-09','CHINH_THUC',N'Học bán trú.'),
(6,2,'HV003',N'Đỗ Minh Khang',NULL,N'Phụ huynh F',23,4,4,'2026-11-23T08:00:00',NULL,'TIEP_NHAN',N'Chưa bổ sung đầy đủ hồ sơ.');
SET IDENTITY_INSERT dbo.HocVien OFF;
INSERT dbo.TuongTacHocVien(MaCoSo,MaHocVien,MaNhanSu,LoaiTuongTac,MaKhoaHocMongMuon,ThoiGianMongMuon,NoiDung,KetQua,TrangThaiTruoc,TrangThaiSau,NgayTao) VALUES
(1,1,1,'TIEP_NHAN',2,N'Sáng thứ hai',N'Phụ huynh hỏi lớp Level 1.',NULL,NULL,NULL,'2026-11-01'),
(1,1,1,'DOI_TRANG_THAI',2,NULL,N'Xếp học thử.',NULL,'TIEP_NHAN','HOC_THU','2026-11-02'),
(1,1,1,'DOI_TRANG_THAI',2,NULL,N'Đồng ý đăng ký.',NULL,'HOC_THU','CHO_XEP_LOP','2026-11-05'),
(1,1,1,'DOI_TRANG_THAI',2,NULL,N'Hoàn tất xếp lớp.',NULL,'CHO_XEP_LOP','CHINH_THUC','2026-11-09'),
(1,2,1,'TIEP_NHAN',2,N'Chiều thứ hai',N'Muốn thử lớp.',NULL,NULL,NULL,'2026-11-10'),
(1,2,1,'DOI_TRANG_THAI',2,NULL,N'Đã thống nhất lịch thử.',NULL,'TIEP_NHAN','HOC_THU','2026-11-11'),
(1,2,1,'TU_VAN',2,NULL,N'Học viên vắng, hẹn buổi khác.',N'Hẹn ngày 30/11',NULL,NULL,'2026-11-16'),
(1,3,1,'TIEP_NHAN',3,N'Chiều',N'Quan tâm Level 2.',NULL,NULL,NULL,'2026-11-20'),
(1,3,1,'DOI_TRANG_THAI',3,NULL,N'Đồng ý mức phí.',NULL,'TIEP_NHAN','CHO_XEP_LOP','2026-11-21'),
(2,5,4,'TIEP_NHAN',NULL,N'Bán trú',N'Phụ huynh muốn nhập học.',NULL,NULL,NULL,'2026-11-02'),
(2,5,4,'DOI_TRANG_THAI',NULL,NULL,N'Đồng ý nhập học, chờ xếp lớp.',NULL,'TIEP_NHAN','CHO_XEP_LOP','2026-11-05'),
(2,5,4,'DOI_TRANG_THAI',NULL,NULL,N'Đã nhập lớp Mầm.',NULL,'CHO_XEP_LOP','CHINH_THUC','2026-11-09'),
(2,6,4,'TIEP_NHAN',NULL,N'Tháng 12, bán trú',N'Phụ huynh muốn tham quan trường.',NULL,NULL,NULL,'2026-11-23');
SET IDENTITY_INSERT dbo.LopHoc ON;
INSERT dbo.LopHoc(MaLop,MaCoSo,MaLopHienThi,TenLop,MaKhoaHoc,MaNamHoc,MaGiaoVien,SiSoToiDa,NgayBatDau,HocPhiMacDinh,TienAnMacDinh) VALUES
(1,1,'L001',N'Abacus Level 1 - sáng',2,NULL,2,15,'2026-11-01',3500000,0),
(2,1,'L002',N'Abacus Level 2 - chiều',3,NULL,2,15,'2026-11-01',4000000,0),
(3,1,'L003',N'Abacus Level 1 - chiều',2,NULL,2,15,'2026-11-01',3500000,0),
(4,2,'L001',N'Lớp Mầm',NULL,21,5,20,'2026-09-01',2000000,35000),
(5,2,'L002',N'Lớp Chồi',NULL,21,5,20,'2026-09-01',2200000,35000);
SET IDENTITY_INSERT dbo.LopHoc OFF;
SET IDENTITY_INSERT dbo.DangKyHoc ON;
INSERT dbo.DangKyHoc(MaDangKy,MaCoSo,MaHocVien,MaKhoaHoc,MaLop,MaDangKyGoc,MaDangKyTruoc,NgayDangKy,NgayBatDau,NgayKetThucDuKien,NgayKetThucThucTe,TongSoBuoi,HocPhiThoaThuan,SoTienGiam,MaNguoiDangKy,TrangThai) VALUES
(1,1,1,2,1,NULL,NULL,'2026-11-05','2026-11-09','2027-02-28','2026-11-15',24,3500000,200000,1,'DA_CHUYEN'),
(2,1,3,3,NULL,NULL,NULL,'2026-11-21',NULL,NULL,NULL,24,4000000,0,1,'CHO_XEP_LOP'),
(3,2,4,NULL,4,NULL,NULL,'2026-11-01','2026-11-01',NULL,NULL,NULL,2000000,0,4,'DANG_HOC'),
(4,2,5,NULL,4,NULL,NULL,'2026-11-05','2026-11-09',NULL,NULL,NULL,1800000,0,4,'DANG_HOC');
INSERT dbo.DangKyHoc(MaDangKy,MaCoSo,MaHocVien,MaKhoaHoc,MaLop,MaDangKyGoc,MaDangKyTruoc,NgayDangKy,NgayBatDau,NgayKetThucDuKien,MaNguoiDangKy,TrangThai,GhiChu) VALUES
(5,1,1,2,3,1,1,'2026-11-16','2026-11-16','2027-02-28',1,'DANG_HOC',N'Chuyển ca chiều cùng khóa, không phát sinh phí mới.');
SET IDENTITY_INSERT dbo.DangKyHoc OFF;
SET IDENTITY_INSERT dbo.ThoiKhoaBieu ON;
INSERT dbo.ThoiKhoaBieu(MaThoiKhoaBieu,MaCoSo,MaLop,ThuTrongTuan,MaCaHoc,TuNgay) VALUES
(1,1,1,1,4,'2026-11-01'),(2,1,1,3,4,'2026-11-01'),(3,1,2,6,5,'2026-11-01'),(4,1,3,1,5,'2026-11-01'),(5,1,3,3,5,'2026-11-01'),
(6,2,4,1,22,'2026-09-01'),(7,2,4,2,22,'2026-09-01'),(8,2,4,3,22,'2026-09-01'),(9,2,4,4,22,'2026-09-01'),(10,2,4,5,22,'2026-09-01'),
(11,2,5,1,22,'2026-09-01'),(12,2,5,2,22,'2026-09-01'),(13,2,5,3,22,'2026-09-01'),(14,2,5,4,22,'2026-09-01'),(15,2,5,5,22,'2026-09-01');
SET IDENTITY_INSERT dbo.ThoiKhoaBieu OFF;
SET IDENTITY_INSERT dbo.BuoiHoc ON;
INSERT dbo.BuoiHoc(MaBuoiHoc,MaCoSo,MaLop,MaThoiKhoaBieu,NgayHoc,MaCaHoc,GioBatDau,GioKetThuc,MaGiaoVien,MaNguoiKichHoat,TrangThai,NgayKichHoat) VALUES
(1,1,1,2,'2026-11-04',4,'08:00','09:30',2,2,'HOAN_THANH','2026-11-04T01:00:00'),
(2,1,1,1,'2026-11-09',4,'08:00','09:30',2,2,'HOAN_THANH','2026-11-09T01:00:00'),
(3,1,3,4,'2026-11-16',5,'14:00','15:30',2,2,'HOAN_THANH','2026-11-16T07:00:00'),
(4,1,3,5,'2026-11-18',5,'14:00','15:30',2,2,'HOAN_THANH','2026-11-18T07:00:00'),
(5,1,3,4,'2026-11-30',5,'14:00','15:30',2,NULL,'DU_KIEN',NULL),
(6,2,4,6,'2026-11-23',22,'07:00','16:30',5,5,'HOAN_THANH','2026-11-23T00:00:00'),
(7,2,4,7,'2026-11-24',22,'07:00','16:30',5,5,'DA_KICH_HOAT','2026-11-24T00:00:00');
SET IDENTITY_INSERT dbo.BuoiHoc OFF;
SET IDENTITY_INSERT dbo.DiemDanh ON;
INSERT dbo.DiemDanh(MaDiemDanh,MaCoSo,MaHocVien,MaBuoiHoc,MaDangKy,LoaiThamGia,TrangThai,TinhBuoi,MaDiemDanhNguon,MaNguoiCapNhat,GhiChu) VALUES
(1,1,1,1,NULL,'HOC_THU','CO_MAT',0,NULL,2,N'Học thử thành công.'),
(2,1,1,2,1,'CHINH_THUC','CO_MAT',1,NULL,2,NULL),
(3,1,1,3,5,'CHINH_THUC','VANG_CO_PHEP',0,NULL,2,N'Ví dụ nghỉ không trừ buổi, quy tắc chưa chốt.'),
(4,1,2,3,NULL,'HOC_THU','VANG_KHONG_PHEP',0,NULL,2,N'Không đến.'),
(5,1,1,4,5,'HOC_BU','CO_MAT',1,3,2,N'Học bù cho buổi 16/11.'),
(6,1,2,5,NULL,'HOC_THU','CHUA_DIEM_DANH',0,NULL,1,N'Lịch thử lần hai.'),
(7,2,4,6,3,'CHINH_THUC','CO_MAT',0,NULL,5,NULL),
(8,2,5,6,4,'CHINH_THUC','VANG_CO_PHEP',0,NULL,5,N'Phụ huynh báo nghỉ.'),
(9,2,4,7,3,'CHINH_THUC','CO_MAT',0,NULL,5,NULL),
(10,2,5,7,4,'CHINH_THUC','CHUA_DIEM_DANH',0,NULL,5,NULL);
SET IDENTITY_INSERT dbo.DiemDanh OFF;
SET IDENTITY_INSERT dbo.GiaoDichTaiChinh ON;
INSERT dbo.GiaoDichTaiChinh(MaGiaoDich,MaCoSo,MaHocVien,MaDangKy,MaNhomChungTu,LoaiGiaoDich,MaLoaiPhi,KyPhi,SoLuong,DonGia,SoTien,NgayGiaoDich,MaNhanSu,DienGiai) VALUES
(1,1,1,1,'PTT-001','PHAI_THU',9,NULL,NULL,NULL,3300000,'2026-11-05',1,N'Học phí Level 1 sau giảm.'),
(2,1,3,2,'PTT-002','PHAI_THU',9,NULL,NULL,NULL,4000000,'2026-11-21',1,N'Học phí đã thống nhất, chờ xếp lớp.'),
(3,2,4,3,'PTT-001','PHAI_THU',26,'2026-11-01',1,2000000,2000000,'2026-11-01',4,N'Học phí tháng 11 (mẫu).'),
(4,2,4,3,'PTT-002','PHAI_THU',27,'2026-11-01',20,35000,700000,'2026-11-01',4,N'Tiền ăn tạm thu 20 ngày (mẫu).'),
(5,2,5,4,'PTT-003','PHAI_THU',26,'2026-11-01',1,1800000,1800000,'2026-11-09',4,N'Học phí riêng tháng 11 (mẫu).');
INSERT dbo.GiaoDichTaiChinh(MaGiaoDich,MaCoSo,MaHocVien,MaDangKy,MaGiaoDichNguon,MaNhomChungTu,LoaiGiaoDich,MaLoaiPhi,MaHinhThuc,SoTien,NgayGiaoDich,MaNhanSu,DienGiai) VALUES
(6,1,1,1,1,'PT-001','THU',9,8,2000000,'2026-11-05',1,N'Thu đợt 1.'),
(7,1,1,1,1,'PT-002','THU',9,7,1000000,'2026-11-09',1,N'Thu đợt 2.'),
(8,1,3,2,2,'PT-003','THU',9,8,500000,'2026-11-21',1,N'Đặt cọc.'),
(9,2,4,3,3,'PT-001','THU',26,25,2000000,'2026-11-02',4,N'Thu học phí.'),
(10,2,4,3,4,'PT-001','THU',27,25,700000,'2026-11-02',4,N'Cùng phiếu thu, phân bổ tiền ăn.'),
(11,2,5,4,5,'PT-002','THU',26,24,1000000,'2026-11-09',4,N'Thu một phần.'),
(12,1,NULL,NULL,NULL,'PC-001','CHI',10,7,250000,'2026-11-12',3,N'Mua văn phòng phẩm.'),
(13,2,NULL,NULL,NULL,'PC-001','CHI',28,24,350000,'2026-11-23',6,N'Mua thực phẩm.'),
(14,2,4,3,4,'DC-001','DIEU_CHINH',27,NULL,-35000,'2026-11-23',4,N'Điều chỉnh phí minh họa, không phải quy tắc tự động.');
INSERT dbo.GiaoDichTaiChinh(MaGiaoDich,MaCoSo,MaHocVien,MaDangKy,MaGiaoDichNguon,MaNhomChungTu,LoaiGiaoDich,MaLoaiPhi,MaHinhThuc,SoTien,NgayGiaoDich,MaNhanSu,DienGiai) VALUES
(15,2,4,3,10,'PH-001','HOAN',27,24,35000,'2026-11-24',4,N'Hoàn tiền ăn đã điều chỉnh (mẫu).');
SET IDENTITY_INSERT dbo.GiaoDichTaiChinh OFF;
INSERT dbo.GiaoDichKho(MaCoSo,MaNhomChungTu,MaHangHoa,MaHocVien,LoaiGiaoDich,SoLuong,TonThucTe,DonGia,MaNhanSu,NgayGiaoDich,GhiChu) VALUES
(1,'NK-001',11,NULL,'NHAP',20,NULL,80000,3,'2026-11-01',N'Nhập bàn tính.'),
(1,'NK-001',12,NULL,'NHAP',30,NULL,40000,3,'2026-11-01',N'Nhập giáo trình.'),
(1,'XK-001',11,1,'XUAT',-1,NULL,80000,3,'2026-11-09',N'Xuất cho học viên.'),
(1,'KK-001',11,NULL,'KIEM_KE',-1,18,80000,3,'2026-11-23',N'Tồn sổ 19, thực tế 18.'),
(2,'NK-001',29,NULL,'NHAP',25,NULL,25000,6,'2026-11-01',NULL),
(2,'XK-001',29,4,'XUAT',-2,NULL,25000,6,'2026-11-23',NULL);

-- ====================== VIEW TRA CUU (KHONG TANG SO BANG) ======================
EXEC(N'CREATE VIEW dbo.vw_HocVien AS
SELECT h.*, c.MaCoSoHienThi,c.TenCoSo,c.LoaiCoSo,n.HoTen AS TenNguoiTuVan,
 (SELECT COUNT(*) FROM dbo.DangKyHoc d WHERE d.MaHocVien=h.MaHocVien AND d.TrangThai=''DANG_HOC'') AS SoLopDangHoc,
 (SELECT COUNT(*) FROM dbo.DiemDanh dd JOIN dbo.BuoiHoc b ON b.MaBuoiHoc=dd.MaBuoiHoc WHERE dd.MaHocVien=h.MaHocVien AND dd.LoaiThamGia=''HOC_THU'' AND dd.TrangThai<>''HUY'' AND b.TrangThai<>''HUY'') AS SoLanXepHocThu
FROM dbo.HocVien h JOIN dbo.CoSo c ON c.MaCoSo=h.MaCoSo LEFT JOIN dbo.NhanSu n ON n.MaNhanSu=h.MaNguoiTuVan');
EXEC(N'CREATE VIEW dbo.vw_TienTrinhHoc AS
SELECT d.MaDangKy,d.MaCoSo,d.MaHocVien,d.MaKhoaHoc,k.TenDanhMuc AS TenKhoaHoc,d.NgayDangKy,d.NgayBatDau,d.NgayKetThucDuKien,d.TongSoBuoi,d.HocPhiThoaThuan,d.SoTienGiam,
 x.SoBuoiDaTinh, CASE WHEN d.TongSoBuoi IS NULL THEN NULL ELSE d.TongSoBuoi-x.SoBuoiDaTinh END AS SoBuoiConLai
FROM dbo.DangKyHoc d LEFT JOIN dbo.DanhMuc k ON k.MaDanhMuc=d.MaKhoaHoc
CROSS APPLY (SELECT COUNT(*) AS SoBuoiDaTinh FROM dbo.DiemDanh dd JOIN dbo.DangKyHoc dk ON dk.MaDangKy=dd.MaDangKy JOIN dbo.BuoiHoc b ON b.MaBuoiHoc=dd.MaBuoiHoc WHERE ISNULL(dk.MaDangKyGoc,dk.MaDangKy)=d.MaDangKy AND dd.TinhBuoi=1 AND b.TrangThai=''HOAN_THANH'') x
WHERE d.MaDangKyGoc IS NULL');
EXEC(N'CREATE VIEW dbo.vw_CongNo AS
SELECT g.MaGiaoDich AS MaKhoanPhaiThu,g.MaCoSo,g.MaHocVien,g.MaDangKy,g.MaLoaiPhi,g.KyPhi,g.SoTien AS TienGoc,
 ISNULL(a.DieuChinh,0) AS DieuChinh,ISNULL(a.DaThu,0) AS DaThu,ISNULL(r.DaHoan,0) AS DaHoan,
 g.SoTien+ISNULL(a.DieuChinh,0)-ISNULL(a.DaThu,0)+ISNULL(r.DaHoan,0) AS CongNo
FROM dbo.GiaoDichTaiChinh g
OUTER APPLY (SELECT SUM(CASE WHEN LoaiGiaoDich=''DIEU_CHINH'' THEN SoTien ELSE 0 END) DieuChinh,SUM(CASE WHEN LoaiGiaoDich=''THU'' THEN SoTien ELSE 0 END) DaThu FROM dbo.GiaoDichTaiChinh WHERE MaGiaoDichNguon=g.MaGiaoDich AND TrangThai=''XAC_NHAN'') a
OUTER APPLY (SELECT SUM(h.SoTien) DaHoan FROM dbo.GiaoDichTaiChinh t JOIN dbo.GiaoDichTaiChinh h ON h.MaGiaoDichNguon=t.MaGiaoDich AND h.LoaiGiaoDich=''HOAN'' AND h.TrangThai=''XAC_NHAN'' WHERE t.MaGiaoDichNguon=g.MaGiaoDich AND t.LoaiGiaoDich=''THU'' AND t.TrangThai=''XAC_NHAN'') r
WHERE g.LoaiGiaoDich=''PHAI_THU'' AND g.TrangThai=''XAC_NHAN''');
EXEC(N'CREATE VIEW dbo.vw_ThuChi AS
SELECT g.MaGiaoDich,g.MaCoSo,g.MaHocVien,g.MaNhomChungTu,g.NgayGiaoDich,g.LoaiGiaoDich,m.TenDanhMuc AS LoaiPhi,g.DienGiai,
 CASE WHEN g.LoaiGiaoDich=''THU'' THEN g.SoTien ELSE 0 END AS TienThu,
 CASE WHEN g.LoaiGiaoDich IN (''CHI'',''HOAN'') THEN g.SoTien ELSE 0 END AS TienChi
FROM dbo.GiaoDichTaiChinh g JOIN dbo.DanhMuc m ON m.MaDanhMuc=g.MaLoaiPhi
WHERE g.TrangThai=''XAC_NHAN'' AND g.LoaiGiaoDich IN (''THU'',''CHI'',''HOAN'')');
EXEC(N'CREATE VIEW dbo.vw_TonKho AS
SELECT d.MaCoSo,d.MaDanhMuc AS MaHangHoa,d.MaKyHieu,d.TenDanhMuc AS TenHangHoa,JSON_VALUE(d.CauHinh,''$.DonViTinh'') AS DonViTinh,
 ISNULL(SUM(g.SoLuong),0) AS TonKho
FROM dbo.DanhMuc d LEFT JOIN dbo.GiaoDichKho g ON g.MaHangHoa=d.MaDanhMuc AND g.MaCoSo=d.MaCoSo AND g.TrangThai=''XAC_NHAN''
WHERE d.NhomDanhMuc=''HANG_HOA'' GROUP BY d.MaCoSo,d.MaDanhMuc,d.MaKyHieu,d.TenDanhMuc,JSON_VALUE(d.CauHinh,''$.DonViTinh'')');

-- Kiem tra du lieu mau ngay trong transaction; loi thi rollback.
EXEC(N'IF (SELECT COUNT(*) FROM sys.tables WHERE is_ms_shipped=0)<>12
    THROW 50010, N''Số bảng không đúng dự kiến.'',1;
IF NOT EXISTS (SELECT 1 FROM dbo.vw_TienTrinhHoc WHERE MaDangKy=1 AND SoBuoiDaTinh=2 AND SoBuoiConLai=22)
    THROW 50011, N''Kiểm tra tiến trình học không đạt.'',1;
IF NOT EXISTS (SELECT 1 FROM dbo.vw_CongNo WHERE MaKhoanPhaiThu=1 AND CongNo=300000)
    THROW 50012, N''Kiểm tra công nợ trung tâm không đạt.'',1;
IF NOT EXISTS (SELECT 1 FROM dbo.vw_CongNo WHERE MaKhoanPhaiThu=4 AND CongNo=0)
    THROW 50013, N''Kiểm tra hoàn tiền mầm non không đạt.'',1;
IF NOT EXISTS (SELECT 1 FROM dbo.vw_TonKho WHERE MaHangHoa=11 AND TonKho=18)
    THROW 50014, N''Kiểm tra tồn kho không đạt.'',1;');
COMMIT TRANSACTION;
END TRY
BEGIN CATCH
 IF XACT_STATE()<>0 ROLLBACK TRANSACTION;
 THROW;
END CATCH;

-- ====================== KIEM TRA SAU CAI DAT ======================
USE VINABACUS_V2_TEST;
GO

-- KIEM TRA SO LUONG BANG
SELECT COUNT(*) AS SoBang
FROM sys.tables
WHERE is_ms_shipped = 0;

-- ==========================================
-- 12 BANG DU LIEU
-- ==========================================

SELECT N'01. CoSo' AS TenBang;
SELECT * FROM dbo.CoSo;

SELECT N'02. NhanSu' AS TenBang;
SELECT * FROM dbo.NhanSu;

SELECT N'03. DanhMuc' AS TenBang;
SELECT * FROM dbo.DanhMuc;

SELECT N'04. HocVien' AS TenBang;
SELECT * FROM dbo.HocVien;

SELECT N'05. TuongTacHocVien' AS TenBang;
SELECT * FROM dbo.TuongTacHocVien;

SELECT N'06. LopHoc' AS TenBang;
SELECT * FROM dbo.LopHoc;

SELECT N'07. DangKyHoc' AS TenBang;
SELECT * FROM dbo.DangKyHoc;

SELECT N'08. ThoiKhoaBieu' AS TenBang;
SELECT * FROM dbo.ThoiKhoaBieu;

SELECT N'09. BuoiHoc' AS TenBang;
SELECT * FROM dbo.BuoiHoc;

SELECT N'10. DiemDanh' AS TenBang;
SELECT * FROM dbo.DiemDanh;

SELECT N'11. GiaoDichTaiChinh' AS TenBang;
SELECT * FROM dbo.GiaoDichTaiChinh;

SELECT N'12. GiaoDichKho' AS TenBang;
SELECT * FROM dbo.GiaoDichKho;

-- ==========================================
-- 5 VIEW KIEM TRA NGHIEP VU
-- ==========================================

SELECT N'13. vw_HocVien - Tổng quan học viên' AS TenView;
SELECT * FROM dbo.vw_HocVien
ORDER BY MaCoSo, MaHocVien;

SELECT N'14. vw_TienTrinhHoc - Tiến trình học' AS TenView;
SELECT * FROM dbo.vw_TienTrinhHoc
ORDER BY MaDangKy;

SELECT N'15. vw_CongNo - Công nợ' AS TenView;
SELECT * FROM dbo.vw_CongNo
ORDER BY MaKhoanPhaiThu;

SELECT N'16. vw_ThuChi - Thu chi' AS TenView;
SELECT * FROM dbo.vw_ThuChi;

SELECT N'17. vw_TonKho - Tồn kho' AS TenView;
SELECT * FROM dbo.vw_TonKho
ORDER BY MaCoSo, MaHangHoa;