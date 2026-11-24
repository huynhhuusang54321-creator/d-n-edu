/* Chỉ dùng khi được yêu cầu; dừng nếu cột đã có dữ liệu để không làm mất lịch sử. */
SET XACT_ABORT ON;
IF DB_NAME() <> N'VINABACUS'
    THROW 50703, N'Rollback này chỉ dành cho VINABACUS sau khi được phê duyệt.', 1;
BEGIN TRANSACTION;
IF COL_LENGTH(N'dbo.PhieuThu',N'TenNguoiNop') IS NOT NULL
BEGIN
    IF EXISTS(SELECT 1 FROM dbo.PhieuThu WHERE TenNguoiNop IS NOT NULL)
        THROW 50704, N'Cột đã có tên người nộp; không được xóa dữ liệu.', 1;
    ALTER TABLE dbo.PhieuThu DROP COLUMN TenNguoiNop;
END;
COMMIT TRANSACTION;
