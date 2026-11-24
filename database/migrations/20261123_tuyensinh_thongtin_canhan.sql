-- Chỉ chuẩn bị để duyệt; không chạy trên VINABACUS khi chưa có phê duyệt riêng.
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRAN;
    IF OBJECT_ID(N'dbo.KhachTiemNang',N'U') IS NULL
        THROW 51110,N'Thiếu bảng KhachTiemNang.',1;
    IF COL_LENGTH('dbo.KhachTiemNang','NgaySinh') IS NULL
    BEGIN
        ALTER TABLE dbo.KhachTiemNang ADD NgaySinh DATE NULL;
        EXEC sys.sp_addextendedproperty @name=N'VINABACUS_TS_CANHAN',@value=N'20261123',
            @level0type=N'SCHEMA',@level0name=N'dbo',@level1type=N'TABLE',@level1name=N'KhachTiemNang',
            @level2type=N'COLUMN',@level2name=N'NgaySinh';
    END;
    IF COL_LENGTH('dbo.KhachTiemNang','GioiTinh') IS NULL
    BEGIN
        ALTER TABLE dbo.KhachTiemNang ADD GioiTinh VARCHAR(15) NULL;
        EXEC sys.sp_addextendedproperty @name=N'VINABACUS_TS_CANHAN',@value=N'20261123',
            @level0type=N'SCHEMA',@level0name=N'dbo',@level1type=N'TABLE',@level1name=N'KhachTiemNang',
            @level2type=N'COLUMN',@level2name=N'GioiTinh';
    END;
    IF COL_LENGTH('dbo.KhachTiemNang','DiaChi') IS NULL
    BEGIN
        ALTER TABLE dbo.KhachTiemNang ADD DiaChi NVARCHAR(400) NULL;
        EXEC sys.sp_addextendedproperty @name=N'VINABACUS_TS_CANHAN',@value=N'20261123',
            @level0type=N'SCHEMA',@level0name=N'dbo',@level1type=N'TABLE',@level1name=N'KhachTiemNang',
            @level2type=N'COLUMN',@level2name=N'DiaChi';
    END;
    IF OBJECT_ID(N'dbo.CK_KhachTiemNang_GioiTinh_TS',N'C') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.KhachTiemNang WITH CHECK ADD CONSTRAINT CK_KhachTiemNang_GioiTinh_TS CHECK (GioiTinh IS NULL OR GioiTinh IN (''NAM'',''NU'',''KHAC''))';
    COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT>0 ROLLBACK;
    THROW;
END CATCH;
