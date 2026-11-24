/* ĐỀ XUẤT GĐ7 – CHƯA ĐƯỢC PHÉP CHẠY trên CSDL chính.
   Bổ sung tên người nộp có cấu trúc; phiếu cũ để NULL, không suy đoán dữ liệu. */
SET XACT_ABORT ON;
IF DB_NAME() <> N'VINABACUS'
    THROW 50701, N'Script này chỉ dành cho VINABACUS sau khi được phê duyệt.', 1;
BEGIN TRANSACTION;
IF OBJECT_ID(N'dbo.PhieuThu',N'U') IS NULL
    THROW 50702, N'Thiếu bảng dbo.PhieuThu.', 1;
IF COL_LENGTH(N'dbo.PhieuThu',N'TenNguoiNop') IS NULL
    ALTER TABLE dbo.PhieuThu ADD TenNguoiNop NVARCHAR(180) NULL;
COMMIT TRANSACTION;
