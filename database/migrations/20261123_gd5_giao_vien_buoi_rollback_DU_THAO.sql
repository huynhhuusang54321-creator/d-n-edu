/* CHI DE DUYET. KHONG CHAY TU DONG.
 * Chi duoc go bo cot neu khong co anh chup giao vien buoi hoc nao.
 */
IF DB_NAME() <> N'VINABACUS'
    THROW 58111, N'Sai database.', 1;
IF COL_LENGTH(N'dbo.BuoiHoc', N'MaGiaoVienPhuTrach') IS NULL
    THROW 58112, N'Cot can rollback khong ton tai.', 1;

SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    IF EXISTS (SELECT 1 FROM dbo.BuoiHoc WITH (UPDLOCK, HOLDLOCK)
               WHERE MaGiaoVienPhuTrach IS NOT NULL)
        THROW 58113, N'Da co lich su giao vien theo buoi; khong duoc rollback.', 1;
    IF EXISTS (
        SELECT 1 FROM sys.index_columns ic
        JOIN sys.indexes i ON i.object_id=ic.object_id AND i.index_id=ic.index_id
        WHERE ic.object_id=OBJECT_ID(N'dbo.BuoiHoc')
          AND ic.column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.BuoiHoc'),
                                           N'MaGiaoVienPhuTrach', 'ColumnId')
          AND i.name<>N'IX_BuoiHoc_GiaoVienNgay_GD5'
    )
        THROW 58114, N'Cot da co index phu thuoc khac; doi chieu truoc rollback.', 1;
    DROP INDEX IX_BuoiHoc_GiaoVienNgay_GD5 ON dbo.BuoiHoc;
    ALTER TABLE dbo.BuoiHoc DROP CONSTRAINT FK_BuoiHoc_GiaoVienPhuTrach_GD5;
    ALTER TABLE dbo.BuoiHoc DROP COLUMN MaGiaoVienPhuTrach;
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
