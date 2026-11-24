-- Giai doan 3: pham vi mon hoc va lich su nhieu chuong trinh giam gia.
-- CHI CHUAN BI: khong tu chay tren CSDL that. Doi chieu live schema va sao luu truoc khi duyet.
-- Legacy DangKyKhoaHoc.MaChuongTrinh/SoTienGiam duoc giu nguyen; khong backfill
-- snapshot tu cau hinh hien tai vi khong the chung minh do la cau hinh tai thoi diem cu.
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;

    IF OBJECT_ID(N'dbo.ChuongTrinhGiamGia', N'U') IS NULL
       OR OBJECT_ID(N'dbo.MonHoc', N'U') IS NULL
       OR OBJECT_ID(N'dbo.KhoaHoc', N'U') IS NULL
       OR OBJECT_ID(N'dbo.GiamGiaKhoaHoc', N'U') IS NULL
       OR OBJECT_ID(N'dbo.DangKyKhoaHoc', N'U') IS NULL
        THROW 51031, N'Thieu bang nen Giai doan 3; dung migration va doi chieu CSDL.', 1;

    IF COL_LENGTH(N'dbo.ChuongTrinhGiamGia', N'MoTa') IS NULL
    BEGIN
        ALTER TABLE dbo.ChuongTrinhGiamGia ADD MoTa NVARCHAR(500) NULL;
        EXEC sys.sp_addextendedproperty
            @name = N'VINABACUS_GD3_20261122', @value = N'1',
            @level0type = N'SCHEMA', @level0name = N'dbo',
            @level1type = N'TABLE', @level1name = N'ChuongTrinhGiamGia',
            @level2type = N'COLUMN', @level2name = N'MoTa';
    END;

    IF COL_LENGTH(N'dbo.ChuongTrinhGiamGia', N'ChoPhepKetHop') IS NULL
        ALTER TABLE dbo.ChuongTrinhGiamGia
            ADD ChoPhepKetHop BIT NOT NULL
                CONSTRAINT DF_ChuongTrinhGiamGia_ChoPhepKetHop_GD3 DEFAULT (0) WITH VALUES;

    IF COL_LENGTH(N'dbo.ChuongTrinhGiamGia', N'ApDungTatCaMon') IS NULL
        ALTER TABLE dbo.ChuongTrinhGiamGia
            ADD ApDungTatCaMon BIT NOT NULL
                CONSTRAINT DF_ChuongTrinhGiamGia_ApDungTatCaMon_GD3 DEFAULT (0) WITH VALUES;

    -- Gia tri mac dinh 0 giu nguyen pham vi cua chuong trinh cu
    -- (cac khoa cu duoc lien ket qua GiamGiaKhoaHoc).
    IF OBJECT_ID(N'dbo.GiamGiaMonHoc', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.GiamGiaMonHoc (
            MaChuongTrinh INT NOT NULL,
            MaMonHoc INT NOT NULL,
            MaCoSo INT NOT NULL,
            CONSTRAINT PK_GiamGiaMonHoc_GD3 PRIMARY KEY (MaChuongTrinh, MaMonHoc),
            CONSTRAINT FK_GiamGiaMonHoc_ChuongTrinh_GD3
                FOREIGN KEY (MaChuongTrinh, MaCoSo)
                REFERENCES dbo.ChuongTrinhGiamGia (MaChuongTrinh, MaCoSo),
            CONSTRAINT FK_GiamGiaMonHoc_MonHoc_GD3
                FOREIGN KEY (MaMonHoc, MaCoSo)
                REFERENCES dbo.MonHoc (MaMonHoc, MaCoSo)
        );

        -- Chi chuyen doi khi vua tao bang. Chay lai script khong duoc khoi phuc
        -- cac lien ket mon da duoc nguoi dung thay doi sau migration.
        -- Khoa cu van chi ap dung dung GiamGiaKhoaHoc, khong mo rong sang
        -- tat ca khoa trong mon.
        INSERT dbo.GiamGiaMonHoc (MaChuongTrinh, MaMonHoc, MaCoSo)
        SELECT DISTINCT g.MaChuongTrinh, k.MaMonHoc, g.MaCoSo
        FROM dbo.GiamGiaKhoaHoc g
        INNER JOIN dbo.KhoaHoc k
            ON k.MaKhoaHoc=g.MaKhoaHoc AND k.MaCoSo=g.MaCoSo
        INNER JOIN dbo.ChuongTrinhGiamGia ct
            ON ct.MaChuongTrinh=g.MaChuongTrinh AND ct.MaCoSo=g.MaCoSo
           AND ct.PhamViApDung='KHOA_HOC';
    END;

    IF NOT EXISTS (SELECT 1 FROM sys.key_constraints
                   WHERE parent_object_id=OBJECT_ID(N'dbo.GiamGiaMonHoc')
                     AND name=N'PK_GiamGiaMonHoc_GD3')
       OR NOT EXISTS (SELECT 1 FROM sys.foreign_keys
                      WHERE parent_object_id=OBJECT_ID(N'dbo.GiamGiaMonHoc')
                        AND name=N'FK_GiamGiaMonHoc_ChuongTrinh_GD3')
       OR NOT EXISTS (SELECT 1 FROM sys.foreign_keys
                      WHERE parent_object_id=OBJECT_ID(N'dbo.GiamGiaMonHoc')
                        AND name=N'FK_GiamGiaMonHoc_MonHoc_GD3')
        THROW 51033, N'GiamGiaMonHoc da ton tai voi rang buoc khac; can doi chieu thu cong.', 1;

    -- MaDangKy la PK, nhung FK snapshot can khoa ghep de bao ve MaCoSo.
    IF NOT EXISTS (SELECT 1 FROM sys.key_constraints
                   WHERE parent_object_id = OBJECT_ID(N'dbo.DangKyKhoaHoc')
                     AND name = N'UQ_DangKyKhoaHoc_MaDangKyCoSo_GD3')
    BEGIN
        ALTER TABLE dbo.DangKyKhoaHoc
            ADD CONSTRAINT UQ_DangKyKhoaHoc_MaDangKyCoSo_GD3
                UNIQUE (MaDangKy, MaCoSo);
    END;

    IF OBJECT_ID(N'dbo.ChiTietGiamGiaDangKy', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.ChiTietGiamGiaDangKy (
            MaDangKy INT NOT NULL,
            MaChuongTrinh INT NOT NULL,
            MaCoSo INT NOT NULL,
            TenChuongTrinhSnapshot NVARCHAR(180) NOT NULL,
            LoaiGiamSnapshot VARCHAR(15) NOT NULL,
            GiaTriGiamSnapshot DECIMAL(18,2) NOT NULL,
            SoTienGiam DECIMAL(18,2) NOT NULL,
            ThuTuApDung INT NOT NULL,
            NgayApDung DATETIME2(0) NOT NULL
                CONSTRAINT DF_ChiTietGiamGiaDangKy_NgayApDung_GD3 DEFAULT SYSUTCDATETIME(),
            CONSTRAINT PK_ChiTietGiamGiaDangKy_GD3
                PRIMARY KEY (MaDangKy, MaChuongTrinh),
            CONSTRAINT UQ_ChiTietGiamGiaDangKy_ThuTu_GD3
                UNIQUE (MaDangKy, ThuTuApDung),
            CONSTRAINT CK_ChiTietGiamGiaDangKy_SoTien_GD3
                CHECK (SoTienGiam >= 0 AND ThuTuApDung > 0),
            CONSTRAINT CK_ChiTietGiamGiaDangKy_GiaTri_GD3
                CHECK (GiaTriGiamSnapshot >= 0
                       AND LoaiGiamSnapshot IN ('PHAN_TRAM', 'SO_TIEN')
                       AND (LoaiGiamSnapshot <> 'PHAN_TRAM'
                            OR GiaTriGiamSnapshot <= 100)),
            CONSTRAINT FK_ChiTietGiamGiaDangKy_DangKy_GD3
                FOREIGN KEY (MaDangKy, MaCoSo)
                REFERENCES dbo.DangKyKhoaHoc (MaDangKy, MaCoSo),
            CONSTRAINT FK_ChiTietGiamGiaDangKy_ChuongTrinh_GD3
                FOREIGN KEY (MaChuongTrinh, MaCoSo)
                REFERENCES dbo.ChuongTrinhGiamGia (MaChuongTrinh, MaCoSo)
        );
    END;

    IF NOT EXISTS (SELECT 1 FROM sys.key_constraints
                   WHERE parent_object_id=OBJECT_ID(N'dbo.ChiTietGiamGiaDangKy')
                     AND name=N'PK_ChiTietGiamGiaDangKy_GD3')
       OR NOT EXISTS (SELECT 1 FROM sys.foreign_keys
                      WHERE parent_object_id=OBJECT_ID(N'dbo.ChiTietGiamGiaDangKy')
                        AND name=N'FK_ChiTietGiamGiaDangKy_DangKy_GD3')
       OR NOT EXISTS (SELECT 1 FROM sys.foreign_keys
                      WHERE parent_object_id=OBJECT_ID(N'dbo.ChiTietGiamGiaDangKy')
                        AND name=N'FK_ChiTietGiamGiaDangKy_ChuongTrinh_GD3')
        THROW 51034, N'ChiTietGiamGiaDangKy da ton tai voi rang buoc khac; can doi chieu thu cong.', 1;

    IF COL_LENGTH(N'dbo.GiamGiaMonHoc', N'MaMonHoc') IS NULL
       OR COL_LENGTH(N'dbo.ChiTietGiamGiaDangKy', N'TenChuongTrinhSnapshot') IS NULL
       OR COL_LENGTH(N'dbo.ChiTietGiamGiaDangKy', N'SoTienGiam') IS NULL
        THROW 51032, N'Bang Giai doan 3 da ton tai voi cau truc khac; can doi chieu thu cong.', 1;

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
