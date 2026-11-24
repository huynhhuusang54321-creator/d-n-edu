-- Phuong an rollback migration Giai doan 3, CHUA CHAY.
-- Chi dung khi chua co du lieu Giai doan 3 phat sinh. Cac lien ket mon
-- duoc migration suy ra tu GiamGiaKhoaHoc co the duoc bo neu van trung
-- khop 100% voi pham vi khoa cu. Neu da thay doi pham vi/giao dich, script
-- dung va can khoi phuc tu ban sao luu sau khi doi chieu giao dich.
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;

    IF OBJECT_ID(N'dbo.ChiTietGiamGiaDangKy', N'U') IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM sys.key_constraints
                       WHERE parent_object_id=OBJECT_ID(N'dbo.ChiTietGiamGiaDangKy')
                         AND name=N'PK_ChiTietGiamGiaDangKy_GD3')
        THROW 51046, N'ChiTietGiamGiaDangKy khong do migration nay tao; khong rollback.', 1;
    IF OBJECT_ID(N'dbo.GiamGiaMonHoc', N'U') IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM sys.key_constraints
                       WHERE parent_object_id=OBJECT_ID(N'dbo.GiamGiaMonHoc')
                         AND name=N'PK_GiamGiaMonHoc_GD3')
        THROW 51047, N'GiamGiaMonHoc khong do migration nay tao; khong rollback.', 1;

    IF OBJECT_ID(N'dbo.ChiTietGiamGiaDangKy', N'U') IS NOT NULL
        EXEC sys.sp_executesql
            N'IF EXISTS (SELECT 1 FROM dbo.ChiTietGiamGiaDangKy)
                THROW 51041, N''ChiTietGiamGiaDangKy da co du lieu; khong rollback tu dong.'', 1;';
    IF OBJECT_ID(N'dbo.GiamGiaMonHoc', N'U') IS NOT NULL
        EXEC sys.sp_executesql
            N'IF EXISTS (
                  SELECT MaChuongTrinh,MaMonHoc,MaCoSo
                  FROM dbo.GiamGiaMonHoc
                  EXCEPT
                  SELECT g.MaChuongTrinh,k.MaMonHoc,g.MaCoSo
                  FROM dbo.GiamGiaKhoaHoc g
                  JOIN dbo.KhoaHoc k
                    ON k.MaKhoaHoc=g.MaKhoaHoc AND k.MaCoSo=g.MaCoSo
                  JOIN dbo.ChuongTrinhGiamGia ct
                    ON ct.MaChuongTrinh=g.MaChuongTrinh AND ct.MaCoSo=g.MaCoSo
                   AND ct.PhamViApDung=''KHOA_HOC''
                ) OR EXISTS (
                  SELECT g.MaChuongTrinh,k.MaMonHoc,g.MaCoSo
                  FROM dbo.GiamGiaKhoaHoc g
                  JOIN dbo.KhoaHoc k
                    ON k.MaKhoaHoc=g.MaKhoaHoc AND k.MaCoSo=g.MaCoSo
                  JOIN dbo.ChuongTrinhGiamGia ct
                    ON ct.MaChuongTrinh=g.MaChuongTrinh AND ct.MaCoSo=g.MaCoSo
                   AND ct.PhamViApDung=''KHOA_HOC''
                  EXCEPT
                  SELECT MaChuongTrinh,MaMonHoc,MaCoSo
                  FROM dbo.GiamGiaMonHoc
                )
                THROW 51042, N''Pham vi mon da thay doi; khong rollback tu dong.'', 1;';

    IF COL_LENGTH(N'dbo.ChuongTrinhGiamGia', N'ChoPhepKetHop') IS NOT NULL
        EXEC sys.sp_executesql
            N'IF EXISTS (SELECT 1 FROM dbo.ChuongTrinhGiamGia WHERE ChoPhepKetHop <> 0)
                THROW 51043, N''ChoPhepKetHop da duoc su dung; khong rollback tu dong.'', 1;';
    IF COL_LENGTH(N'dbo.ChuongTrinhGiamGia', N'ApDungTatCaMon') IS NOT NULL
        EXEC sys.sp_executesql
            N'IF EXISTS (SELECT 1 FROM dbo.ChuongTrinhGiamGia WHERE ApDungTatCaMon <> 0)
                THROW 51044, N''ApDungTatCaMon da duoc su dung; khong rollback tu dong.'', 1;';

    IF OBJECT_ID(N'dbo.ChiTietGiamGiaDangKy', N'U') IS NOT NULL
       AND EXISTS (SELECT 1 FROM sys.key_constraints
                   WHERE parent_object_id = OBJECT_ID(N'dbo.ChiTietGiamGiaDangKy')
                     AND name = N'PK_ChiTietGiamGiaDangKy_GD3')
        DROP TABLE dbo.ChiTietGiamGiaDangKy;

    IF OBJECT_ID(N'dbo.GiamGiaMonHoc', N'U') IS NOT NULL
       AND EXISTS (SELECT 1 FROM sys.key_constraints
                   WHERE parent_object_id = OBJECT_ID(N'dbo.GiamGiaMonHoc')
                     AND name = N'PK_GiamGiaMonHoc_GD3')
        DROP TABLE dbo.GiamGiaMonHoc;

    IF EXISTS (SELECT 1 FROM sys.key_constraints
               WHERE parent_object_id = OBJECT_ID(N'dbo.DangKyKhoaHoc')
                 AND name = N'UQ_DangKyKhoaHoc_MaDangKyCoSo_GD3')
        ALTER TABLE dbo.DangKyKhoaHoc
            DROP CONSTRAINT UQ_DangKyKhoaHoc_MaDangKyCoSo_GD3;

    IF OBJECT_ID(N'dbo.DF_ChuongTrinhGiamGia_ChoPhepKetHop_GD3', N'D') IS NOT NULL
    BEGIN
        ALTER TABLE dbo.ChuongTrinhGiamGia
            DROP CONSTRAINT DF_ChuongTrinhGiamGia_ChoPhepKetHop_GD3;
        ALTER TABLE dbo.ChuongTrinhGiamGia DROP COLUMN ChoPhepKetHop;
    END;
    IF OBJECT_ID(N'dbo.DF_ChuongTrinhGiamGia_ApDungTatCaMon_GD3', N'D') IS NOT NULL
    BEGIN
        ALTER TABLE dbo.ChuongTrinhGiamGia
            DROP CONSTRAINT DF_ChuongTrinhGiamGia_ApDungTatCaMon_GD3;
        ALTER TABLE dbo.ChuongTrinhGiamGia DROP COLUMN ApDungTatCaMon;
    END;

    IF EXISTS (
        SELECT 1
        FROM sys.extended_properties ep
        JOIN sys.columns c ON c.object_id=ep.major_id AND c.column_id=ep.minor_id
        WHERE ep.major_id=OBJECT_ID(N'dbo.ChuongTrinhGiamGia')
          AND c.name=N'MoTa' AND ep.name=N'VINABACUS_GD3_20261122')
    BEGIN
        EXEC sys.sp_executesql
            N'IF EXISTS (SELECT 1 FROM dbo.ChuongTrinhGiamGia WHERE MoTa IS NOT NULL)
                THROW 51045, N''MoTa da duoc su dung; khong rollback tu dong.'', 1;';
        EXEC sys.sp_dropextendedproperty
            @name = N'VINABACUS_GD3_20261122',
            @level0type = N'SCHEMA', @level0name = N'dbo',
            @level1type = N'TABLE', @level1name = N'ChuongTrinhGiamGia',
            @level2type = N'COLUMN', @level2name = N'MoTa';
        ALTER TABLE dbo.ChuongTrinhGiamGia DROP COLUMN MoTa;
    END;

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
