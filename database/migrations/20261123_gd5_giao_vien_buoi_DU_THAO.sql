/* GIAI DOAN 5 - CHI LA BAN DU THAO, KHONG TU DONG THUC THI.
 * Phai duyet va chay migration lich su GD4 truoc. Cac buoi cu giu NULL:
 * khong suy dien giao vien qua khu tu LopTrungTam.MaGiaoVien hien tai.
 */
IF DB_NAME() <> N'VINABACUS'
    THROW 58101, N'Script chi duoc phep chay tren VINABACUS.', 1;
IF OBJECT_ID(N'dbo.BuoiHoc', N'U') IS NULL
   OR OBJECT_ID(N'dbo.NhanVien', N'U') IS NULL
   OR OBJECT_ID(N'dbo.PhanCongGiaoVienTrungTam', N'U') IS NULL
    THROW 58102, N'Thieu bang nen hoac migration lich su GD4.', 1;
IF COL_LENGTH(N'dbo.BuoiHoc', N'MaGiaoVienPhuTrach') IS NOT NULL
    THROW 58103, N'Cot giao vien buoi hoc da ton tai; doi chieu truoc khi chay.', 1;

SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    ALTER TABLE dbo.BuoiHoc ADD MaGiaoVienPhuTrach INT NULL;
    ALTER TABLE dbo.BuoiHoc WITH CHECK ADD CONSTRAINT FK_BuoiHoc_GiaoVienPhuTrach_GD5
        FOREIGN KEY (MaGiaoVienPhuTrach) REFERENCES dbo.NhanVien(MaNhanVien);
    CREATE INDEX IX_BuoiHoc_GiaoVienNgay_GD5
        ON dbo.BuoiHoc(MaGiaoVienPhuTrach, NgayHoc, SoCa)
        INCLUDE (TrangThai, MaLop)
        WHERE MaGiaoVienPhuTrach IS NOT NULL;
    IF EXISTS (SELECT 1 FROM dbo.BuoiHoc WHERE MaGiaoVienPhuTrach IS NOT NULL)
        THROW 58104, N'Buoi hoc cu bi gan giao vien ngoai du kien.', 1;
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
