/*
 * GIAI DOAN 4 - DU THAO, KHONG DUOC TU DONG THUC THI.
 * Can doi chieu lai schema live va duoc chu CSDL phe duyet rieng.
 * Khong sua NhanVien, LopTrungTam, MonHoc hay du lieu kinh doanh hien co.
 * MaGiaoVien tren LopTrungTam van la giao vien hien tai. Luu anh chup phan
 * cong hien tai sang bang moi; TuNgay=NULL cho ban ghi cu vi khong biet
 * ngay bat dau. Khong suy doan chuyen mon cu tu phan cong lop.
 * Chuyen mon cu khong co nguon du lieu de suy ra: bang GiaoVienMonHoc ban dau
 * rong; nguoi dung khai bao/ket thuc cac dot moi tu giao dien sau migration.
 * TaiKhoanThucHien de NULL khi ung dung khong co phien dang nhap xac thuc;
 * khong gan nguoi thao tac gia. ThoiDiemGhiNhan la moc UTC cua ban ghi.
 * Chuyen giao vien chi mo cho lop chua tung co lich/hoc vien; lop da hoat
 * dong tiep tuc bi chan cho toi khi co quy trinh theo tung buoi.
 */
SET XACT_ABORT ON;

IF DB_NAME() <> N'VINABACUS'
    THROW 58040, N'Script chi danh cho CSDL VINABACUS.', 1;

IF OBJECT_ID(N'dbo.GiaoVienMonHoc', N'U') IS NOT NULL
    THROW 58041, N'GiaoVienMonHoc da ton tai; can doi chieu truoc khi chay.', 1;

IF OBJECT_ID(N'dbo.PhanCongGiaoVienTrungTam', N'U') IS NOT NULL
    THROW 58042, N'PhanCongGiaoVienTrungTam da ton tai; can doi chieu truoc khi chay.', 1;

