-- Chỉ chạy theo yêu cầu riêng; dừng nếu đã nhập dữ liệu cá nhân.
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRAN;
    DECLARE @n INT;
    IF COL_LENGTH('dbo.KhachTiemNang','GioiTinh') IS NOT NULL
       AND EXISTS(SELECT 1 FROM sys.extended_properties ep JOIN sys.columns c ON c.object_id=ep.major_id AND c.column_id=ep.minor_id
                  WHERE ep.major_id=OBJECT_ID('dbo.KhachTiemNang') AND ep.name=N'VINABACUS_TS_CANHAN' AND c.name=N'GioiTinh')
    BEGIN
        EXEC sys.sp_executesql N'SELECT @x=COUNT(*) FROM dbo.KhachTiemNang WHERE GioiTinh IS NOT NULL',N'@x INT OUTPUT',@x=@n OUTPUT;
        IF @n>0 THROW 51111,N'Giới tính đã có dữ liệu; không xóa.',1;
        IF OBJECT_ID(N'dbo.CK_KhachTiemNang_GioiTinh_TS',N'C') IS NOT NULL
            ALTER TABLE dbo.KhachTiemNang DROP CONSTRAINT CK_KhachTiemNang_GioiTinh_TS;
        ALTER TABLE dbo.KhachTiemNang DROP COLUMN GioiTinh;
    END;
    IF COL_LENGTH('dbo.KhachTiemNang','NgaySinh') IS NOT NULL
       AND EXISTS(SELECT 1 FROM sys.extended_properties ep JOIN sys.columns c ON c.object_id=ep.major_id AND c.column_id=ep.minor_id
                  WHERE ep.major_id=OBJECT_ID('dbo.KhachTiemNang') AND ep.name=N'VINABACUS_TS_CANHAN' AND c.name=N'NgaySinh')
    BEGIN
        EXEC sys.sp_executesql N'SELECT @x=COUNT(*) FROM dbo.KhachTiemNang WHERE NgaySinh IS NOT NULL',N'@x INT OUTPUT',@x=@n OUTPUT;
        IF @n>0 THROW 51112,N'Ngày sinh đã có dữ liệu; không xóa.',1;
        ALTER TABLE dbo.KhachTiemNang DROP COLUMN NgaySinh;
    END;
    IF COL_LENGTH('dbo.KhachTiemNang','DiaChi') IS NOT NULL
       AND EXISTS(SELECT 1 FROM sys.extended_properties ep JOIN sys.columns c ON c.object_id=ep.major_id AND c.column_id=ep.minor_id
                  WHERE ep.major_id=OBJECT_ID('dbo.KhachTiemNang') AND ep.name=N'VINABACUS_TS_CANHAN' AND c.name=N'DiaChi')
    BEGIN
        EXEC sys.sp_executesql N'SELECT @x=COUNT(*) FROM dbo.KhachTiemNang WHERE DiaChi IS NOT NULL',N'@x INT OUTPUT',@x=@n OUTPUT;
        IF @n>0 THROW 51113,N'Địa chỉ đã có dữ liệu; không xóa.',1;
        ALTER TABLE dbo.KhachTiemNang DROP COLUMN DiaChi;
    END;
    COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT>0 ROLLBACK;
    THROW;
END CATCH;
