/* CHI DU THAO. Chi duoc rollback neu chua co du lieu GD6 phu thuoc. */
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
IF DB_NAME() NOT IN (N'VINABACUS',N'VINABACUS_GD6_TEST_20261123')
    THROW 58611, N'Sai CSDL VINABACUS hoac ban sao TEST GD6 da xac minh.', 1;
IF COL_LENGTH(N'dbo.BuoiHoc', N'NoiDungHoc') IS NULL
   OR OBJECT_ID(N'dbo.LichSuSuaDiemDanhTrungTam', N'U') IS NULL
   OR COL_LENGTH(N'dbo.SapXepBuoiHoc', N'LyDoHuy') IS NULL
   OR COL_LENGTH(N'dbo.DangKyKhoaHoc', N'HoanThanhTuDongGD6') IS NULL
    THROW 58612, N'Schema GD6 khong day du; dung de doi chieu.', 1;
IF EXISTS (SELECT 1 FROM dbo.LichSuSuaDiemDanhTrungTam)
   OR EXISTS (SELECT 1 FROM dbo.DangKyKhoaHoc WHERE HoanThanhTuDongGD6=1)
   OR EXISTS (SELECT 1 FROM dbo.SapXepBuoiHoc WHERE LoaiSapXep='HOC_BU')
   OR EXISTS (SELECT 1 FROM dbo.BuoiHoc WHERE NoiDungHoc IS NOT NULL)
   OR EXISTS (SELECT 1 FROM dbo.SapXepBuoiHoc
              WHERE LyDoHuy IS NOT NULL OR NgayHuy IS NOT NULL OR MaTaiKhoanHuy IS NOT NULL)
    THROW 58613, N'Co du lieu GD6; khong rollback de tranh mat lich su.', 1;
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    DROP TABLE dbo.LichSuSuaDiemDanhTrungTam;
    ALTER TABLE dbo.DangKyKhoaHoc
        DROP CONSTRAINT DF_DangKyKhoaHoc_HoanThanhTuDongGD6;
    ALTER TABLE dbo.DangKyKhoaHoc DROP COLUMN HoanThanhTuDongGD6;
    DROP INDEX UX_SapXepBuoiHoc_HocBuDangHoatDong_GD6 ON dbo.SapXepBuoiHoc;
    ALTER TABLE dbo.SapXepBuoiHoc DROP CONSTRAINT FK_SapXepBuoiHoc_TaiKhoanHuy_GD6;
    ALTER TABLE dbo.SapXepBuoiHoc DROP COLUMN LyDoHuy,NgayHuy,MaTaiKhoanHuy;
    ALTER TABLE dbo.BuoiHoc DROP COLUMN NoiDungHoc;
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT>0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