BEGIN TRY
    BEGIN TRANSACTION;

    IF OBJECT_ID(N'dbo.NhanVienCoSo', N'U') IS NULL
       OR OBJECT_ID(N'dbo.MonHoc', N'U') IS NULL
       OR OBJECT_ID(N'dbo.LopTrungTam', N'U') IS NULL
       OR OBJECT_ID(N'dbo.KhoaHoc', N'U') IS NULL
       OR OBJECT_ID(N'dbo.TaiKhoan', N'U') IS NULL
        THROW 58043, N'Thieu bang nguon; dung migration.', 1;

    IF EXISTS (
        SELECT 1 FROM dbo.LopTrungTam l WITH (HOLDLOCK)
        LEFT JOIN dbo.NhanVienCoSo ncs WITH (HOLDLOCK)
          ON ncs.MaNhanVien=l.MaGiaoVien AND ncs.MaCoSo=l.MaCoSo
        WHERE l.MaGiaoVien IS NOT NULL AND ncs.MaNhanVien IS NULL
    )
        THROW 58044, N'Phan cong hien tai thieu NhanVienCoSo; dung de doi chieu.', 1;

    CREATE TABLE dbo.GiaoVienMonHoc (
        MaGiaoVienMonHoc BIGINT IDENTITY(1,1) NOT NULL
            CONSTRAINT PK_GiaoVienMonHoc_GD4 PRIMARY KEY,
        MaNhanVien INT NOT NULL,
        MaCoSo INT NOT NULL,
        MaMonHoc INT NOT NULL,
        TuNgay DATE NOT NULL,
        DenNgay DATE NULL,
        ThoiDiemGhiNhan DATETIME2(3) NOT NULL
            CONSTRAINT DF_GiaoVienMonHoc_GhiNhan_GD4 DEFAULT (SYSUTCDATETIME()),
        MaTaiKhoanThucHien INT NULL,
        GhiChu NVARCHAR(500) NULL,
        LyDoKetThuc NVARCHAR(500) NULL,
        ThoiDiemKetThucGhiNhan DATETIME2(3) NULL,
        CONSTRAINT CK_GiaoVienMonHoc_Ngay_GD4
            CHECK (DenNgay IS NULL OR DenNgay >= TuNgay),
        CONSTRAINT CK_GiaoVienMonHoc_KetThuc_GD4
            CHECK ((DenNgay IS NULL AND LyDoKetThuc IS NULL
                    AND ThoiDiemKetThucGhiNhan IS NULL)
                OR (DenNgay IS NOT NULL AND LyDoKetThuc IS NOT NULL
                    AND ThoiDiemKetThucGhiNhan IS NOT NULL)),
        CONSTRAINT FK_GiaoVienMonHoc_NhanVienCoSo_GD4
            FOREIGN KEY (MaNhanVien, MaCoSo)
            REFERENCES dbo.NhanVienCoSo(MaNhanVien, MaCoSo),
        CONSTRAINT FK_GiaoVienMonHoc_MonHoc_GD4
            FOREIGN KEY (MaMonHoc, MaCoSo)
            REFERENCES dbo.MonHoc(MaMonHoc, MaCoSo),
        CONSTRAINT FK_GiaoVienMonHoc_TaiKhoan_GD4
            FOREIGN KEY (MaTaiKhoanThucHien)
            REFERENCES dbo.TaiKhoan(MaTaiKhoan)
    );
    -- Mot chuyen mon co the co nhieu dot khac nhau; chi mot dot dang mo.
    CREATE UNIQUE INDEX UX_GiaoVienMonHoc_DangDay_GD4
        ON dbo.GiaoVienMonHoc(MaNhanVien, MaCoSo, MaMonHoc)
        WHERE DenNgay IS NULL;

    CREATE TABLE dbo.PhanCongGiaoVienTrungTam (
        MaPhanCong BIGINT IDENTITY(1,1) NOT NULL
            CONSTRAINT PK_PhanCongGiaoVienTrungTam_GD4 PRIMARY KEY,
        MaLop INT NOT NULL,
        MaKhoaHoc INT NOT NULL,
        MaCoSo INT NOT NULL,
        MaNhanVien INT NOT NULL,
        MaNhanVienTruoc INT NULL,
        TuNgay DATE NULL,
        DenNgay DATE NULL,
        ThoiDiemGhiNhan DATETIME2(3) NOT NULL
            CONSTRAINT DF_PhanCongGiaoVienTrungTam_GhiNhan_GD4
            DEFAULT (SYSUTCDATETIME()),
        MaTaiKhoanThucHien INT NULL,
        NguonDuLieu VARCHAR(12) NOT NULL
            CONSTRAINT DF_PhanCongGiaoVienTrungTam_Nguon_GD4 DEFAULT ('PHAT_SINH'),
        GhiChu NVARCHAR(500) NULL,
        CONSTRAINT CK_PhanCongGiaoVienTrungTam_Ngay_GD4
            CHECK ((NguonDuLieu='HIEN_TRANG' AND TuNgay IS NULL)
                OR (NguonDuLieu='PHAT_SINH' AND TuNgay IS NOT NULL
                    AND (DenNgay IS NULL OR DenNgay >= TuNgay))),
        CONSTRAINT CK_PhanCongGiaoVienTrungTam_Nguon_GD4
            CHECK (NguonDuLieu IN ('HIEN_TRANG','PHAT_SINH')),
        -- MaKhoaHoc/MaCoSo la anh chup tai thoi diem phan cong. Khong tham
        -- chieu bo 3 vao lop hien tai vi khoa hoc cua lop co the duoc sua
        -- truoc khi co hoc vien/lich; lich su phai giu khoa cu.
        CONSTRAINT FK_PhanCongGiaoVienTrungTam_Lop_GD4
            FOREIGN KEY (MaLop)
            REFERENCES dbo.LopTrungTam(MaLop),
        CONSTRAINT FK_PhanCongGiaoVienTrungTam_KhoaHoc_GD4
            FOREIGN KEY (MaKhoaHoc, MaCoSo)
            REFERENCES dbo.KhoaHoc(MaKhoaHoc, MaCoSo),
        CONSTRAINT FK_PhanCongGiaoVienTrungTam_NhanVienCoSo_GD4
            FOREIGN KEY (MaNhanVien, MaCoSo)
            REFERENCES dbo.NhanVienCoSo(MaNhanVien, MaCoSo),
        CONSTRAINT FK_PhanCongGiaoVienTrungTam_GiaoVienTruoc_GD4
            FOREIGN KEY (MaNhanVienTruoc, MaCoSo)
            REFERENCES dbo.NhanVienCoSo(MaNhanVien, MaCoSo),
        CONSTRAINT FK_PhanCongGiaoVienTrungTam_TaiKhoan_GD4
            FOREIGN KEY (MaTaiKhoanThucHien)
            REFERENCES dbo.TaiKhoan(MaTaiKhoan)
    );

    -- Luu lai phan cong dang thay tren lop. TuNgay chua biet thi de NULL,
    -- khong lay ngay khai giang lam ngay nhan lop.
    INSERT dbo.PhanCongGiaoVienTrungTam
        (MaLop, MaKhoaHoc, MaCoSo, MaNhanVien, TuNgay, DenNgay,
         NguonDuLieu, GhiChu)
    SELECT l.MaLop, l.MaKhoaHoc, l.MaCoSo, l.MaGiaoVien,
           NULL, NULL, 'HIEN_TRANG', N'Anh chup phan cong truoc migration GD4'
    FROM dbo.LopTrungTam l WITH (HOLDLOCK)
    WHERE l.MaGiaoVien IS NOT NULL;

    IF @@ROWCOUNT <> (
        SELECT COUNT(*) FROM dbo.LopTrungTam WITH (HOLDLOCK)
        WHERE MaGiaoVien IS NOT NULL
    )
        THROW 58045, N'So phan cong sao luu khong khop; da rollback.', 1;

    IF EXISTS (
        SELECT 1 FROM dbo.LopTrungTam l WITH (HOLDLOCK)
        WHERE l.MaGiaoVien IS NOT NULL
          AND NOT EXISTS (
              SELECT 1 FROM dbo.PhanCongGiaoVienTrungTam pc
              WHERE pc.MaLop=l.MaLop AND pc.MaCoSo=l.MaCoSo
                AND pc.MaKhoaHoc=l.MaKhoaHoc AND pc.MaNhanVien=l.MaGiaoVien
                AND pc.NguonDuLieu='HIEN_TRANG' AND pc.TuNgay IS NULL
                AND pc.DenNgay IS NULL
          )
    )
        THROW 58046, N'Anh chup phan cong khong khop lop hien tai; da rollback.', 1;

    CREATE UNIQUE INDEX UX_PhanCongGiaoVienTrungTam_DangPhuTrach_GD4
        ON dbo.PhanCongGiaoVienTrungTam(MaLop)
        WHERE DenNgay IS NULL;

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;

/*
 * Code hien tai ghi dong thoi LopTrungTam.MaGiaoVien va PhanCong trong mot
 * giao dich cho lop chua tung co lich/hoc vien. Khai bao chuyen mon moi cung
 * ghi theo giao dich rieng, ket thuc dot cu bang DenNgay va ly do. Unique chi
 * ngan hai dot dang mo; quy trinh viet phai kiem tra khoang thoi gian.
 * Lop da co xep lop/lich tiep tuc bi chan de giu lich su theo tung buoi.
 * Can duyet migration rieng; script nay KHONG duoc chay tu dong.
 */
