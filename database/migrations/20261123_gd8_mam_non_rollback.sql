-- Chỉ chạy theo yêu cầu riêng. Dừng nếu dữ liệu mới đã được sử dụng.
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRAN;
    DECLARE @soDong INT;
    IF OBJECT_ID(N'dbo.NgayHoatDongMamNon',N'U') IS NOT NULL
       AND EXISTS(SELECT 1 FROM sys.extended_properties ep WHERE ep.major_id=OBJECT_ID(N'dbo.NgayHoatDongMamNon')
           AND ep.minor_id=0 AND ep.name=N'VINABACUS_GD8')
    BEGIN
        EXEC sys.sp_executesql N'SELECT @n=COUNT(*) FROM dbo.NgayHoatDongMamNon',N'@n INT OUTPUT',@n=@soDong OUTPUT;
        IF @soDong>0
            THROW 51084, N'Lịch hoạt động đã có dữ liệu; không tự xóa.', 1;
        DROP TABLE dbo.NgayHoatDongMamNon;
    END;
    IF OBJECT_ID(N'dbo.ChinhSachTienAnMamNon',N'U') IS NOT NULL
       AND EXISTS(SELECT 1 FROM sys.extended_properties ep WHERE ep.major_id=OBJECT_ID(N'dbo.ChinhSachTienAnMamNon')
           AND ep.minor_id=0 AND ep.name=N'VINABACUS_GD8')
    BEGIN
        EXEC sys.sp_executesql N'SELECT @n=COUNT(*) FROM dbo.ChinhSachTienAnMamNon',N'@n INT OUTPUT',@n=@soDong OUTPUT;
        IF @soDong>0
            THROW 51081, N'Bảng chính sách đã có dữ liệu; không tự xóa.', 1;
        DROP TABLE dbo.ChinhSachTienAnMamNon;
    END;
    IF EXISTS(SELECT 1 FROM sys.extended_properties ep JOIN sys.columns c ON c.object_id=ep.major_id AND c.column_id=ep.minor_id
              WHERE ep.major_id=OBJECT_ID(N'dbo.LopMamNon') AND ep.name=N'VINABACUS_GD8' AND c.name=N'Khoi')
    BEGIN
        EXEC sys.sp_executesql N'SELECT @n=COUNT(*) FROM dbo.LopMamNon WHERE Khoi IS NOT NULL',N'@n INT OUTPUT',@n=@soDong OUTPUT;
        IF @soDong>0
            THROW 51082, N'Khối lớp đã được thiết lập; không tự xóa.', 1;
        IF OBJECT_ID(N'dbo.CK_LopMamNon_Khoi_GD8',N'C') IS NOT NULL
            ALTER TABLE dbo.LopMamNon DROP CONSTRAINT CK_LopMamNon_Khoi_GD8;
        ALTER TABLE dbo.LopMamNon DROP COLUMN Khoi;
    END;
    IF EXISTS(SELECT 1 FROM sys.extended_properties ep JOIN sys.columns c ON c.object_id=ep.major_id AND c.column_id=ep.minor_id
              WHERE ep.major_id=OBJECT_ID(N'dbo.LopMamNon') AND ep.name=N'VINABACUS_GD8' AND c.name=N'SiSoToiDa')
    BEGIN
        EXEC sys.sp_executesql N'SELECT @n=COUNT(*) FROM dbo.LopMamNon WHERE SiSoToiDa IS NOT NULL',N'@n INT OUTPUT',@n=@soDong OUTPUT;
        IF @soDong>0
            THROW 51083, N'Sĩ số tối đa đã được thiết lập; không tự xóa.', 1;
        IF OBJECT_ID(N'dbo.CK_LopMamNon_SiSoToiDa_GD8',N'C') IS NOT NULL
            ALTER TABLE dbo.LopMamNon DROP CONSTRAINT CK_LopMamNon_SiSoToiDa_GD8;
        ALTER TABLE dbo.LopMamNon DROP COLUMN SiSoToiDa;
    END;
    COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT>0 ROLLBACK;
    THROW;
END CATCH;
