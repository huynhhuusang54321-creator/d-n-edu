/* Giai doan 6: CHI DU THAO. Khong tu dong chay tren CSDL that.
 * Chay thu tren ban sao TEST rieng, doi chieu backup va schema truoc khi duyet.
 * Khong sua du lieu lich su, khong gan noi dung cho buoi cu.
 */
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
IF DB_NAME() NOT IN (N'VINABACUS',N'VINABACUS_GD6_TEST_20261123')
    THROW 58601, N'Sai CSDL VINABACUS hoac ban sao TEST GD6 da xac minh.', 1;
IF OBJECT_ID(N'dbo.BuoiHoc', N'U') IS NULL
   OR OBJECT_ID(N'dbo.DiemDanhTrungTam', N'U') IS NULL
   OR OBJECT_ID(N'dbo.SapXepBuoiHoc', N'U') IS NULL
   OR OBJECT_ID(N'dbo.DangKyKhoaHoc', N'U') IS NULL
   OR OBJECT_ID(N'dbo.TaiKhoan', N'U') IS NULL
    THROW 58602, N'Thieu bang nen Giai doan 6.', 1;
IF COL_LENGTH(N'dbo.BuoiHoc', N'NoiDungHoc') IS NOT NULL
   OR OBJECT_ID(N'dbo.LichSuSuaDiemDanhTrungTam', N'U') IS NOT NULL
   OR COL_LENGTH(N'dbo.SapXepBuoiHoc', N'LyDoHuy') IS NOT NULL
   OR COL_LENGTH(N'dbo.DangKyKhoaHoc', N'HoanThanhTuDongGD6') IS NOT NULL
    THROW 58603, N'Migration GD6 da chay mot phan hoac toan bo; can doi chieu thu cong.', 1;
IF EXISTS (
    SELECT 1 FROM dbo.SapXepBuoiHoc
    WHERE LoaiSapXep='HOC_BU' AND TrangThai IN ('DA_XEP','HOAN_THANH')
    GROUP BY MaDangKy,MaSapXepNguon HAVING COUNT(*)>1
)
    THROW 58604, N'Co nhieu luot hoc bu hoat dong tren mot buoi vang.', 1;

SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    ALTER TABLE dbo.BuoiHoc ADD NoiDungHoc NVARCHAR(500) NULL;
    ALTER TABLE dbo.DangKyKhoaHoc ADD HoanThanhTuDongGD6 BIT NOT NULL
        CONSTRAINT DF_DangKyKhoaHoc_HoanThanhTuDongGD6 DEFAULT (0);
    ALTER TABLE dbo.SapXepBuoiHoc ADD
        LyDoHuy NVARCHAR(500) NULL,
        NgayHuy DATETIME2(0) NULL,
        MaTaiKhoanHuy INT NULL;
    ALTER TABLE dbo.SapXepBuoiHoc WITH CHECK ADD
        CONSTRAINT FK_SapXepBuoiHoc_TaiKhoanHuy_GD6
        FOREIGN KEY (MaTaiKhoanHuy) REFERENCES dbo.TaiKhoan(MaTaiKhoan);
    CREATE UNIQUE INDEX UX_SapXepBuoiHoc_HocBuDangHoatDong_GD6
        ON dbo.SapXepBuoiHoc(MaDangKy,MaSapXepNguon)
        WHERE LoaiSapXep='HOC_BU' AND TrangThai IN ('DA_XEP','HOAN_THANH');
    CREATE TABLE dbo.LichSuSuaDiemDanhTrungTam (
        MaLichSu BIGINT IDENTITY(1,1) NOT NULL
            CONSTRAINT PK_LichSuSuaDiemDanhTrungTam_GD6 PRIMARY KEY,
        MaDiemDanh INT NOT NULL,
        TrangThaiTruoc VARCHAR(25) NOT NULL,
        TrangThaiSau VARCHAR(25) NOT NULL,
        GhiChuTruoc NVARCHAR(500) NULL,
        GhiChuSau NVARCHAR(500) NULL,
        LyDo NVARCHAR(500) NOT NULL,
        MaTaiKhoan INT NOT NULL,
        NgaySua DATETIME2(0) NOT NULL
            CONSTRAINT DF_LichSuSuaDiemDanhTrungTam_NgaySua_GD6
            DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_LichSuSuaDiemDanhTrungTam_DiemDanh_GD6
            FOREIGN KEY (MaDiemDanh) REFERENCES dbo.DiemDanhTrungTam(MaDiemDanh),
        CONSTRAINT FK_LichSuSuaDiemDanhTrungTam_TaiKhoan_GD6
            FOREIGN KEY (MaTaiKhoan) REFERENCES dbo.TaiKhoan(MaTaiKhoan),
        CONSTRAINT CK_LichSuSuaDiemDanhTrungTam_LyDo_GD6
            CHECK (LEN(LTRIM(RTRIM(LyDo)))>0)
    );
    CREATE INDEX IX_LichSuSuaDiemDanhTrungTam_DiemDanh_GD6
        ON dbo.LichSuSuaDiemDanhTrungTam(MaDiemDanh,NgaySua DESC);
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT>0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
