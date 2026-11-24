/*
 * GD4 - rollback CO DIEU KIEN, CHUA DUOC THUC THI.
 * Chi dung ngay sau migration, TRUOC KHI khai bao chuyen mon, tao lop co
 * giao vien hoac chuyen phan cong. Neu da co lich su moi, script dung de
 * khong lam mat du lieu; can phuong an khoi phuc rieng.
 */
SET XACT_ABORT ON;
IF DB_NAME() <> N'VINABACUS'
    THROW 58070, N'Rollback chi danh cho VINABACUS.', 1;

BEGIN TRY
    BEGIN TRANSACTION;
    IF OBJECT_ID(N'dbo.GiaoVienMonHoc', N'U') IS NULL
       OR OBJECT_ID(N'dbo.PhanCongGiaoVienTrungTam', N'U') IS NULL
        THROW 58071, N'Thieu bang GD4; dung rollback.', 1;

    IF EXISTS (SELECT 1 FROM dbo.GiaoVienMonHoc WITH (HOLDLOCK))
        THROW 58072, N'Da co lich su chuyen mon moi; khong duoc xoa.', 1;

    IF EXISTS (
        SELECT 1 FROM dbo.PhanCongGiaoVienTrungTam pc WITH (HOLDLOCK)
        LEFT JOIN dbo.LopTrungTam l WITH (HOLDLOCK)
          ON l.MaLop=pc.MaLop AND l.MaCoSo=pc.MaCoSo
        WHERE pc.NguonDuLieu<>'HIEN_TRANG' OR pc.TuNgay IS NOT NULL
           OR pc.DenNgay IS NOT NULL OR pc.MaNhanVienTruoc IS NOT NULL
           OR pc.MaTaiKhoanThucHien IS NOT NULL
           OR l.MaLop IS NULL OR l.MaGiaoVien IS NULL
           OR l.MaGiaoVien<>pc.MaNhanVien
           OR l.MaKhoaHoc<>pc.MaKhoaHoc
    )
        THROW 58073, N'Phan cong da thay doi; khong duoc xoa lich su.', 1;

    IF (SELECT COUNT(*) FROM dbo.PhanCongGiaoVienTrungTam WITH (HOLDLOCK))
       <> (SELECT COUNT(*) FROM dbo.LopTrungTam WITH (HOLDLOCK)
           WHERE MaGiaoVien IS NOT NULL)
        THROW 58074, N'So anh chup khong khop lop hien tai; dung rollback.', 1;

    DROP TABLE dbo.PhanCongGiaoVienTrungTam;
    DROP TABLE dbo.GiaoVienMonHoc;
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT>0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
